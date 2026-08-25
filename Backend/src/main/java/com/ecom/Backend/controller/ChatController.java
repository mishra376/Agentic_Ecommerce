package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final ProductServices productServices;

    @Autowired
    public ChatController(ChatModel chatModel, ProductServices productServices) {
        this.chatClient = ChatClient.create(chatModel);
        this.productServices = productServices;
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply, String status) {}

    // Tool definition class containing @Tool annotated methods for Spring AI 2.0
    public static class ProductTools {
        private final ProductServices productServices;

        public ProductTools(ProductServices productServices) {
            this.productServices = productServices;
        }

        @Tool(description = "Search the product database dynamically for matching catalog items based on query/keyword, category, and maximum price budget.")
        public List<Product> searchProducts(String query, String category, Double maxPrice) {
            System.out.println(">>> [AI Native Tool Call] searchProducts invoked with query: '" + query + "', category: '" + category + "', maxPrice: " + maxPrice);
            List<Product> results = productServices.searchProducts(query, category, maxPrice);
            System.out.println(">>> [AI Native Tool Call] Returned " + results.size() + " products from database.");
            return results;
        }
    }

    @PostMapping
    public ChatResponse handleChat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new ChatResponse("I didn't receive any message. How can I help you today?", "success");
        }

        String userMessage = request.message().trim();
        String reply = generateAiResponse(userMessage);
        return new ChatResponse(reply, "success");
    }

    private List<Product> extractAndSearchProducts(String message) {
        String msgLower = message.toLowerCase();
        Double maxPrice = null;

        // 1. Detect max price/budget (e.g. "under 50k" -> 50000, "under 50000" -> 50000)
        java.util.regex.Pattern kPattern = java.util.regex.Pattern.compile("(?:under|below|budget|less\\s+than)?\\s*(\\d+(?:\\.\\d+)?)\\s*k", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher kMatcher = kPattern.matcher(msgLower);
        if (kMatcher.find()) {
            try {
                maxPrice = Double.parseDouble(kMatcher.group(1)) * 1000;
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        if (maxPrice == null) {
            java.util.regex.Pattern numPattern = java.util.regex.Pattern.compile("(?:under|below|budget|less\\s+than|price)?\\s*(\\d+(?:\\.\\d+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher numMatcher = numPattern.matcher(msgLower);
            if (numMatcher.find()) {
                try {
                    maxPrice = Double.parseDouble(numMatcher.group(1));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        // 2. Detect common category/product keywords
        String query = null;
        String[] keywords = {"laptop", "phone", "smartphone", "tv", "camera", "watch", "keyboard", "mouse", "monitor", "headphone", "tablet"};
        for (String kw : keywords) {
            if (msgLower.contains(kw)) {
                query = kw;
                break;
            }
        }

        // Fallback: If no keyword matched, clean up the text to extract the core search subject
        if (query == null) {
            String cleaned = msgLower
                .replaceAll("(?:show|me|i|want|to|buy|looking|for|under|below|budget|less|than|price|\\d+k|\\d+)", "")
                .trim();
            if (!cleaned.isEmpty()) {
                query = cleaned;
            }
        }

        System.out.println(">>> [Pre-search Fallback Parser] Extracted Query: '" + query + "', Max Price: " + maxPrice);
        List<Product> results = productServices.searchProducts(query, null, maxPrice);
        System.out.println(">>> [Pre-search Fallback Parser] Found " + results.size() + " matching products.");
        return results;
    }

    private String generateAiResponse(String message) {
        try {
            // Pre-fetch products using natural language parsing to inject context for models without tool-calling support
            List<Product> products = extractAndSearchProducts(message);
            StringBuilder contextBuilder = new StringBuilder();
            if (!products.isEmpty()) {
                contextBuilder.append("\n\n[CRITICAL CONTEXT - AVAILABLE PRODUCTS IN DATABASE]:\n");
                for (Product p : products) {
                    contextBuilder.append(String.format("- ID: %s, Name: %s, Category: %s, Price: %.2f, Description: %s\n",
                            p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getDescription()));
                }
                contextBuilder.append("\nInstructions relative to the above context:\n");
                contextBuilder.append("1. Recommend ONLY from these actual database products.\n");
                contextBuilder.append("2. Under no circumstances suggest a product that is not listed here.\n");
                contextBuilder.append("3. Format details with Name, Price, direct API link (/api/products/{id}), and reason.\n");
            }

            // System Instructions guiding the AI
            String systemInstruction = """
                    You are an intelligent E-Commerce Assistant.
                    
                    You have access to a tool named `searchProducts` to find matching products from the database.
                    Always use this tool when the user queries about products, asks for laptops, smartphones, prices, or budgets. Do not assume or guess products.
                    
                    Instructions for Product Queries:
                    1. When recommending a product, ensure it actually exists in the database.
                    2. Suggest ONLY from the matching products returned.
                    3. Under no circumstances should you recommend a product that violates the user's constraints (e.g., if a budget is under 50k, do not suggest a 60k laptop; if Ryzen is requested, do not suggest an Intel processor).
                    4. Suggest top 3-4 matching products.
                    5. If no products matching the criteria are available, politely inform the user that no products matching their exact criteria are currently available.
                    6. For each recommended product, strictly provide:
                       - Product Name & Price
                       - Key reason to buy matching their criteria
                       - Direct link/API endpoint to view product: `/api/products/{id}`
                    7. Format the output cleanly in Markdown with bullet points.
                    """ + contextBuilder.toString();

            String reply = chatClient.prompt()
                    .system(systemInstruction)
                    .user(message)
                    .tools(new ProductTools(productServices))
                    .call()
                    .content();

            return reply != null ? reply : "### Error\n\nNo response received from the AI model.";
        } catch (Exception e) {
            e.printStackTrace();
            return "### Error\n\nFailed to generate response: " + e.getMessage();
        }
    }
}