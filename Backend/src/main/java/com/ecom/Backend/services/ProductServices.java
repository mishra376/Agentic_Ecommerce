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
                        if (!pCat.contains(cat) && !cat.contains(pCat)) {
                            return false;
                        }
                    }
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.toLowerCase().trim();
                        String name = p.getName() != null ? p.getName().toLowerCase() : "";
                        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        String pCat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                        String combinedText = name + " " + desc + " " + pCat;

                        // Check full query match first
                        if (combinedText.contains(q)) {
                            // match found
                        } else {
                            // Check individual keywords with singular/plural support
                            String[] keywords = q.split("\\s+");
                            boolean allKeywordsMatch = true;
                            for (String kw : keywords) {
                                if (kw.isEmpty()) continue;
                                boolean matched = combinedText.contains(kw);
                                if (!matched && kw.endsWith("s") && kw.length() > 3) {
                                    matched = combinedText.contains(kw.substring(0, kw.length() - 1));
                                }
                                if (!matched && kw.endsWith("es") && kw.length() > 4) {
                                    matched = combinedText.contains(kw.substring(0, kw.length() - 2));
                                }
                                if (!matched) {
                                    allKeywordsMatch = false;
                                    break;
                                }
                            }
                            if (!allKeywordsMatch) {
                                return false;
                            }
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
