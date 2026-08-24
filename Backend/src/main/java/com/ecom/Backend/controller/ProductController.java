package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.services.ProductServices;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {

    private final ProductServices productServices;

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productServices.saveProduct(product);
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
