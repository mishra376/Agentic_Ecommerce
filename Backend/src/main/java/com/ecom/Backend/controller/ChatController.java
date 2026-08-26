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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final ProductServices productServices;
    private final OrderServices orderServices;
    private final AddressServices addressServices;

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

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply, String status) {}

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

        @Tool(description = "Place a new order for the currently logged in user. You must collect the items list containing mongodbProductId and quantity. If a shipping address is not explicitly given, or if the user requests to use their default shipping address, set shippingAddress to 'default'. Collect or prompt for paymentMethod if not specified.")
        public String placeOrder(String shippingAddress, String paymentMethod, List<OrderItemInput> items) {
            System.out.println(">>> [AI Native Tool Call] placeOrder invoked.");
            if (userDetails == null) {
                return "Error: You must be logged in to place an order.";
            }
            try {
                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setShippingAddress(shippingAddress);
                orderRequest.setPaymentMethod(paymentMethod);
                
                List<OrderRequest.OrderItemDto> dtoList = new ArrayList<>();
                for (OrderItemInput item : items) {
                    OrderRequest.OrderItemDto dto = new OrderRequest.OrderItemDto();
                    dto.setMongodbProductId(item.mongodbProductId());
                    dto.setQuantity(item.quantity());
                    dtoList.add(dto);
                }
                orderRequest.setItems(dtoList);

                Order order = orderServices.placeOrder(userDetails.getId(), orderRequest);
                return "Success! Order placed successfully. Order ID: " + order.getId() 
                       + ", Total Amount: " + order.getTotalAmount() 
                       + ", Shipped to: " + order.getShippingAddress() 
                       + ", Payment Method: " + order.getPaymentMethod() 
                       + ", Status: " + order.getStatus() + ".";
            } catch (Exception e) {
                return "Error: Failed to place order. " + e.getMessage();
            }
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
        String reply = generateAiResponse(userMessage, userDetails);
        return new ChatResponse(reply, "success");
    }

    private String generateAiResponse(String message, CustomUserDetails userDetails) {
        try {
            String systemInstruction = """
                    You are a helpful, direct E-Commerce Assistant.
                    
                    You have access to the following tools:
                    1. `getAllProducts`: Retrieve all catalog products. Call this when searching or identifying product IDs.
                    2. `getUserAddresses`: Retrieve registered shipping addresses of the currently logged in user.
                    3. `placeOrder`: Place a new order for the authenticated user.
                    
                    Instructions for Placing Orders:
                    - First, use `getAllProducts` to find the exact MongoDB product `id` of any products the user wants to buy.
                    - Check if the user has any registered addresses using `getUserAddresses` if they ask to ship to their default address or don't specify one. If a default address is set, use "default" as the shippingAddress parameter for the `placeOrder` tool and tell the user you will use their default address. If no addresses are saved, ask the user to provide their shipping address.
                    - If the user provides a direct shipping address (e.g. "123 Main St..."), use it directly in `placeOrder`.
                    - Prompt for or verify `paymentMethod` (e.g. "Credit Card", "UPI", "COD") if not clear.
                    - Call `placeOrder` only after resolving product ID, shippingAddress, and paymentMethod. Report success details (Order ID, Total) to the user.
                    
                    Respond in a natural, conversational chat style.
                    """;

            String reply = chatClient.prompt()
                    .system(systemInstruction)
                    .user(message)
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