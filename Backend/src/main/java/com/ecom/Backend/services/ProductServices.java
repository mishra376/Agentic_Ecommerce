package com.ecom.Backend.services;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.repository.ProductRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProductServices {

    private final ProductRepo productRepo;

    public Product saveProduct(Product product) {
        product.setUpdatedAt(LocalDateTime.now());
        return productRepo.save(product);
    }

    public Optional<Product> getProductById(String id) {
        return productRepo.findById(id);
    }

    public Optional<Product> getProductByProductId(Long productId) {
        return productRepo.findByProductId(productId);
    }

    public List<Product> getProductsByMerchantId(Long merchantId) {
        return productRepo.findByMerchantId(merchantId);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public void deleteProduct(String id) {
        productRepo.deleteById(id);
    }
}
