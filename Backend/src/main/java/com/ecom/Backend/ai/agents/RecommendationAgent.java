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

            Your job is to suggest complementary products that pair well with what the user is buying — like a laptop bag with a laptop, or a phone case with a phone.

            RULES:
            1. Use your tools to browse the catalog and find products that logically complement the items being purchased.
            2. BUDGET CONSTRAINT: If a budget limit is provided, the total of (current order + your suggestions) MUST NOT exceed it.
               - Calculate: remaining_budget = budget_limit - current_order_total
               - Only suggest products priced ≤ remaining_budget
               - If multiple suggestions, their combined total must stay within remaining_budget
               - If no budget fits, say "Your budget is fully allocated — no add-ons to suggest!"
            3. Suggest 2-4 products max. Quality over quantity.
            4. For each suggestion, explain WHY it pairs well with the purchase.
            5. Never suggest the same product the user is already buying.
            6. Present each suggestion with: product name, price (₹), and reason for pairing.

            Return your suggestions clearly formatted. The orchestrator will include them in the final response.
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
