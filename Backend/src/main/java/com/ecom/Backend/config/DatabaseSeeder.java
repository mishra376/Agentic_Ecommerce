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
            p1.setStock(50);
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
            p2.setStock(30);
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
            p3.setStock(25);
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
            p4.setStock(40);
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
            p5.setStock(60);
            p5.setAttributes(new HashMap<>());
            p5.setVariants(new ArrayList<>());
            p5.setImages(new ArrayList<>());
            p5.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p5);

            Product p6 = new Product();
            p6.setId("macbook-extended-warranty");
            p6.setProductId(106L);
            p6.setMerchantId(1L);
            p6.setName("AppleCare+ Extended Warranty for MacBook (2 Years)");
            p6.setPrice(14900.0);
            p6.setDescription("Extend your MacBook warranty by 2 additional years. Covers hardware repairs, accidental damage (up to 2 incidents), and battery replacement if capacity falls below 80%.");
            p6.setCategory("Warranty & Protection");
            p6.setStock(999);
            p6.setAttributes(new HashMap<>());
            p6.setVariants(new ArrayList<>());
            p6.setImages(new ArrayList<>());
            p6.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p6);

            Product p7 = new Product();
            p7.setId("macbook-hard-case");
            p7.setProductId(107L);
            p7.setMerchantId(1L);
            p7.setName("MacBook Hard Plastic Protective Case (Clear Matte)");
            p7.setPrice(1499.0);
            p7.setDescription("Snap-on hard plastic shell case for MacBook Air/Pro 13-14 inch. Crystal clear matte finish, scratch-resistant, ventilated bottom for heat dissipation. Slim profile, does not add bulk.");
            p7.setCategory("Laptop Accessories");
            p7.setStock(120);
            p7.setAttributes(new HashMap<>());
            p7.setVariants(new ArrayList<>());
            p7.setImages(new ArrayList<>());
            p7.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p7);

            Product p8 = new Product();
            p8.setId("usb-c-hub-7in1");
            p8.setProductId(108L);
            p8.setMerchantId(2L);
            p8.setName("7-in-1 USB-C Hub Adapter");
            p8.setPrice(2499.0);
            p8.setDescription("USB-C multiport adapter with HDMI 4K@60Hz, 2× USB-A 3.0, USB-C PD 100W pass-through charging, SD/TF card reader, and Gigabit Ethernet. Compatible with MacBook, Dell XPS, and all USB-C laptops.");
            p8.setCategory("Laptop Accessories");
            p8.setStock(80);
            p8.setAttributes(new HashMap<>());
            p8.setVariants(new ArrayList<>());
            p8.setImages(new ArrayList<>());
            p8.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p8);

            Product p9 = new Product();
            p9.setId("ergonomic-laptop-stand");
            p9.setProductId(109L);
            p9.setMerchantId(2L);
            p9.setName("Aluminium Ergonomic Laptop Stand");
            p9.setPrice(3999.0);
            p9.setDescription("Premium aluminium laptop stand with adjustable height and angle. Raises screen to eye level for better posture. Anti-slip silicone pads, supports laptops up to 17 inches. Foldable and portable.");
            p9.setCategory("Laptop Accessories");
            p9.setStock(55);
            p9.setAttributes(new HashMap<>());
            p9.setVariants(new ArrayList<>());
            p9.setImages(new ArrayList<>());
            p9.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p9);

            Product p10 = new Product();
            p10.setId("wireless-mouse-logitech");
            p10.setProductId(110L);
            p10.setMerchantId(2L);
            p10.setName("Logitech MX Master 3S Wireless Mouse");
            p10.setPrice(8999.0);
            p10.setDescription("Advanced wireless mouse with MagSpeed electromagnetic scroll, 8K DPI sensor, USB-C quick charging, quiet clicks, and multi-device support via Bluetooth or USB receiver. Works on any surface including glass.");
            p10.setCategory("Laptop Accessories");
            p10.setStock(70);
            p10.setAttributes(new HashMap<>());
            p10.setVariants(new ArrayList<>());
            p10.setImages(new ArrayList<>());
            p10.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p10);

            Product p11 = new Product();
            p11.setId("iphone-screen-protector");
            p11.setProductId(111L);
            p11.setMerchantId(2L);
            p11.setName("iPhone 15 Tempered Glass Screen Protector (2-Pack)");
            p11.setPrice(499.0);
            p11.setDescription("9H hardness tempered glass screen protector for iPhone 15. Edge-to-edge coverage, anti-fingerprint oleophobic coating, bubble-free installation. Includes alignment frame and cleaning kit.");
            p11.setCategory("Phone Accessories");
            p11.setStock(200);
            p11.setAttributes(new HashMap<>());
            p11.setVariants(new ArrayList<>());
            p11.setImages(new ArrayList<>());
            p11.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p11);

            Product p12 = new Product();
            p12.setId("iphone-silicone-case");
            p12.setProductId(112L);
            p12.setMerchantId(2L);
            p12.setName("Apple iPhone 15 Silicone Case — Midnight");
            p12.setPrice(3999.0);
            p12.setDescription("Official Apple silicone case for iPhone 15. Soft-touch exterior with microfiber lining for added protection. Supports MagSafe wireless charging. Available in Midnight color.");
            p12.setCategory("Phone Accessories");
            p12.setStock(90);
            p12.setAttributes(new HashMap<>());
            p12.setVariants(new ArrayList<>());
            p12.setImages(new ArrayList<>());
            p12.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p12);

            Product p13 = new Product();
            p13.setId("laptop-backpack");
            p13.setProductId(113L);
            p13.setMerchantId(1L);
            p13.setName("Premium Laptop Backpack — Water Resistant (15.6 inch)");
            p13.setPrice(2799.0);
            p13.setDescription("Durable water-resistant laptop backpack with padded compartment for up to 15.6-inch laptops. Features USB charging port, anti-theft hidden zipper, organizer pockets, and breathable back panel. Ideal for commuters and travelers.");
            p13.setCategory("Laptop Accessories");
            p13.setStock(100);
            p13.setAttributes(new HashMap<>());
            p13.setVariants(new ArrayList<>());
            p13.setImages(new ArrayList<>());
            p13.setUpdatedAt(LocalDateTime.now());
            productRepo.save(p13);

            System.out.println(">>> Database seeding completed. 13 products saved.");
        } else {
            System.out.println(">>> Database already has " + productRepo.count() + " products. Skipping seeding.");
        }
    }
}
