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
 * Recommendation Specialist Agent.
 * Suggests complementary products (upselling / cross-selling) after an order is prepared.
 * Uses LLM reasoning to infer "frequently bought together" patterns from the catalog.
 */
@Component
public class RecommendationAgent {

    private final ChatClient chatClient;
    private final ProductServices productServices;

    private static final String SYSTEM_PROMPT = """
            You are a Recommendation Specialist for an e-commerce platform.

            Your job is to suggest ONLY direct, genuine accessories or complements for the specific item the user is purchasing (e.g., a case/warranty/hub for a laptop, or a screen protector/case for a phone).

            STRICT RELEVANCE RULES:
            1. ONLY suggest products that are DIRECT, GENUINE ACCESSORIES to the purchased item.
            2. NEVER recommend unrelated main electronics (e.g., NEVER recommend smartphones, iPhones, MacBooks, or gaming laptops when a user buys headphones).
            3. If there are NO direct, genuine matching accessories in the database catalog for the item being bought, return EXACTLY: "NO_GENUINE_RECOMMENDATIONS".
            4. Do NOT force recommendations if no genuine complement exists. Quality over quantity!
            5. BUDGET CONSTRAINT: If a budget limit is provided, the total of (current order + your suggestions) MUST NOT exceed it.
            6. If multiple genuine accessories fit, suggest 1-3. Present each with product name, price (₹), and why it pairs well.

            If no genuine accessory fits, output: "NO_GENUINE_RECOMMENDATIONS".
            """;

    public RecommendationAgent(ChatModel chatModel, ProductServices productServices) {
        this.productServices = productServices;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(new RecommendationTools(productServices))
                .build();
    }

    /**
     * Execute a task delegated by the Master orchestrator.
     *
     * @param taskDescription the task description with order details, category, budget info
     * @return the agent's response with product recommendations
     */
    public String execute(String taskDescription) {
        System.out.println(">>> [RecommendationAgent] Received task: " + taskDescription);
        try {
            String result = chatClient.prompt()
                    .user(taskDescription)
                    .call()
                    .content();
            System.out.println(">>> [RecommendationAgent] Task completed.");
            return result != null ? result : "No recommendations available.";
        } catch (Exception e) {
            System.err.println(">>> [RecommendationAgent] Error: " + e.getMessage());
            return "Recommendation Agent error: " + e.getMessage();
        }
    }

    // ─── Tool definitions ────────────────────────────────────────────────

    public static class RecommendationTools {
        private final ProductServices productServices;

        public RecommendationTools(ProductServices productServices) {
            this.productServices = productServices;
        }

        @Tool(description = "Get all products in a specific category to find complementary items.")
        public List<Product> getProductsByCategory(
                @ToolParam(description = "The category to search for (e.g., 'Electronics', 'Accessories').") String category) {
            System.out.println(">>> [RecommendationAgent Tool] getProductsByCategory invoked — category: " + category);
            List<Product> results = productServices.searchProducts(null, category, null);
            System.out.println(">>> [RecommendationAgent Tool] Returned " + results.size() + " products in category: " + category);
            return results;
        }

        @Tool(description = "Get the complete list of all products in the catalog to find cross-category complements.")
        public List<Product> getAllProducts() {
            System.out.println(">>> [RecommendationAgent Tool] getAllProducts invoked.");
            List<Product> results = productServices.getAllProducts();
            System.out.println(">>> [RecommendationAgent Tool] Returned " + results.size() + " products.");
            return results;
        }

        @Tool(description = "Get detailed information about a specific product by its MongoDB document ID.")
        public Product getProductById(
                @ToolParam(description = "The MongoDB document ID of the product.") String id) {
            System.out.println(">>> [RecommendationAgent Tool] getProductById invoked — id: " + id);
            Optional<Product> product = productServices.getProductById(id);
            return product.orElse(null);
        }
    }
}
