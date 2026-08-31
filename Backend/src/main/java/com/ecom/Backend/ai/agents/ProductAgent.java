package com.ecom.Backend.ai.agents;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Product Specialist Agent.
 * Handles all product-related queries: browsing, searching, and fetching details.
 */
@Component
public class ProductAgent {

    private final ChatClient chatClient;
    private final ProductServices productServices;

    private static final String SYSTEM_PROMPT = """
            You are a Product Specialist for an e-commerce platform.

            Your job is to search, browse, and provide detailed product information to help the user find what they need.

            RULES:
            1. Use your tools to search and browse the product catalog.
            2. When the user asks to see products, search by name/category, or asks for details about a specific product, use the appropriate tool.
            3. Always present product information clearly, including: name, price (₹), category, stock availability, and a brief description.
            4. When listing multiple products, format them as a numbered list for easy reference.
            5. Include the MongoDB product ID in your response so other agents can reference it.
            6. If no products match a search, say so clearly and suggest broadening the search.

            Respond with the raw product information and results. The orchestrator will format the final user-facing message.
            """;

    public ProductAgent(ChatModel chatModel, ProductServices productServices) {
        this.productServices = productServices;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(new ProductTools(productServices))
                .build();
    }

    /**
     * Execute a task delegated by the Master orchestrator.
     *
     * @param taskDescription the task description crafted by the Master AI
     * @return the agent's response with product information
     */
    public String execute(String taskDescription) {
        System.out.println(">>> [ProductAgent] Received task: " + taskDescription);
        try {
            String result = chatClient.prompt()
                    .user(taskDescription)
                    .call()
                    .content();
            System.out.println(">>> [ProductAgent] Task completed.");
            return result != null ? result : "No response from Product Agent.";
        } catch (Exception e) {
            System.err.println(">>> [ProductAgent] Error: " + e.getMessage());
            return "Product Agent error: " + e.getMessage();
        }
    }

    // ─── Tool definitions ────────────────────────────────────────────────

    public static class ProductTools {
        private final ProductServices productServices;

        public ProductTools(ProductServices productServices) {
            this.productServices = productServices;
        }

        @Tool(description = "Get the complete list of all products in the e-commerce catalog database.")
        public List<Product> getAllProducts() {
            System.out.println(">>> [ProductAgent Tool] getAllProducts invoked.");
            List<Product> results = productServices.getAllProducts();
            System.out.println(">>> [ProductAgent Tool] Returned " + results.size() + " products.");
            return results;
        }

        @Tool(description = "Search products by name/description query, category, and/or maximum price. All parameters are optional — pass null to skip a filter.")
        public List<Product> searchProducts(
                @ToolParam(description = "Text to search in product name and description. Pass null to skip.") String query,
                @ToolParam(description = "Category to filter by. Pass null to skip.") String category,
                @ToolParam(description = "Maximum price filter. Pass null to skip.") Double maxPrice) {
            System.out.println(">>> [ProductAgent Tool] searchProducts invoked — query: " + query + ", category: " + category + ", maxPrice: " + maxPrice);
            List<Product> results = productServices.searchProducts(query, category, maxPrice);
            System.out.println(">>> [ProductAgent Tool] Returned " + results.size() + " matching products.");
            return results;
        }

        @Tool(description = "Get detailed information about a specific product by its MongoDB document ID.")
        public Product getProductById(
                @ToolParam(description = "The MongoDB document ID of the product.") String id) {
            System.out.println(">>> [ProductAgent Tool] getProductById invoked — id: " + id);
            Optional<Product> product = productServices.getProductById(id);
            if (product.isPresent()) {
                System.out.println(">>> [ProductAgent Tool] Found product: " + product.get().getName());
                return product.get();
            }
            System.out.println(">>> [ProductAgent Tool] Product not found with id: " + id);
            return null;
        }
    }
}
