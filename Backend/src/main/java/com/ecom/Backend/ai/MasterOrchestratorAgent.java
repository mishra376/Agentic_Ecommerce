package com.ecom.Backend.ai;

import com.ecom.Backend.ai.agents.OrderAgent;
import com.ecom.Backend.ai.agents.PaymentAgent;
import com.ecom.Backend.ai.agents.ProductAgent;
import com.ecom.Backend.ai.agents.RecommendationAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Master Orchestrator Agent — the brain that routes user requests to specialist agents.
 * 
 * Receives user messages with conversation history, decides which specialist agent
 * to call using a single routeToAgent tool, and composes the final user-facing response.
 * Supports multi-step flows (e.g., product lookup → order → recommendations in a single turn).
 */
@Component
public class MasterOrchestratorAgent {

    private final ChatClient chatClient;
    private final ProductAgent productAgent;
    private final OrderAgent orderAgent;
    private final PaymentAgent paymentAgent;
    private final RecommendationAgent recommendationAgent;

    private static final String SYSTEM_PROMPT = """
            You are the Master AI Orchestrator for an e-commerce platform.

            You do NOT answer product, order, or payment questions yourself. Instead, you delegate tasks to specialist agents using the `routeToAgent` tool.

            AVAILABLE AGENTS:
            - PRODUCT: Search, browse, get product details. Use for any product-related queries.
            - ORDER: Prepare orders, confirm orders, cancel orders, view order history. Use for anything related to purchasing.
            - PAYMENT: Check payment status, view payment history, explain Razorpay payment workflow. Use for payment-related queries or questions about how payments work.
            - RECOMMENDATION: Suggest complementary products after a purchase. Always call this after prepareOrder succeeds.

            HOW TO USE `routeToAgent`:
            1. Analyze the user's message and determine which agent should handle it.
            2. Call `routeToAgent(agentName, taskDescription)` with:
               - `agentName`: One of PRODUCT, ORDER, PAYMENT, RECOMMENDATION
               - `taskDescription`: A clear, detailed task description for the agent. Include all relevant context the agent needs (user ID, product IDs, budget, etc.)
            3. You will receive the agent's response. Use it to compose your final reply to the user.

            MULTI-STEP FLOWS:
            - If a task requires multiple agents, call them sequentially. For example:
              1. Call PRODUCT to find product IDs
              2. Call ORDER with those IDs to prepare the order
              3. Call RECOMMENDATION after order preparation
            - You can make multiple `routeToAgent` calls in a single turn.

            RECOMMENDATION FLOW:
            - Call RECOMMENDATION agent ONLY if there are genuine, direct accessories matching the purchased item (e.g. laptop bag for laptop, screen protector for phone).
            - Do NOT recommend unrelated main high-ticket electronics (e.g. NEVER recommend laptops or smartphones when buying headphones).
            - If RECOMMENDATION agent returns "NO_GENUINE_RECOMMENDATIONS" or no genuine match exists, OMIT recommendations completely from your response. Do not output vague or irrelevant recommendations!

            ORDER & PAYMENT SELECTION FLOW:
            1. When user asks to buy/order a product, route to ORDER agent to prepare the order.
            2. The ORDER agent will return the Order Summary and ask the user to choose their payment method: **COD (Cash on Delivery)** or **Razorpay**.
            3. When the user specifies their payment choice (e.g. "Razorpay", "COD", "pay via razorpay", "cash on delivery"), route to ORDER agent with task: "Confirm order for userId X with paymentMethod Y (where Y is RAZORPAY or COD)".
            4. If user chose Razorpay, the ORDER agent returns the Razorpay payment tag which automatically opens the Razorpay checkout window on screen.

            IMPORTANT RULES:
            - ALWAYS include `[userId: X]` in the task description so agents know which user to act on.
            - You have the full conversation history. Use it to understand context like "yes confirm", "razorpay", "COD", "the first one", "that laptop".
            - When the user confirms or specifies payment method or cancels an order, route to ORDER agent with a clear instruction.
            - Present the final response in a natural, conversational, friendly chat style.
            - Format prices with ₹ symbol.
            - For general greetings or non-shopping questions, respond directly without calling any agent.
            """;

    public MasterOrchestratorAgent(ChatModel chatModel, ProductAgent productAgent,
                                    OrderAgent orderAgent, PaymentAgent paymentAgent,
                                    RecommendationAgent recommendationAgent) {
        this.productAgent = productAgent;
        this.orderAgent = orderAgent;
        this.paymentAgent = paymentAgent;
        this.recommendationAgent = recommendationAgent;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(new MasterTools(productAgent, orderAgent, paymentAgent, recommendationAgent))
                .build();
    }

    /**
     * DTO for a single message in conversation history.
     */
    public record HistoryMessage(String sender, String text) {}

    /**
     * Process a user message with conversation history and return the final response.
     *
     * @param currentMessage the user's current message
     * @param history previous conversation messages
     * @param userId the authenticated user's ID
     * @return the final AI-generated response
     */
    public String chat(String currentMessage, List<HistoryMessage> history, Long userId) {
        System.out.println(">>> [Master] Received message from userId " + userId + ": " + currentMessage);

        try {
            // Build message list with conversation history
            List<Message> messages = new ArrayList<>();

            // Inject userId into the system context so the master knows who the user is
            messages.add(new SystemMessage(SYSTEM_PROMPT + "\n\nCurrent user ID: " + userId));

            // Add conversation history
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
                    .call()
                    .content();

            System.out.println(">>> [Master] Response generated.");
            return reply != null ? reply : "### Error\n\nNo response received from the AI model.";
        } catch (Exception e) {
            System.err.println(">>> [Master] Error: " + e.getMessage());
            e.printStackTrace();
            return "### Error\n\nFailed to generate response: " + e.getMessage();
        }
    }

    // ─── Tool definitions (the single routeToAgent tool) ─────────────────

    public static class MasterTools {
        private final ProductAgent productAgent;
        private final OrderAgent orderAgent;
        private final PaymentAgent paymentAgent;
        private final RecommendationAgent recommendationAgent;

        public MasterTools(ProductAgent productAgent, OrderAgent orderAgent,
                           PaymentAgent paymentAgent, RecommendationAgent recommendationAgent) {
            this.productAgent = productAgent;
            this.orderAgent = orderAgent;
            this.paymentAgent = paymentAgent;
            this.recommendationAgent = recommendationAgent;
        }

        @Tool(description = """
                Route a task to a specialist agent. This is your ONLY tool. Use it to delegate work to the appropriate agent.
                
                Available agents:
                - PRODUCT: Search, browse, get product details
                - ORDER: Prepare orders, confirm, cancel, view order history
                - PAYMENT: Check payment status, view payment history
                - RECOMMENDATION: Suggest complementary products after a purchase. Always call this after prepareOrder succeeds. Pass the product details, category, current order total, and any budget constraint the user mentioned.
                
                Always include [userId: X] in the taskDescription so the agent knows which user to operate on.
                """)
        public String routeToAgent(
                @ToolParam(description = "The agent to route to: PRODUCT, ORDER, PAYMENT, or RECOMMENDATION") String agentName,
                @ToolParam(description = "A detailed task description for the agent. Include all relevant context: user ID, product IDs, quantities, addresses, budget constraints, etc.") String taskDescription) {
            System.out.println(">>> [Master] Routing to " + agentName + " agent with task: " + taskDescription);

            return switch (agentName.toUpperCase().trim()) {
                case "PRODUCT" -> productAgent.execute(taskDescription);
                case "ORDER" -> orderAgent.execute(taskDescription);
                case "PAYMENT" -> paymentAgent.execute(taskDescription);
                case "RECOMMENDATION" -> recommendationAgent.execute(taskDescription);
                default -> "Error: Unknown agent '" + agentName + "'. Available agents: PRODUCT, ORDER, PAYMENT, RECOMMENDATION.";
            };
        }
    }
}
