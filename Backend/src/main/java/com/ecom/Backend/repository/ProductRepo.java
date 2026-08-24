package com.ecom.Backend.repository;

import com.ecom.Backend.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepo extends MongoRepository<Product, String> {

    Optional<Product> findByProductId(Long productId);

    List<Product> findByMerchantId(Long merchantId);
}
