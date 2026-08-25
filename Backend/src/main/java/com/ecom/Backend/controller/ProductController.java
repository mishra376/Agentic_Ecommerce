package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.services.ProductServices;
import com.ecom.Backend.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {

    private final ProductServices productServices;

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestBody Product product,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null || !userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_MERCHANT".equals(auth.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access Denied: Only logged-in merchants can create products.");
        }

        // Set the merchant ID automatically from the authenticated merchant's details
        product.setMerchantId(userDetails.getId());
        Product savedProduct = productServices.saveProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        return productServices.getProductById(id).orElse(null);
    }

    @GetMapping("/productId/{productId}")
    public Product getProductByProductId(@PathVariable Long productId) {
        return productServices.getProductByProductId(productId).orElse(null);
    }

    @GetMapping("/merchant/{merchantId}")
    public List<Product> getProductsByMerchantId(@PathVariable Long merchantId) {
        return productServices.getProductsByMerchantId(merchantId);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productServices.getAllProducts();
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productServices.deleteProduct(id);
    }
}
