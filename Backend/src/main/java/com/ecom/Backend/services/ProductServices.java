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

    public List<Product> searchProducts(String query, String category, Double maxPrice) {
        return productRepo.findAll().stream()
                .filter(p -> {
                    if (category != null && !category.trim().isEmpty()) {
                        String cat = category.toLowerCase().trim();
                        String pCat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                        if (!pCat.contains(cat)) {
                            return false;
                        }
                    }
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.toLowerCase().trim();
                        String name = p.getName() != null ? p.getName().toLowerCase() : "";
                        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        if (!name.contains(q) && !desc.contains(q)) {
                            return false;
                        }
                    }
                    if (maxPrice != null) {
                        if (p.getPrice() == null || p.getPrice() > maxPrice) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    public void deleteProduct(String id) {
        productRepo.deleteById(id);
    }
}
