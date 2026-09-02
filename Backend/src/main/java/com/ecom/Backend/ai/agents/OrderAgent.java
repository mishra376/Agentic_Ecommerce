package com.ecom.Backend.ai.agents;

import com.ecom.Backend.entity.Address;
import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.Product;
import com.ecom.Backend.dto.OrderRequest;
import com.ecom.Backend.services.AddressServices;
import com.ecom.Backend.services.OrderServices;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order Specialist Agent.
 * Handles order preparation, confirmation, cancellation, and order history.
 * Owns the in-memory pendingOrders state.
 */
@Component
public class OrderAgent {

    private final ChatClient chatClient;
    private final ProductServices productServices;
    private final OrderServices orderServices;
    private final AddressServices addressServices;

    // In-memory store for pending order preparations keyed by userId
    private static final Map<Long, PendingOrderPreparation> pendingOrders = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            You are an Order Specialist for an e-commerce platform.

            Your job is to handle the entire order lifecycle: preparing orders, confirming them, cancelling them, and showing order history.

            CRITICAL ORDER FLOW — Follow these steps exactly:
            1. When asked to prepare an order, use `prepareOrder` with the product ID, shipping address, and quantity.
               - If no shipping address is specified, or the user wants their default address, set shippingAddress to "default".
               - Unless user explicitly specified COD or RAZORPAY upfront, default paymentMethod to "PENDING_SELECTION".
            2. After preparing, return the order summary clearly. Ask the user to choose their payment method: **COD (Cash on Delivery)** or **Razorpay**.
            3. When the user confirms and/or specifies their payment choice (e.g. "Razorpay", "COD"), call `confirmOrder(userId, paymentMethod)` passing "RAZORPAY" or "COD".
            4. If user chose Razorpay, the order will be created and the Razorpay payment window will automatically launch for payment.
            5. Only call `cancelOrder` when explicitly told the user wants to cancel.
            6. For order history, use `getOrderHistory`.
            7. Use `getUserAddresses` to look up the user's registered addresses when needed.

            IMPORTANT:
            - NEVER confirm an order without explicit user confirmation.
            - Always include the order summary with product names, quantities, prices, shipping address, and payment options (**COD** or **Razorpay**).
            - The userId is passed as part of your task description. Use it for all operations.

            Return raw results. The orchestrator will format the final user-facing message.
            """;

    public OrderAgent(ChatModel chatModel, ProductServices productServices,
                      OrderServices orderServices, AddressServices addressServices) {
        this.productServices = productServices;
        this.orderServices = orderServices;
        this.addressServices = addressServices;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(new OrderTools(productServices, orderServices, addressServices, pendingOrders))
                .build();
    }

    /**
     * Execute a task delegated by the Master orchestrator.
     *
     * @param taskDescription the task description crafted by the Master AI (includes userId)
     * @return the agent's response with order information
     */
    public String execute(String taskDescription) {
        System.out.println(">>> [OrderAgent] Received task: " + taskDescription);
        try {
            String result = chatClient.prompt()
                    .user(taskDescription)
                    .call()
                    .content();
            System.out.println(">>> [OrderAgent] Task completed.");
            return result != null ? result : "No response from Order Agent.";
        } catch (Exception e) {
            System.err.println(">>> [OrderAgent] Error: " + e.getMessage());
            return "Order Agent error: " + e.getMessage();
        }
    }

    // ─── Data class for pending orders ───────────────────────────────────

    public static class PendingOrderPreparation {
        public String shippingAddress;
        public String paymentMethod;
        public List<OrderItemInput> items;
        public String summary;

        public PendingOrderPreparation(String shippingAddress, String paymentMethod,
                                       List<OrderItemInput> items, String summary) {
            this.shippingAddress = shippingAddress;
            this.paymentMethod = paymentMethod;
            this.items = items;
            this.summary = summary;
        }
    }

    public record OrderItemInput(String mongodbProductId, Integer quantity) {}

    // ─── Tool definitions ────────────────────────────────────────────────

    public static class OrderTools {
        private final ProductServices productServices;
        private final OrderServices orderServices;
        private final AddressServices addressServices;
        private final Map<Long, PendingOrderPreparation> pendingOrders;

        public OrderTools(ProductServices productServices, OrderServices orderServices,
                          AddressServices addressServices, Map<Long, PendingOrderPreparation> pendingOrders) {
            this.productServices = productServices;
            this.orderServices = orderServices;
            this.addressServices = addressServices;
            this.pendingOrders = pendingOrders;
        }

        @Tool(description = "Retrieve all registered shipping addresses of a user.")
        public List<Address> getUserAddresses(
                @ToolParam(description = "The user's ID.") Long userId) {
            System.out.println(">>> [OrderAgent Tool] getUserAddresses invoked for userId: " + userId);
            List<Address> results = addressServices.getAddressesByUser(userId);
            System.out.println(">>> [OrderAgent Tool] Returned " + results.size() + " addresses.");
            return results;
        }

        @Tool(description = "Prepare an order for a user WITHOUT placing it. Validates product, checks stock, resolves shipping address, and returns an order summary. Prompts the user to select payment method: COD or RAZORPAY.")
        public String prepareOrder(
                @ToolParam(description = "The user's ID.") Long userId,
                @ToolParam(description = "Shipping address string, or 'default' to use user's default address.") String shippingAddress,
                @ToolParam(description = "Payment method if specified (e.g. COD or RAZORPAY), otherwise 'PENDING_SELECTION'.") String paymentMethod,
                @ToolParam(description = "List of items to order. Each item has mongodbProductId and quantity.") List<OrderItemInput> items) {
            System.out.println(">>> [OrderAgent Tool] prepareOrder invoked for userId: " + userId);
            try {
                // Resolve shipping address
                String resolvedAddress = shippingAddress;
                if (resolvedAddress == null || resolvedAddress.trim().isEmpty() || "default".equalsIgnoreCase(resolvedAddress.trim())) {
                    List<Address> addresses = addressServices.getAddressesByUser(userId);
                    Address defaultAddress = addresses.stream()
                            .filter(Address::getIsDefault)
                            .findFirst()
                            .orElse(null);
                    if (defaultAddress == null) {
                        return "Error: No default shipping address found. Please provide a shipping address.";
                    }
                    resolvedAddress = defaultAddress.getStreetAddress() + ", " + defaultAddress.getCity()
                            + ", " + defaultAddress.getState() + " " + defaultAddress.getZipCode();
                }

                String selectedMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "PENDING_SELECTION";

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

                String summary = summaryBuilder.toString();

                // Store the pending preparation in memory
                PendingOrderPreparation preparation = new PendingOrderPreparation(
                        resolvedAddress, selectedMethod, items, summary
                );
                pendingOrders.put(userId, preparation);

                System.out.println(">>> [OrderAgent Tool] Order prepared for userId: " + userId + ". Awaiting payment method & confirmation.");
                return summary + "\n\nPlease ask the user to select their payment method: **COD (Cash on Delivery)** or **Razorpay**, and confirm the order.";
            } catch (Exception e) {
                return "Error: Failed to prepare order. " + e.getMessage();
            }
        }

        @Tool(description = "Confirm and place a previously prepared order. Accepts paymentMethod parameter: 'COD' or 'RAZORPAY'. If user chose Razorpay, this triggers the Razorpay payment window.")
        public String confirmOrder(
                @ToolParam(description = "The user's ID.") Long userId,
                @ToolParam(description = "Payment method specified by user: 'COD' or 'RAZORPAY'. Defaults to 'RAZORPAY' if user wants online payment.") String paymentMethod) {
            System.out.println(">>> [OrderAgent Tool] confirmOrder invoked for userId: " + userId + " with paymentMethod: " + paymentMethod);
            PendingOrderPreparation preparation = pendingOrders.remove(userId);
            if (preparation == null) {
                return "Error: No pending order found to confirm. Please prepare an order first using prepareOrder.";
            }
            try {
                String finalPaymentMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.toUpperCase().trim() : preparation.paymentMethod;
                if ("PENDING_SELECTION".equalsIgnoreCase(finalPaymentMethod) || finalPaymentMethod.isBlank()) {
                    finalPaymentMethod = "RAZORPAY"; // default fallback for confirmation
                }

                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setShippingAddress(preparation.shippingAddress);
                orderRequest.setPaymentMethod(finalPaymentMethod);

                List<OrderRequest.OrderItemDto> dtoList = new ArrayList<>();
                for (OrderItemInput item : preparation.items) {
                    OrderRequest.OrderItemDto dto = new OrderRequest.OrderItemDto();
                    dto.setMongodbProductId(item.mongodbProductId());
                    dto.setQuantity(item.quantity());
                    dtoList.add(dto);
                }
                orderRequest.setItems(dtoList);

                Order order = orderServices.placeOrder(userId, orderRequest);
                String response = "Order placed successfully! Order ID: #" + order.getId()
                        + ", Total Amount: ₹" + String.format("%,.2f", order.getTotalAmount())
                        + ", Shipping Address: " + order.getShippingAddress()
                        + ", Payment Method: " + order.getPaymentMethod()
                        + ", Order Status: " + order.getStatus() + ".";
                if ("RAZORPAY".equalsIgnoreCase(order.getPaymentMethod()) && order.getRazorpayOrderId() != null) {
                    response += "\n\n[PAY_WITH_RAZORPAY: orderId=" + order.getId()
                            + ", razorpayOrderId=" + order.getRazorpayOrderId()
                            + ", amount=" + String.format(java.util.Locale.US, "%.2f", order.getTotalAmount())
                            + ", autoOpen=true]";
                }
                System.out.println(">>> [OrderAgent Tool] Order confirmed and placed. Order ID: " + order.getId());
                return response;
            } catch (Exception e) {
                return "Error: Failed to place order. " + e.getMessage();
            }
        }

        @Tool(description = "Cancel a previously prepared order that has not been confirmed yet. Call this when the user declines the order.")
        public String cancelOrder(
                @ToolParam(description = "The user's ID.") Long userId) {
            System.out.println(">>> [OrderAgent Tool] cancelOrder invoked for userId: " + userId);
            PendingOrderPreparation removed = pendingOrders.remove(userId);
            if (removed != null) {
                System.out.println(">>> [OrderAgent Tool] Pending order cancelled for userId: " + userId);
                return "Order has been cancelled. No order was placed and no stock was deducted.";
            }
            return "No pending order to cancel.";
        }

        @Tool(description = "Get the order history for a user, showing all past orders.")
        public List<Order> getOrderHistory(
                @ToolParam(description = "The user's ID.") Long userId) {
            System.out.println(">>> [OrderAgent Tool] getOrderHistory invoked for userId: " + userId);
            List<Order> orders = orderServices.getOrdersByUser(userId);
            System.out.println(">>> [OrderAgent Tool] Returned " + orders.size() + " orders.");
            return orders;
        }
    }
}
