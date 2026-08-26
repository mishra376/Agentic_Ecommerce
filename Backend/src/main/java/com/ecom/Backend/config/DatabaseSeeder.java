package com.ecom.Backend.config;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepo productRepo;

    @Override
    public void run(String... args) throws Exception {
        if (productRepo.count() == 0) {
            System.out.println(">>> Seeding MongoDB database with initial catalog products...");
            
            Product p1 = new Product();
            p1.setId("macbook-air-m2");
            p1.setProductId(101L);
            p1.setMerchantId(1L);
            p1.setName("Apple MacBook Air M2");
            p1.setPrice(99000.0);
            p1.setDescription("Apple MacBook Air laptop with M2 chip, 8GB RAM, 256GB SSD, and 13.6-inch Liquid Retina Display.");
            p1.setCategory("Laptop");
            p1.setAttributes(new HashMap<>());
            p1.setVariants(new ArrayList<>());
            p1.setImages(new ArrayList<>());
            p1.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p1);

            Product p2 = new Product();
            p2.setId("macbook-pro-14");
            p2.setProductId(102L);
            p2.setMerchantId(1L);
            p2.setName("Apple MacBook Pro 14");
            p2.setPrice(169000.0);
            p2.setDescription("Apple MacBook Pro laptop with M3 chip, 16GB Unified Memory, 512GB SSD, and Liquid Retina XDR Display.");
            p2.setCategory("Laptop");
            p2.setAttributes(new HashMap<>());
            p2.setVariants(new ArrayList<>());
            p2.setImages(new ArrayList<>());
            p2.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p2);

            Product p3 = new Product();
            p3.setId("lenovo-legion-5");
            p3.setProductId(103L);
            p3.setMerchantId(1L);
            p3.setName("Lenovo Legion 5 Gaming Laptop");
            p3.setPrice(78000.0);
            p3.setDescription("Gaming laptop with AMD Ryzen 7 5800H processor, NVIDIA GeForce RTX 3060, 16GB RAM, and 512GB SSD.");
            p3.setCategory("Laptop");
            p3.setAttributes(new HashMap<>());
            p3.setVariants(new ArrayList<>());
            p3.setImages(new ArrayList<>());
            p3.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p3);

            Product p4 = new Product();
            p4.setId("iphone-15");
            p4.setProductId(104L);
            p4.setMerchantId(2L);
            p4.setName("Apple iPhone 15");
            p4.setPrice(79000.0);
            p4.setDescription("Apple iPhone 15 smartphone with 128GB storage, A16 Bionic chip, and 48MP Advanced Camera system.");
            p4.setCategory("Smartphone");
            p4.setAttributes(new HashMap<>());
            p4.setVariants(new ArrayList<>());
            p4.setImages(new ArrayList<>());
            p4.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p4);

            Product p5 = new Product();
            p5.setId("sony-xm5");
            p5.setProductId(105L);
            p5.setMerchantId(2L);
            p5.setName("Sony WH-1000XM5 Noise Cancelling Headphones");
            p5.setPrice(29999.0);
            p5.setDescription("Sony over-ear wireless headphones with industry-leading noise cancellation, 30-hour battery life, and Alexa built-in.");
            p5.setCategory("Headphones");
            p5.setAttributes(new HashMap<>());
            p5.setVariants(new ArrayList<>());
            p5.setImages(new ArrayList<>());
            p5.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p5);

            System.out.println(">>> Database seeding completed. 5 products saved.");
        } else {
            System.out.println(">>> Database already has " + productRepo.count() + " products. Skipping seeding.");
        }
    }
}
