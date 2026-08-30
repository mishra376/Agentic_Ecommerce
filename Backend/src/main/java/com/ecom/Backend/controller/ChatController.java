package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Address;
import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.Product;
import com.ecom.Backend.dto.OrderRequest;
import com.ecom.Backend.security.CustomUserDetails;
import com.ecom.Backend.services.AddressServices;
import com.ecom.Backend.services.OrderServices;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final ProductServices productServices;
    private final OrderServices orderServices;
    private final AddressServices addressServices;

    // In-memory store for pending order preparations keyed by userId
    private static final Map<Long, PendingOrderPreparation> pendingOrders = new ConcurrentHashMap<>();

    @Autowired
    public ChatController(
            ChatModel chatModel,
            ProductServices productServices,
            OrderServices orderServices,
            AddressServices addressServices
    ) {
        this.chatClient = ChatClient.create(chatModel);
        this.productServices = productServices;
        this.orderServices = orderServices;
        this.addressServices = addressServices;
    }

    // DTO for a single message in conversation history
    public record HistoryMessage(String sender, String text) {}

    // Updated ChatRequest to include conversation history
    public record ChatRequest(String message, List<HistoryMessage> history) {}
    public record ChatResponse(String reply, String status) {}

    // Data class to hold a pending (prepared but not yet confirmed) order
    public static class PendingOrderPreparation {
        public String shippingAddress;
        public String paymentMethod;
        public List<OrderItemInput> items;
        public String summary;

        public PendingOrderPreparation(String shippingAddress, String paymentMethod, List<OrderItemInput> items, String summary) {
            this.shippingAddress = shippingAddress;
            this.paymentMethod = paymentMethod;
            this.items = items;
            this.summary = summary;
        }
    }

    // Tool definition class containing @Tool annotated methods for Spring AI
    public static class ChatTools {
        private final ProductServices productServices;
        private final OrderServices orderServices;
        private final AddressServices addressServices;
        private final CustomUserDetails userDetails;

        public ChatTools(
                ProductServices productServices,
                OrderServices orderServices,
                AddressServices addressServices,
                CustomUserDetails userDetails
        ) {
            this.productServices = productServices;
            this.orderServices = orderServices;
            this.addressServices = addressServices;
            this.userDetails = userDetails;
        }

        @Tool(description = "Get the list of all products in the e-commerce catalog database.")
        public List<Product> getAllProducts() {
            System.out.println(">>> [AI Native Tool Call] getAllProducts invoked.");
            List<Product> results = productServices.getAllProducts();
            System.out.println(">>> [AI Native Tool Call] Returned " + results.size() + " products from database.");
            return results;
        }

        @Tool(description = "Retrieve all registered shipping addresses of the currently logged in user.")
        public List<Address> getUserAddresses() {
            System.out.println(">>> [AI Native Tool Call] getUserAddresses invoked.");
            if (userDetails == null) {
                throw new RuntimeException("User is not authenticated. Please log in first.");
            }
            List<Address> results = addressServices.getAddressesByUser(userDetails.getId());
            System.out.println(">>> [AI Native Tool Call] Returned " + results.size() + " addresses for user ID: " + userDetails.getId());
            return results;
        }

        @Tool(description = "Prepare an order for the currently logged in user WITHOUT placing it. This validates the product, checks stock, resolves the shipping address, and returns an order summary for user confirmation. The order is NOT placed yet and stock is NOT deducted. You must collect the items list containing mongodbProductId and quantity. If a shipping address is not explicitly given, or if the user requests to use their default shipping address, set shippingAddress to 'default'. Collect or prompt for paymentMethod if not specified. After calling this, you MUST present the summary to the user and ask them to confirm before calling confirmOrder.")
        public String prepareOrder(String shippingAddress, String paymentMethod, List<OrderItemInput> items) {
            System.out.println(">>> [AI Native Tool Call] prepareOrder invoked.");
            if (userDetails == null) {
                return "Error: You must be logged in to place an order.";
            }
            try {
                // Resolve shipping address
                String resolvedAddress = shippingAddress;
                if (resolvedAddress == null || resolvedAddress.trim().isEmpty() || "default".equalsIgnoreCase(resolvedAddress.trim())) {
                    List<Address> addresses = addressServices.getAddressesByUser(userDetails.getId());
                    Address defaultAddress = addresses.stream()
                            .filter(Address::getIsDefault)
                            .findFirst()
                            .orElse(null);
                    if (defaultAddress == null) {
                        return "Error: No default shipping address found. Please provide a shipping address.";
                    }
                    resolvedAddress = defaultAddress.getStreetAddress() + ", " + defaultAddress.getCity() + ", " + defaultAddress.getState() + " " + defaultAddress.getZipCode();
                }

                // Validate products and calculate total
                double totalAmount = 0.0;
                StringBuilder summaryBuilder = new StringBuilder();
                summaryBuilder.append("**Order Summary (Pending Confirmation)**\n\n");

                for (OrderItemInput item : items) {
                    Product product = productServices.getProductById(item.mongodbProductId()).orElse(null);
                    if (product == null) {
                        return "Error: Product not found with ID: " + item.mongodbProductId();
                    }
                    int availableStock = product.getStock() != null ? product.getStock() : 0;
                    if (availableStock < item.quantity()) {
                        return "Error: Insufficient stock for product: " + product.getName()
                                + " (Requested: " + item.quantity() + ", Available: " + availableStock + ")";
                    }
                    double itemTotal = product.getPrice() * item.quantity();
                    totalAmount += itemTotal;
                    summaryBuilder.append("- **").append(product.getName()).append("** × ").append(item.quantity())
                            .append(" — ₹").append(String.format("%,.2f", itemTotal)).append("\n");
                }

                summaryBuilder.append("\n**Total Amount:** ₹").append(String.format("%,.2f", totalAmount));
                summaryBuilder.append("\n**Shipping Address:** ").append(resolvedAddress);
                summaryBuilder.append("\n**Payment Method:** ").append(paymentMethod);

                String summary = summaryBuilder.toString();

                // Store the pending preparation in memory
                PendingOrderPreparation preparation = new PendingOrderPreparation(
                        resolvedAddress, paymentMethod, items, summary
                );
                pendingOrders.put(userDetails.getId(), preparation);

                System.out.println(">>> [AI Native Tool Call] Order prepared for user ID: " + userDetails.getId() + ". Awaiting confirmation.");
                return summary + "\n\nPlease ask the user to confirm this order. Call confirmOrder when the user says yes. If the user says no or wants to cancel, call cancelOrder.";
            } catch (Exception e) {
                return "Error: Failed to prepare order. " + e.getMessage();
            }
        }

        @Tool(description = "Confirm and place a previously prepared order for the currently logged in user. Only call this AFTER the user has explicitly confirmed they want to place the order (e.g., user says 'yes', 'confirm', 'place it', 'go ahead'). This will actually place the order, deduct stock, and set the status to PLACED.")
        public String confirmOrder() {
            System.out.println(">>> [AI Native Tool Call] confirmOrder invoked.");
            if (userDetails == null) {
                return "Error: You must be logged in to confirm an order.";
            }
            PendingOrderPreparation preparation = pendingOrders.remove(userDetails.getId());
            if (preparation == null) {
                return "Error: No pending order found to confirm. Please prepare an order first using prepareOrder.";
            }
            try {
                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setShippingAddress(preparation.shippingAddress);
                orderRequest.setPaymentMethod(preparation.paymentMethod);

                List<OrderRequest.OrderItemDto> dtoList = new ArrayList<>();
                for (OrderItemInput item : preparation.items) {
                    OrderRequest.OrderItemDto dto = new OrderRequest.OrderItemDto();
                    dto.setMongodbProductId(item.mongodbProductId());
                    dto.setQuantity(item.quantity());
                    dtoList.add(dto);
                }
                orderRequest.setItems(dtoList);

                Order order = orderServices.placeOrder(userDetails.getId(), orderRequest);
                String response = "Order placed successfully! Order ID: " + order.getId()
                       + ", Total Amount: ₹" + String.format("%,.2f", order.getTotalAmount())
                       + ", Shipped to: " + order.getShippingAddress()
                       + ", Payment Method: " + order.getPaymentMethod()
                       + ", Status: " + order.getStatus() + ".";
                if ("RAZORPAY".equalsIgnoreCase(order.getPaymentMethod()) && order.getRazorpayOrderId() != null) {
                    response += " Please complete your payment using Razorpay Order ID: " + order.getRazorpayOrderId()
                             + ". The client should launch checkout and invoke verify API `/api/payment/verify` upon completion.";
                }
                System.out.println(">>> [AI Native Tool Call] Order confirmed and placed. Order ID: " + order.getId());
                return response;
            } catch (Exception e) {
                return "Error: Failed to place order. " + e.getMessage();
            }
        }

        @Tool(description = "Cancel a previously prepared order that has not been confirmed yet. Call this when the user says 'no', 'cancel', 'don't place it', or declines the order. No stock is deducted and no order is created.")
        public String cancelOrder() {
            System.out.println(">>> [AI Native Tool Call] cancelOrder invoked.");
            if (userDetails == null) {
                return "Error: You must be logged in.";
            }
            PendingOrderPreparation removed = pendingOrders.remove(userDetails.getId());
            if (removed != null) {
                System.out.println(">>> [AI Native Tool Call] Pending order cancelled for user ID: " + userDetails.getId());
                return "Order has been cancelled. No order was placed and no stock was deducted. Let me know if you'd like to order something else!";
            }
            return "No pending order to cancel.";
        }
    }

    public record OrderItemInput(String mongodbProductId, Integer quantity) {}

    @PostMapping
    public ChatResponse handleChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new ChatResponse("I didn't receive any message. How can I help you today?", "success");
        }

        String userMessage = request.message().trim();
        List<HistoryMessage> history = request.history() != null ? request.history() : List.of();
        String reply = generateAiResponse(userMessage, history, userDetails);
        return new ChatResponse(reply, "success");
    }

    private String generateAiResponse(String currentMessage, List<HistoryMessage> history, CustomUserDetails userDetails) {
        try {
            String systemInstruction = """
                    You are a helpful, direct E-Commerce Assistant.
                    
                    You have access to the following tools:
                    1. `getAllProducts`: Retrieve all catalog products. Call this when searching or identifying product IDs.
                    2. `getUserAddresses`: Retrieve registered shipping addresses of the currently logged in user.
                    3. `prepareOrder`: Prepare an order (validate product, stock, address) and return a summary. Does NOT place the order.
                    4. `confirmOrder`: Confirm and actually place a previously prepared order. Only call this after user explicitly confirms.
                    5. `cancelOrder`: Cancel a prepared order if user declines.
                    
                    CRITICAL ORDER FLOW — You MUST follow these steps exactly:
                    1. First, use `getAllProducts` to find the exact MongoDB product `id` of any products the user wants to buy.
                    2. Check if the user has any registered addresses using `getUserAddresses` if they ask to ship to their default address or don't specify one.
                    3. Prompt for or verify `paymentMethod` (e.g. "Credit Card", "UPI", "COD") if not clear.
                    4. Call `prepareOrder` with the resolved product ID, shipping address, and payment method. This returns an order summary.
                    5. Present the order summary to the user and ask: "Would you like to confirm and place this order?"
                    6. WAIT for the user's response. Do NOT call confirmOrder in the same turn as prepareOrder.
                    7. If the user confirms (says "yes", "confirm", "place it", "go ahead", etc.), call `confirmOrder`.
                    8. If the user declines (says "no", "cancel", etc.), call `cancelOrder`.
                    
                    IMPORTANT RULES:
                    - NEVER call confirmOrder without the user explicitly confirming first.
                    - NEVER place an order without showing the summary and getting confirmation.
                    - You have full conversation history. Use it to understand references like "option 1", "the first one", "that laptop", etc.
                    - When the user references a previous message or option, look at the conversation history to resolve what they mean.
                    
                    Respond in a natural, conversational chat style.
                    """;

            // Build the message list with full conversation history
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemInstruction));

            // Add conversation history (previous messages)
            if (history != null && !history.isEmpty()) {
                for (HistoryMessage msg : history) {
                    if ("user".equalsIgnoreCase(msg.sender())) {
                        messages.add(new UserMessage(msg.text()));
                    } else if ("assistant".equalsIgnoreCase(msg.sender())) {
                        messages.add(new AssistantMessage(msg.text()));
                    }
                }
            }

            // Add the current user message
            messages.add(new UserMessage(currentMessage));

            String reply = chatClient.prompt()
                    .messages(messages)
                    .tools(new ChatTools(productServices, orderServices, addressServices, userDetails))
                    .call()
                    .content();

            return reply != null ? reply : "### Error\n\nNo response received from the AI model.";
        } catch (Exception e) {
            e.printStackTrace();
            return "### Error\n\nFailed to generate response: " + e.getMessage();
        }
    }
}