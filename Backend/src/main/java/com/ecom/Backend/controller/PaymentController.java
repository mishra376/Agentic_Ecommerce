package com.ecom.Backend.controller;

import com.ecom.Backend.dto.PaymentVerificationRequest;
import com.ecom.Backend.security.CustomUserDetails;
import com.ecom.Backend.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        return ResponseEntity.ok(Map.of("keyId", razorpayKeyId));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentVerificationRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            paymentService.verifyPayment(request);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "FAILED",
                    "message", "Payment verification failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/fail")
    public ResponseEntity<?> reportPaymentFailure(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> payload
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            String errorMessage = payload.getOrDefault("errorMessage", "Payment failed or cancelled by user").toString();
            paymentService.reportPaymentFailure(orderId, errorMessage);
            return ResponseEntity.ok(Map.of("status", "CANCELLED", "message", "Payment failure recorded cleanly"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
