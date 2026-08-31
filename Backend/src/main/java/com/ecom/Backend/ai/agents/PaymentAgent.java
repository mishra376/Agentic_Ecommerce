package com.ecom.Backend.ai.agents;

import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.Payment;
import com.ecom.Backend.repository.PaymentRepo;
import com.ecom.Backend.services.OrderServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Payment Specialist Agent.
 * Handles payment status checks and payment history queries.
 */
@Component
public class PaymentAgent {

    private final ChatClient chatClient;
    private final OrderServices orderServices;
    private final PaymentRepo paymentRepo;

    private static final String SYSTEM_PROMPT = """
            You are a Payment Specialist for an e-commerce platform.

            Your job is to check payment statuses and provide payment information for orders.

            RULES:
            1. Use `getPaymentStatus` to check the payment status of a specific order by its order ID.
            2. Use `getPaymentHistory` to retrieve all payment transactions for a user.
            3. Present payment information clearly: order ID, payment method, status (SUCCESS/FAILED/PENDING), amount, and timestamps.
            4. If a payment has failed, include the error message if available.
            5. For Razorpay payments, include the Razorpay order ID and payment ID when available.

            Return raw results. The orchestrator will format the final user-facing message.
            """;

    public PaymentAgent(ChatModel chatModel, OrderServices orderServices, PaymentRepo paymentRepo) {
        this.orderServices = orderServices;
        this.paymentRepo = paymentRepo;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(new PaymentTools(orderServices, paymentRepo))
                .build();
    }

    /**
     * Execute a task delegated by the Master orchestrator.
     *
     * @param taskDescription the task description crafted by the Master AI
     * @return the agent's response with payment information
     */
    public String execute(String taskDescription) {
        System.out.println(">>> [PaymentAgent] Received task: " + taskDescription);
        try {
            String result = chatClient.prompt()
                    .user(taskDescription)
                    .call()
                    .content();
            System.out.println(">>> [PaymentAgent] Task completed.");
            return result != null ? result : "No response from Payment Agent.";
        } catch (Exception e) {
            System.err.println(">>> [PaymentAgent] Error: " + e.getMessage());
            return "Payment Agent error: " + e.getMessage();
        }
    }

    // ─── Tool definitions ────────────────────────────────────────────────

    public static class PaymentTools {
        private final OrderServices orderServices;
        private final PaymentRepo paymentRepo;

        public PaymentTools(OrderServices orderServices, PaymentRepo paymentRepo) {
            this.orderServices = orderServices;
            this.paymentRepo = paymentRepo;
        }

        @Tool(description = "Get the payment status and details for a specific order by its order ID.")
        public List<Payment> getPaymentStatus(
                @ToolParam(description = "The order ID to check payment status for.") Long orderId) {
            System.out.println(">>> [PaymentAgent Tool] getPaymentStatus invoked for orderId: " + orderId);
            List<Payment> payments = paymentRepo.findByOrderId(orderId);
            System.out.println(">>> [PaymentAgent Tool] Found " + payments.size() + " payment records.");
            return payments;
        }

        @Tool(description = "Get the complete payment history for a user by looking up all their orders and associated payments.")
        public List<Payment> getPaymentHistory(
                @ToolParam(description = "The user's ID to retrieve payment history for.") Long userId) {
            System.out.println(">>> [PaymentAgent Tool] getPaymentHistory invoked for userId: " + userId);
            List<Order> userOrders = orderServices.getOrdersByUser(userId);
            List<Payment> allPayments = new java.util.ArrayList<>();
            for (Order order : userOrders) {
                allPayments.addAll(paymentRepo.findByOrderId(order.getId()));
            }
            System.out.println(">>> [PaymentAgent Tool] Found " + allPayments.size() + " total payment records.");
            return allPayments;
        }
    }
}
