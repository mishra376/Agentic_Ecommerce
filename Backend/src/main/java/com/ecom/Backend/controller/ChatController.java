package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.security.CustomUserDetails;
import com.ecom.Backend.services.AddressServices;
import com.ecom.Backend.services.ChatToolsService;
import com.ecom.Backend.services.OrderServices;
import com.ecom.Backend.services.PaymentService;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP adapter for the conversational AI chat endpoint.
 *
 * Supports multi-turn conversational history, tool‑based product search,
 * address retrieval, order placement, and Razorpay/COD payment flows.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final ProductServices productServices;
    private final OrderServices orderServices;
    private final AddressServices addressServices;
    private final PaymentService paymentService;

    @Autowired
    public ChatController(
            ChatModel chatModel,
            ProductServices productServices,
            OrderServices orderServices,
            AddressServices addressServices,
            PaymentService paymentService
    ) {
        this.chatClient = ChatClient.create(chatModel);
        this.productServices = productServices;
        this.orderServices = orderServices;
        this.addressServices = addressServices;
        this.paymentService = paymentService;
    }

    public record ChatMessageDto(String role, String content) {}
    public record ChatRequest(String message, List<ChatMessageDto> history) {}
    public record ChatResponse(String reply, String status) {}

    @GetMapping("/health")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> healthCheck() {
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "UP", "service", "ChatController"));
    }

    @PostMapping
    public ChatResponse handleChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            System.out.println("⚠️ [ChatController] Received empty or null chat message.");
            return new ChatResponse("I didn't receive any message. How can I help you today?", "success");
        }

        String userMessage = request.message().trim();

        // Fast-path for ping / heartbeat so LLM isn't called repeatedly
        if ("ping".equalsIgnoreCase(userMessage)) {
            return new ChatResponse("pong", "success");
        }

        List<ChatMessageDto> history = request.history() != null ? request.history() : List.of();
        
        System.out.println("\n================================================================================");
        System.out.println("🤖 [CHAT CONTROLLER] Incoming Request");
        System.out.println("👤 User: " + (userDetails != null ? userDetails.getUsername() + " (ID: " + userDetails.getId() + ", Authorities: " + userDetails.getAuthorities() + ")" : "Guest (Not Logged In)"));
        System.out.println("💬 Message: \"" + userMessage + "\"");
        System.out.println("📜 History Size: " + history.size() + " messages");
        System.out.println("================================================================================");

        String reply = generateAiResponse(userMessage, history, userDetails);
        return new ChatResponse(reply, "success");
    }

    private String generateAiResponse(String userMessage, List<ChatMessageDto> history, CustomUserDetails userDetails) {
        long startTime = System.currentTimeMillis();
        try {
            // System instruction – strict instructions against exposing tools/APIs or fake payment gateways
            String systemInstruction = """
                    You are the official E-Commerce Shopping Assistant.

                    === STRICT PRIVACY & BEHAVIOR RULES ===
                    1. NEVER mention internal tools, function names (such as getAllProducts, searchProductsByName, placeOrder, confirmPayment, etc.), backend APIs, MongoDB, or database schemas in your responses. Talk naturally and professionally like an online store customer representative.
                    2. NEVER output raw JSON, function schemas, or code blocks with `{"type":"function"` in your chat text.
                    3. Supported payment methods are ONLY:
                       - "RAZORPAY" (Online Payment via Cards, UPI, NetBanking)
                       - "COD" (Cash on Delivery)
                       NEVER suggest Apple Pay, PayPal, Stripe, or any unsupported payment methods.
                    4. ALWAYS format all product prices in Indian Rupees (₹), e.g. ₹99,990.00. NEVER use dollar ($) signs.
                    5. ONLY suggest products that actually match the user's category/query (e.g. if the user asks for laptops, only show laptops).

                    === ORDER & PAYMENT WORKFLOW ===
                    1. Product Discovery: When the user asks for products, call `searchProductsByName` and display the matching options with Name, Price (₹), and key features.
                    2. Collecting Order Information: When a user wants to buy an item, verify:
                       - Selected Product
                       - Quantity
                       - Delivery Address (offer to use saved addresses from `getUserAddresses` or prompt for an address)
                       - Payment Method: 'RAZORPAY' or 'COD'
                    3. Order Summary & Confirmation: Show a clear order summary and ask the user for confirmation (e.g. "Should I place this order?").
                    4. Order Placement: Only after the user confirms ("Yes", "Confirm", "Proceed"), call `placeOrder`.
                    5. Post-Order Next Steps:
                       - If COD: Inform the user that Order #[ID] is confirmed and will be shipped with Cash on Delivery.
                       - If RAZORPAY: Present the order summary and ask: *"Order #[ID] created. Would you like to authorize payment of ₹[amount] now? (Reply Yes to proceed or No to cancel)"*.
                       - When user replies "Yes" / "Authorize" -> call `confirmPayment(orderId)`.
                       - When user replies "No" / "Cancel" -> call `cancelPayment(orderId)`.

                    === RESPONSE STYLE ===
                    Be polite, helpful, and use clean markdown formatting (bullet lists, bold text).
                    """;

            // Build per‑request tools service scoped to the authenticated user
            ChatToolsService tools = new ChatToolsService(
                    productServices, orderServices, addressServices, paymentService, userDetails
            );

            // Construct prompt messages with history
            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(systemInstruction));

            if (history != null && !history.isEmpty()) {
                int startIndex = Math.max(0, history.size() - 12);
                for (int i = startIndex; i < history.size(); i++) {
                    ChatMessageDto chatMsg = history.get(i);
                    if (chatMsg == null || chatMsg.content() == null || chatMsg.content().trim().isEmpty()) {
                        continue;
                    }
                    String role = chatMsg.role() != null ? chatMsg.role().toLowerCase().trim() : "user";
                    if ("assistant".equals(role) || "ai".equals(role) || "model".equals(role)) {
                        promptMessages.add(new AssistantMessage(chatMsg.content().trim()));
                    } else if ("user".equals(role)) {
                        promptMessages.add(new UserMessage(chatMsg.content().trim()));
                    }
                }
            }

            promptMessages.add(new UserMessage(userMessage));

            System.out.println("🚀 [CHAT CONTROLLER] Sending prompt to AI ChatClient (Messages in context: " + promptMessages.size() + ")...");

            String reply = chatClient.prompt()
                    .messages(promptMessages)
                    .tools(tools)
                    .call()
                    .content();

            // Sanitize response to prevent raw JSON tool call leakage or internal API exposure
            reply = sanitizeAndExecuteRawJsonToolCalls(reply, tools);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("✅ [CHAT CONTROLLER] AI responded in " + elapsed + " ms");
            if (reply != null) {
                System.out.println("💬 [CHAT CONTROLLER] Reply Preview:\n" + (reply.length() > 300 ? reply.substring(0, 300) + "..." : reply));
            } else {
                System.out.println("⚠️ [CHAT CONTROLLER] AI returned null content.");
            }
            System.out.println("================================================================================\n");

            return reply != null ? reply : "### Error\n\nNo response received from the AI model.";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("\n❌ [CHAT CONTROLLER] Exception during AI generation after " + elapsed + " ms:");
            System.err.println("❌ Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("❌ Root Cause: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage());
            }
            e.printStackTrace();
            System.err.println("================================================================================\n");
            return "### ⚠️ AI Processing Error\n\nFailed to generate response: **" + e.getMessage() + "**\n\n" +
                   "> *Note: Ensure your Ollama server is running (e.g. `ollama run llama3.2:1b`) and accessible.*";
        }
    }

    /**
     * Intercepts and executes raw JSON function calls accidentally printed as text by small LLMs,
     * and filters out any internal tool notes before returning to the user.
     */
    private String sanitizeAndExecuteRawJsonToolCalls(String reply, ChatToolsService tools) {
        if (reply == null || reply.trim().isEmpty()) {
            return reply;
        }

        String trimmed = reply.trim();
        if (trimmed.contains("searchProductsByName") || trimmed.contains("getAllProducts") 
                || trimmed.contains("getProductById") || trimmed.contains("placeOrder") 
                || trimmed.contains("confirmPayment") || trimmed.contains("cancelPayment") 
                || trimmed.contains("getUserAddresses") || trimmed.contains("\"type\":\"function\"")
                || trimmed.contains("\"function\":")) {
            try {
                int start = trimmed.indexOf('{');
                int end = trimmed.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    String jsonStr = trimmed.substring(start, end + 1);
                    org.json.JSONObject root = new org.json.JSONObject(jsonStr);

                    String fnName = "";
                    org.json.JSONObject params = null;

                    if (root.has("function")) {
                        Object fnObj = root.get("function");
                        if (fnObj instanceof String) {
                            fnName = (String) fnObj;
                        } else if (fnObj instanceof org.json.JSONObject) {
                            org.json.JSONObject fnJson = (org.json.JSONObject) fnObj;
                            fnName = fnJson.optString("name", "");
                            if (fnJson.has("parameters")) {
                                params = fnJson.optJSONObject("parameters");
                            } else if (fnJson.has("arguments")) {
                                params = fnJson.optJSONObject("arguments");
                            }
                        }
                    }
                    if (fnName.isEmpty() && root.has("name")) {
                        fnName = root.optString("name", "");
                    }

                    if (params == null) {
                        if (root.has("parameters") && root.get("parameters") instanceof org.json.JSONObject) {
                            params = root.optJSONObject("parameters");
                        } else if (root.has("arguments") && root.get("arguments") instanceof org.json.JSONObject) {
                            params = root.optJSONObject("arguments");
                        } else {
                            params = new org.json.JSONObject();
                        }
                    }

                    System.out.println("🔧 [ChatController Tool Interceptor] Auto-executing Tool: '" + fnName + "', Params: " + params);

                    if ("searchProductsByName".equalsIgnoreCase(fnName)) {
                        String query = params.optString("name", params.optString("query", ""));
                        List<ChatToolsService.ProductSummary> list = tools.searchProductsByName(query);
                        return formatProductSummariesResponse(query, list);
                    } else if ("getAllProducts".equalsIgnoreCase(fnName)) {
                        List<Product> list = tools.getAllProducts();
                        return formatAllProductsResponse(list);
                    } else if ("getProductById".equalsIgnoreCase(fnName)) {
                        String id = params.optString("productId", params.optString("id", ""));
                        Product p = tools.getProductById(id);
                        return formatSingleProductResponse(p);
                    } else if ("getUserAddresses".equalsIgnoreCase(fnName)) {
                        List<com.ecom.Backend.entity.Address> addrs = tools.getUserAddresses();
                        return formatUserAddressesResponse(addrs);
                    } else if ("placeOrder".equalsIgnoreCase(fnName)) {
                        String prodId = params.optString("productId", params.optString("name", ""));
                        int qty = params.optInt("quantity", 1);
                        String addr = params.optString("shippingAddress", params.optString("address", "default"));
                        String method = params.optString("paymentMethod", "RAZORPAY");
                        return tools.placeOrder(prodId, qty, addr, method);
                    } else if ("confirmPayment".equalsIgnoreCase(fnName)) {
                        String orderId = params.optString("orderId", params.optString("id", ""));
                        return tools.confirmPayment(orderId);
                    } else if ("cancelPayment".equalsIgnoreCase(fnName)) {
                        String orderId = params.optString("orderId", params.optString("id", ""));
                        return tools.cancelPayment(orderId);
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ [ChatController Tool Interceptor] Note: Could not parse text as JSON tool call: " + e.getMessage());
            }
        }

        // Clean out any technical tool leakage notes like "Note: I've used the getAllProducts tool..."
        reply = reply.replaceAll("(?i)Note:\\s*I'?ve used the \\w+ tool[^\\n]*\\n*", "");
        reply = reply.replaceAll("(?i)\\[?Tool [^\\]]+\\]?", "");

        return reply;
    }

    private String formatProductSummariesResponse(String query, List<ChatToolsService.ProductSummary> list) {
        if (list == null || list.isEmpty()) {
            return "I couldn't find any products matching **\"" + (query != null ? query : "") + "\"** in our catalog.\n\nWould you like me to show you all available products?";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here are the matching products from our catalog:\n\n");
        int index = 1;
        for (ChatToolsService.ProductSummary p : list) {
            sb.append(index++).append(". **").append(p.name()).append("**\n");
            sb.append("   - **Price:** ₹").append(String.format("%,.2f", p.price())).append("\n");
            if (p.category() != null && !p.category().isEmpty()) {
                sb.append("   - **Category:** ").append(p.category()).append("\n");
            }
            sb.append("   - **Availability:** ").append(p.stock() > 0 ? "In Stock (" + p.stock() + " available)" : "Out of Stock").append("\n\n");
        }
        sb.append("💬 Let me know which one you'd like to explore or if you'd like to place an order!");
        return sb.toString();
    }

    private String formatAllProductsResponse(List<Product> list) {
        if (list == null || list.isEmpty()) {
            return "Our catalog is currently empty. Please check back later!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here are the products available in our catalog:\n\n");
        int index = 1;
        for (Product p : list) {
            sb.append(index++).append(". **").append(p.getName()).append("** - ₹")
                    .append(String.format("%,.2f", p.getPrice()))
                    .append(p.getStock() != null && p.getStock() > 0 ? " *(In Stock)*" : " *(Out of Stock)*")
                    .append("\n");
        }
        sb.append("\n💬 Let me know which one you'd like to explore or purchase!");
        return sb.toString();
    }

    private String formatSingleProductResponse(Product p) {
        if (p == null) {
            return "Sorry, I could not find details for that product.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### 📦 **").append(p.getName()).append("**\n\n");
        sb.append("- **Price:** ₹").append(String.format("%,.2f", p.getPrice())).append("\n");
        if (p.getCategory() != null) {
            sb.append("- **Category:** ").append(p.getCategory()).append("\n");
        }
        sb.append("- **Stock Status:** ").append(p.getStock() != null && p.getStock() > 0 ? "In Stock (" + p.getStock() + " available)" : "Out of Stock").append("\n");
        if (p.getDescription() != null && !p.getDescription().trim().isEmpty()) {
            sb.append("- **Description:** ").append(p.getDescription()).append("\n");
        }
        sb.append("\nWould you like to place an order for this item?");
        return sb.toString();
    }

    private String formatUserAddressesResponse(List<com.ecom.Backend.entity.Address> addrs) {
        if (addrs == null || addrs.isEmpty()) {
            return "You don't have any saved shipping addresses yet. Please provide your delivery address (Street, City, State, ZIP code).";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here are your saved shipping addresses:\n\n");
        int index = 1;
        for (com.ecom.Backend.entity.Address a : addrs) {
            sb.append(index++).append(". ").append(a.getStreetAddress()).append(", ")
                    .append(a.getCity()).append(", ").append(a.getState()).append(" ").append(a.getZipCode())
                    .append(Boolean.TRUE.equals(a.getIsDefault()) ? " *(Default)*" : "")
                    .append("\n");
        }
        sb.append("\nWhich address would you like to use for your order?");
        return sb.toString();
    }
}