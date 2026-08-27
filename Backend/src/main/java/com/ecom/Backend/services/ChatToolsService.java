package com.ecom.Backend.services;

import com.ecom.Backend.entity.Address;
import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.Product;
import com.ecom.Backend.dto.OrderRequest;
import com.ecom.Backend.security.CustomUserDetails;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contains all Spring AI @Tool methods used by the conversational chat agent.
 *
 * This class is NOT a singleton Spring bean — it is instantiated per-request
 * inside the ChatController so that the current authenticated user's details
 * are safely scoped to the request.
 */
public class ChatToolsService {

    private final ProductServices productServices;
    private final OrderServices orderServices;
    private final AddressServices addressServices;
    private final PaymentService paymentService;
    private final CustomUserDetails userDetails;

    public ChatToolsService(
            ProductServices productServices,
            OrderServices orderServices,
            AddressServices addressServices,
            PaymentService paymentService,
            CustomUserDetails userDetails
    ) {
        this.productServices = productServices;
        this.orderServices = orderServices;
        this.addressServices = addressServices;
        this.paymentService = paymentService;
        this.userDetails = userDetails;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Product Tools
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Get the complete list of all products available in the e-commerce catalog. " +
            "Use this when the user asks to see everything or browse all items. " +
            "Returns full product objects with all details.")
    public List<Product> getAllProducts() {
        System.out.println("🛠️  [AI TOOL CALL] getAllProducts()");
        List<Product> results = productServices.getAllProducts();
        System.out.println("📦 [AI TOOL RESULT] getAllProducts found " + results.size() + " total products.");
        return results;
    }

    @Tool(description = "Search for products in the catalog by name or keyword (e.g. 'laptop', 'MacBook', 'shoes', 'iPhone'). " +
            "ALWAYS use this tool first when the user asks for recommendations or mentions any item. " +
            "Returns a list of matching products with id, name, price (in ₹ INR), category, and stock.")
    public List<ProductSummary> searchProductsByName(String name) {
        System.out.println("🛠️  [AI TOOL CALL] searchProductsByName(\"" + name + "\")");
        List<Product> matches = productServices.searchProducts(name, null, null);
        if (matches.isEmpty() && (name == null || name.trim().isEmpty() || "all".equalsIgnoreCase(name.trim()) || "product".equalsIgnoreCase(name.trim()) || "items".equalsIgnoreCase(name.trim()))) {
            matches = productServices.getAllProducts();
        }
        List<ProductSummary> summaries = matches.stream()
                .map(p -> new ProductSummary(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getCategory(),
                        p.getStock() != null ? p.getStock() : 0
                ))
                .collect(Collectors.toList());
        System.out.println("📦 [AI TOOL RESULT] searchProductsByName found " + summaries.size() + " matching products.");
        summaries.forEach(s -> System.out.println("   ▪ Product: " + s.name() + " (ID: " + s.id() + ", Price: ₹" + s.price() + ", Stock: " + s.stock() + ")"));
        return summaries;
    }

    @Tool(description = "Retrieve full details of a specific product by its ID or name. " +
            "Use this after the user has selected a product to get complete information (description, specs, etc.).")
    public Product getProductById(String productId) {
        System.out.println("🛠️  [AI TOOL CALL] getProductById(\"" + productId + "\")");
        Product product = resolveProduct(productId);
        if (product != null) {
            System.out.println("📦 [AI TOOL RESULT] getProductById found: " + product.getName() + " (Price: ₹" + product.getPrice() + ")");
            return product;
        } else {
            System.out.println("⚠️ [AI TOOL RESULT] getProductById: No product found for: " + productId);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Address Tools
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Retrieve all registered shipping addresses for the currently logged-in user. " +
            "Use this when you need to ask the user which address to ship to, " +
            "or to obtain the default address for auto‑fill.")
    public List<Address> getUserAddresses() {
        System.out.println("🛠️  [AI TOOL CALL] getUserAddresses() for user: " + (userDetails != null ? userDetails.getUsername() : "GUEST"));
        if (userDetails == null) {
            System.out.println("⚠️ [AI TOOL RESULT] getUserAddresses: User is not authenticated.");
            return List.of();
        }
        List<Address> results = addressServices.getAddressesByUser(userDetails.getId());
        System.out.println("📍 [AI TOOL RESULT] getUserAddresses returned " + results.size() + " saved addresses.");
        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order Tools
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Place a new order in the database for the user. " +
            "Call this ONLY after the user gives final confirmation. " +
            "Parameters: " +
            "- productId: The product ID (or exact product name). " +
            "- quantity: Number of units (default 1). " +
            "- shippingAddress: The shipping address provided by the user, or 'default'. " +
            "- paymentMethod: Must be either 'RAZORPAY' or 'COD'. " +
            "Returns a detailed order summary and instructions.")
    public String placeOrder(String productId, int quantity, String shippingAddress, String paymentMethod) {
        System.out.println("🛠️  [AI TOOL CALL] placeOrder(productId=\"" + productId + "\", quantity=" + quantity + ", shippingAddress=\"" + shippingAddress + "\", paymentMethod=\"" + paymentMethod + "\")");
        if (userDetails == null) {
            System.out.println("⚠️ [AI TOOL RESULT] placeOrder rejected: User is not logged in.");
            return "❌ You must be logged in to place an order. Please log in to your account first.";
        }
        try {
            Product matchedProduct = resolveProduct(productId);
            if (matchedProduct == null) {
                return "❌ Could not find product '" + productId + "'. Please check the product name or ID from our catalog.";
            }

            int qty = Math.max(1, quantity);
            String addr = (shippingAddress != null && !shippingAddress.trim().isEmpty()) ? shippingAddress.trim() : "default";
            
            // Normalize payment method to either RAZORPAY or COD
            String payMethod = "RAZORPAY";
            if (paymentMethod != null) {
                String pmUpper = paymentMethod.toUpperCase();
                if (pmUpper.contains("COD") || pmUpper.contains("CASH")) {
                    payMethod = "COD";
                }
            }

            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setShippingAddress(addr);
            orderRequest.setPaymentMethod(payMethod);

            List<OrderRequest.OrderItemDto> dtoList = new ArrayList<>();
            OrderRequest.OrderItemDto dto = new OrderRequest.OrderItemDto();
            dto.setMongodbProductId(matchedProduct.getId());
            dto.setQuantity(qty);
            dtoList.add(dto);
            orderRequest.setItems(dtoList);

            Order order = orderServices.placeOrder(userDetails.getId(), orderRequest);
            System.out.println("✅ [AI TOOL RESULT] Order placed successfully! Order ID: #" + order.getId() + ", Amount: ₹" + order.getTotalAmount() + ", Status: " + order.getStatus());
            
            StringBuilder response = new StringBuilder();
            response.append("🎉 **Order Placed Successfully!**\n\n");
            response.append("📦 **Order Summary:**\n");
            response.append("- **Order ID:** #").append(order.getId()).append("\n");
            response.append("- **Product:** ").append(matchedProduct.getName()).append(" (Quantity: ").append(qty).append(")\n");
            response.append("- **Total Amount:** ₹").append(String.format("%,.2f", order.getTotalAmount())).append("\n");
            response.append("- **Shipping Address:** ").append(order.getShippingAddress()).append("\n");
            response.append("- **Payment Method:** ").append("COD".equalsIgnoreCase(payMethod) ? "Cash on Delivery (COD)" : "Razorpay (Online Payment)").append("\n");
            response.append("- **Order Status:** ").append(order.getStatus()).append("\n\n");

            if ("RAZORPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                if (order.getRazorpayOrderId() != null) {
                    response.append("🔑 **Razorpay Order ID:** `").append(order.getRazorpayOrderId()).append("`\n\n");
                }
                response.append("💳 **Payment Authorization Required:**\n");
                response.append("Would you like to authorize payment of **₹")
                        .append(String.format("%,.2f", order.getTotalAmount()))
                        .append("** for Order #").append(order.getId()).append(" now?\n\n")
                        .append("👉 *Reply with **'Yes'** to complete the payment or **'No'** to cancel.*");
            } else {
                response.append("✅ Your Cash on Delivery order is confirmed! It will be processed and delivered to your address shortly. You can pay in cash upon delivery. Thank you for shopping with us!");
            }
            return response.toString();
        } catch (Exception e) {
            System.err.println("❌ [AI TOOL RESULT] placeOrder error: " + e.getMessage());
            return "❌ Error placing order: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment Tools
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(description = "Confirm and complete the Razorpay payment for an order after the user explicitly " +
            "says 'Yes', 'Confirm', 'Proceed', or authorizes payment. " +
            "Parameter: orderId - The order ID string or number (e.g. '1', 'ORD1', '#1').")
    public String confirmPayment(String orderId) {
        System.out.println("🛠️  [AI TOOL CALL] confirmPayment(orderId=\"" + orderId + "\")");
        if (userDetails == null) {
            System.out.println("⚠️ [AI TOOL RESULT] confirmPayment rejected: User not logged in.");
            return "❌ You must be logged in to confirm payment.";
        }
        Long resolvedOrderId = parseOrderId(orderId);
        if (resolvedOrderId == null) {
            return "❌ Could not find a pending order to confirm payment. Please specify the Order ID.";
        }
        try {
            paymentService.completeConversationalPayment(resolvedOrderId);
            System.out.println("✅ [AI TOOL RESULT] Payment confirmed for Order #" + resolvedOrderId + " -> Marked as PAID.");
            return "✅ **Payment Confirmed & Completed!**\n\n" +
                    "Order **#" + resolvedOrderId + "** is now marked as **PAID** and is being prepared for shipping.\n\n" +
                    "Thank you for your purchase! We'll notify you once it's on its way.";
        } catch (Exception e) {
            System.err.println("❌ [AI TOOL RESULT] confirmPayment failed for Order #" + resolvedOrderId + ": " + e.getMessage());
            return "❌ Payment failed: " + e.getMessage() +
                    "\nOrder #" + resolvedOrderId + " has been cancelled. Please try placing a new order.";
        }
    }

    @Tool(description = "Cancel the pending Razorpay payment for an order when the user says " +
            "'No', 'Cancel', 'Decline', or any negative response. " +
            "Parameter: orderId - The order ID string or number.")
    public String cancelPayment(String orderId) {
        System.out.println("🛠️  [AI TOOL CALL] cancelPayment(orderId=\"" + orderId + "\")");
        if (userDetails == null) {
            System.out.println("⚠️ [AI TOOL RESULT] cancelPayment rejected: User not logged in.");
            return "❌ You must be logged in to cancel payment.";
        }
        Long resolvedOrderId = parseOrderId(orderId);
        if (resolvedOrderId == null) {
            return "❌ Could not find a pending order to cancel.";
        }
        try {
            paymentService.cancelConversationalPayment(resolvedOrderId);
            System.out.println("✅ [AI TOOL RESULT] Payment cancelled for Order #" + resolvedOrderId + ".");
            return "❌ **Payment Cancelled.**\n\n" +
                    "Order **#" + resolvedOrderId + "** has been cancelled and no payment was deducted. " +
                    "Let me know if you would like to explore other products or place a new order!";
        } catch (Exception e) {
            System.err.println("❌ [AI TOOL RESULT] cancelPayment error for Order #" + resolvedOrderId + ": " + e.getMessage());
            return "❌ Error cancelling payment: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private Product resolveProduct(String productIdOrName) {
        if (productIdOrName == null || productIdOrName.trim().isEmpty()) {
            return null;
        }
        String query = productIdOrName.trim();

        // 1. Try direct MongoDB ID lookup
        Optional<Product> direct = productServices.getProductById(query);
        if (direct.isPresent()) {
            return direct.get();
        }

        // 2. Try numeric productId
        try {
            String digits = query.replaceAll("\\D+", "");
            if (!digits.isEmpty()) {
                long numId = Long.parseLong(digits);
                Optional<Product> byNum = productServices.getProductByProductId(numId);
                if (byNum.isPresent()) return byNum.get();
            }
        } catch (Exception ignored) {}

        // 3. Try search by query
        List<Product> matches = productServices.searchProducts(query, null, null);
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        // 4. Try matching against all products
        List<Product> all = productServices.getAllProducts();
        for (Product p : all) {
            if (p.getName() != null && (p.getName().equalsIgnoreCase(query) 
                    || p.getName().toLowerCase().contains(query.toLowerCase()) 
                    || query.toLowerCase().contains(p.getName().toLowerCase()))) {
                return p;
            }
        }

        return null;
    }

    private Long parseOrderId(String orderIdStr) {
        if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
            String digits = orderIdStr.replaceAll("\\D+", "");
            if (!digits.isEmpty()) {
                try {
                    return Long.parseLong(digits);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Fallback: If user is authenticated, find their latest pending order
        if (userDetails != null) {
            List<Order> orders = orderServices.getOrdersByUser(userDetails.getId());
            if (orders != null && !orders.isEmpty()) {
                for (int i = orders.size() - 1; i >= 0; i--) {
                    Order o = orders.get(i);
                    if ("PENDING".equalsIgnoreCase(o.getStatus()) || "PENDING".equalsIgnoreCase(o.getPaymentStatus())) {
                        System.out.println("ℹ️ [ChatToolsService] Auto-resolved orderId to recent pending order: #" + o.getId());
                        return o.getId();
                    }
                }
                return orders.get(orders.size() - 1).getId();
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Supporting Records
    // ─────────────────────────────────────────────────────────────────────────

    /** Lightweight product summary returned by searchProductsByName to reduce token usage. */
    public record ProductSummary(String id, String name, Double price, String category, int stock) {}
}