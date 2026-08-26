package com.ecom.Backend.services;

import com.ecom.Backend.dto.OrderRequest;
import com.ecom.Backend.entity.Order;
import com.ecom.Backend.entity.OrderItem;
import com.ecom.Backend.entity.Product;
import com.ecom.Backend.entity.User;
import com.ecom.Backend.repository.OrderRepo;
import com.ecom.Backend.repository.ProductRepo;
import com.ecom.Backend.repository.UserRepo;
import com.ecom.Backend.entity.Address;
import com.ecom.Backend.repository.AddressRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServices {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final AddressRepo addressRepo;

    @Transactional
    public Order placeOrder(Long userId, OrderRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        String shippingAddress = request.getShippingAddress();
        if (shippingAddress == null || shippingAddress.trim().isEmpty() || "default".equalsIgnoreCase(shippingAddress.trim())) {
            Address defaultAddress = addressRepo.findByUserIdAndIsDefaultTrue(userId)
                    .orElseThrow(() -> new RuntimeException("No shipping address was provided, and no default shipping address was found for this user. Please add an address first."));
            shippingAddress = defaultAddress.getStreetAddress() + ", " + defaultAddress.getCity() + ", " + defaultAddress.getState() + " " + defaultAddress.getZipCode();
        }
        order.setShippingAddress(shippingAddress);

        order.setPaymentStatus("PENDING");
        order.setPaymentMethod(request.getPaymentMethod());

        double totalAmount = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();

        for (OrderRequest.OrderItemDto itemDto : request.getItems()) {
            Product product = productRepo.findById(itemDto.getMongodbProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemDto.getMongodbProductId()));

            int availableStock = product.getStock() != null ? product.getStock() : 0;
            if (availableStock < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() 
                        + " (Requested: " + itemDto.getQuantity() + ", Available: " + availableStock + ")");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getProductId());
            orderItem.setMongodbProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(product.getPrice());

            totalAmount += product.getPrice() * itemDto.getQuantity();
            orderItems.add(orderItem);
            productsToUpdate.add(product);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // First flush to PostgreSQL to trigger constraint/DB failures before modifying MongoDB
        Order savedOrder = orderRepo.saveAndFlush(order);

        // Deduct MongoDB stock since DB save succeeded
        for (int i = 0; i < request.getItems().size(); i++) {
            Product product = productsToUpdate.get(i);
            int orderQty = request.getItems().get(i).getQuantity();
            product.setStock(product.getStock() - orderQty);
            productRepo.save(product);
        }

        return savedOrder;
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }
}
