package com.ecom.Backend.services;

import com.ecom.Backend.dto.PaymentVerificationRequest;
import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.OrderItem;
import com.ecom.Backend.entity.Payment;
import com.ecom.Backend.entity.Product;
import com.ecom.Backend.repository.OrderRepo;
import com.ecom.Backend.repository.PaymentRepo;
import com.ecom.Backend.repository.ProductRepo;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final PaymentRepo paymentRepo;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * Entry point to verify a payment.
     * Verifies the Razorpay signature and updates the order status.
     * If signature or stock deduction fails, updates order to FAILED/CANCELLED and logs history.
     */
    public void verifyPayment(PaymentVerificationRequest request) {
        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + request.getOrderId()));

        try {
            // 1. Verify Razorpay Signature
            org.json.JSONObject attributes = new org.json.JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());

            Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            // 2. Complete order and deduct stock in a transaction (ACID)
            completeOrderAndDeductStock(order, request);

            // 3. Log successful payment transaction in a separate transaction
            savePaymentTransaction(order.getId(), request, "SUCCESS", null, order.getTotalAmount());

        } catch (Exception e) {
            // If any failure occurs (signature failed, out of stock, MongoDB offline),
            // mark the order as failed/cancelled and log payment failure in database.
            handlePaymentFailure(order.getId(), request, e.getMessage(), order.getTotalAmount());
            throw new RuntimeException("Payment verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates PostgreSQL Order and deducts stock from MongoDB.
     * If MongoDB stock deduction fails, PostgreSQL changes are rolled back automatically.
     */
    @Transactional
    public void completeOrderAndDeductStock(Order order, PaymentVerificationRequest request) {
        // Update order status in PostgreSQL
        order.setPaymentStatus("PAID");
        order.setStatus("PROCESSING");
        order.setRazorpayOrderId(request.getRazorpayOrderId());
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());
        orderRepo.save(order);

        // Deduct MongoDB stock. If any item is out of stock, this throws an exception,
        // which rolls back the PostgreSQL transaction.
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepo.findById(item.getMongodbProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found in MongoDB with ID: " + item.getMongodbProductId()));

            int availableStock = product.getStock() != null ? product.getStock() : 0;
            if (availableStock < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product during payment completion: " + product.getName()
                        + " (Requested: " + item.getQuantity() + ", Available: " + availableStock + ")");
            }

            product.setStock(availableStock - item.getQuantity());
            productRepo.save(product);
        }
    }

    /**
     * Handles payment failure logging and updating order to failed.
     * Runs in a new transaction so it commits even if the parent transaction rolled back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailure(Long orderId, PaymentVerificationRequest request, String errorMessage, Double amount) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order != null) {
            order.setPaymentStatus("FAILED");
            order.setStatus("CANCELLED");
            orderRepo.save(order);
        }

        savePaymentTransaction(orderId, request, "FAILED", errorMessage, amount);
    }

    /**
     * Persists transaction details in the Payment table.
     * Runs in a new transaction to guarantee persistence of logs.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePaymentTransaction(Long orderId, PaymentVerificationRequest request, String status, String errorMessage, Double amount) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentMethod("RAZORPAY");
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setErrorMessage(errorMessage);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(payment);
    }
}
