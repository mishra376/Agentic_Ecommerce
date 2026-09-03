package com.ecom.Backend.config;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepo productRepo;

    @Override
    public void run(String... args) throws Exception {
        if (productRepo.count() < 35) {
            System.out.println(">>> Seeding MongoDB database with initial 35 catalog products...");
            productRepo.deleteAll();
            seedAllProducts();
            System.out.println(">>> Database seeding completed. " + productRepo.count() + " products saved.");
        } else {
            // Ensure no existing product in the database is out of stock
            List<Product> products = productRepo.findAll();
            boolean updated = false;
            for (Product p : products) {
                if (p.getStock() == null || p.getStock() <= 0) {
                    p.setStock(50);
                    p.setUpdatedAt(LocalDateTime.now());
                    productRepo.save(p);
                    updated = true;
                }
            }
            if (updated) {
                System.out.println(">>> Updated out-of-stock products in database to have active stock (50 units).");
            } else {
                System.out.println(">>> Database already has " + productRepo.count() + " products, all in stock. Skipping seeding.");
            }
        }
    }

    private void seedAllProducts() {
        // Laptops & Gaming Laptops
        saveProduct("macbook-air-m2", 101L, 1L, "Apple MacBook Air M2", 99000.0,
                "Apple MacBook Air laptop with M2 chip, 8GB RAM, 256GB SSD, and 13.6-inch Liquid Retina Display.", "Laptop", 50);

        saveProduct("macbook-pro-14", 102L, 1L, "Apple MacBook Pro 14", 169000.0,
                "Apple MacBook Pro laptop with M3 chip, 16GB Unified Memory, 512GB SSD, and Liquid Retina XDR Display.", "Laptop", 30);

        saveProduct("lenovo-legion-5", 103L, 1L, "Lenovo Legion 5 Gaming Laptop", 78000.0,
                "Gaming laptop with AMD Ryzen 7 5800H processor, NVIDIA GeForce RTX 3060, 16GB RAM, and 512GB SSD.", "Laptop", 25);

        saveProduct("asus-rog-strix-g16", 114L, 1L, "ASUS ROG Strix G16 Gaming Laptop", 139990.0,
                "High-performance gaming laptop with 13th Gen Intel Core i7-13650HX, NVIDIA GeForce RTX 4060 8GB, 16GB DDR5 RAM, 1TB PCIe 4.0 SSD, and 16-inch FHD+ 165Hz Display.", "Laptop", 25);

        saveProduct("acer-predator-helios-16", 115L, 1L, "Acer Predator Helios 16 Gaming Laptop", 174990.0,
                "Extreme gaming powerhouse featuring Intel Core i9-13900HX, NVIDIA GeForce RTX 4070 8GB, 32GB DDR5 RAM, 1TB Gen4 SSD, and 16-inch WQXGA 240Hz IPS Display.", "Laptop", 25);

        saveProduct("msi-katana-15", 116L, 1L, "MSI Katana 15 Gaming Laptop", 89990.0,
                "Sleek gaming laptop with Intel Core i7-13620H, NVIDIA GeForce RTX 4050 6GB, 16GB DDR5 RAM, 512GB NVMe SSD, and 15.6-inch Full HD 144Hz Display.", "Laptop", 30);

        saveProduct("hp-omen-16", 117L, 1L, "HP Omen 16 Gaming Laptop", 124990.0,
                "Premium gaming laptop with AMD Ryzen 7 7840HS, NVIDIA GeForce RTX 4060 8GB, 16GB DDR5 RAM, 1TB SSD, and 16.1-inch QHD 165Hz Display.", "Laptop", 25);

        saveProduct("dell-xps-13", 130L, 1L, "Dell XPS 13 Laptop", 134990.0,
                "Ultra-thin premium laptop with Intel Core Ultra 7 processor, 16GB LPDDR5X RAM, 512GB SSD, and 13.4-inch FHD+ InfinityEdge display.", "Laptop", 25);

        // MacBook Guards, Covers & Laptop Accessories
        saveProduct("macbook-hard-case", 107L, 1L, "MacBook Hard Plastic Protective Case (Clear Matte)", 1499.0,
                "Snap-on hard plastic shell case for MacBook Air/Pro 13-14 inch. Crystal clear matte finish, scratch-resistant, ventilated bottom for heat dissipation. Slim profile, does not add bulk.", "Laptop Accessories", 120);

        saveProduct("macbook-air-13-guard", 118L, 1L, "MacBook Air 13 (M2/M3) Hard Protection Guard & Keyboard Cover", 1899.0,
                "Full body protection set for MacBook Air 13 inch including hard shell protective guard case, matching keyboard skin, and screen protector. Scratch-proof & impact resistant.", "Laptop Accessories", 100);

        saveProduct("macbook-pro-16-armor", 119L, 1L, "MacBook Pro 16 Heavy-Duty Armor Guard Case", 2499.0,
                "Rugged shockproof protection guard case with foldable kickstand for MacBook Pro 16-inch. TPU bumper edges defend against drops and bumps.", "Laptop Accessories", 75);

        saveProduct("leather-laptop-sleeve-14", 120L, 1L, "Waterproof Leather Laptop Sleeve Cover (13-14 inch)", 1299.0,
                "Slim waterproof PU leather sleeve cover case with soft velvet lining. Fits MacBook Air 13, MacBook Pro 14, and 13-inch Ultrabooks.", "Laptop Accessories", 150);

        saveProduct("padded-laptop-sleeve-15", 121L, 1L, "Padded Laptop Cover Sleeve Bag with Front Pocket (15.6 inch)", 999.0,
                "360-degree shockproof padded laptop sleeve cover case with accessory storage pocket for 15 to 15.6 inch gaming laptops and notebooks.", "Laptop Accessories", 130);

        saveProduct("rgb-laptop-cooling-pad", 122L, 1L, "RGB Gaming Laptop Cooling Pad (6 Silent Fans)", 2199.0,
                "Ergonomic gaming laptop cooling stand with 6 high-speed quiet fans, RGB lighting modes, dual USB ports, and 7 adjustable height angles. Supports up to 17-inch laptops.", "Laptop Accessories", 85);

        saveProduct("macbook-privacy-screen", 123L, 1L, "Magnetic Privacy Screen Guard for MacBook Pro/Air 14 Inch", 1799.0,
                "Removable magnetic privacy screen filter guard. Anti-spy, anti-glare, and anti-blue light protection for 14-inch MacBooks.", "Laptop Accessories", 90);

        saveProduct("silicone-keyboard-guard", 124L, 1L, "Ultra-Thin Silicone Keyboard Guard Cover for MacBook", 499.0,
                "Dustproof, waterproof, washable ultra-thin silicone keyboard guard cover for MacBook Air and Pro. Prevents key wear and spill damage.", "Laptop Accessories", 250);

        saveProduct("usb-c-hub-7in1", 108L, 2L, "7-in-1 USB-C Hub Adapter", 2499.0,
                "USB-C multiport adapter with HDMI 4K@60Hz, 2× USB-A 3.0, USB-C PD 100W pass-through charging, SD/TF card reader, and Gigabit Ethernet. Compatible with MacBook, Dell XPS, and all USB-C laptops.", "Laptop Accessories", 80);

        saveProduct("ergonomic-laptop-stand", 109L, 2L, "Aluminium Ergonomic Laptop Stand", 3999.0,
                "Premium aluminium laptop stand with adjustable height and angle. Raises screen to eye level for better posture. Anti-slip silicone pads, supports laptops up to 17 inches. Foldable and portable.", "Laptop Accessories", 55);

        saveProduct("wireless-mouse-logitech", 110L, 2L, "Logitech MX Master 3S Wireless Mouse", 8999.0,
                "Advanced wireless mouse with MagSpeed electromagnetic scroll, 8K DPI sensor, USB-C quick charging, quiet clicks, and multi-device support via Bluetooth or USB receiver. Works on any surface including glass.", "Laptop Accessories", 70);

        saveProduct("laptop-backpack", 113L, 1L, "Premium Laptop Backpack — Water Resistant (15.6 inch)", 2799.0,
                "Durable water-resistant laptop backpack with padded compartment for up to 15.6-inch laptops. Features USB charging port, anti-theft hidden zipper, organizer pockets, and breathable back panel. Ideal for commuters and travelers.", "Laptop Accessories", 100);

        saveProduct("anker-power-bank-140w", 125L, 2L, "Anker 737 Laptop Power Bank 24,000mAh (140W Fast Charge)", 11999.0,
                "High-capacity 24,000mAh laptop power bank with 140W output capability. Ultra-fast charges MacBooks, laptops, and smartphones. Smart digital display show power output & battery health.", "Laptop Accessories", 40);

        saveProduct("gan-100w-laptop-charger", 126L, 2L, "100W GaN 4-Port Fast USB-C Charger for Laptops", 3499.0,
                "Compact 100W GaN fast charger with 3 USB-C ports and 1 USB-A port. Fast charges MacBook Pro, laptops, tablets, and phones simultaneously.", "Laptop Accessories", 65);

        saveProduct("blue-light-screen-guard", 127L, 2L, "Anti-Blue Light Screen Guard Protector for 15.6-inch Laptops", 799.0,
                "Matte anti-glare anti-blue light screen guard filter for 15.6 inch laptops. Reduces eye strain and protects display panel from scratches.", "Laptop Accessories", 110);

        saveProduct("dual-laptop-stand", 128L, 2L, "Vertical Aluminium Dual Laptop Stand Holder", 1599.0,
                "Space-saving desktop vertical stand holder for dual laptops or MacBook and iPad. Adjustable dock width, solid aluminium construction.", "Laptop Accessories", 60);

        saveProduct("tech-organizer-pouch", 129L, 1L, "Cable & Tech Organizer Bag for Laptop Accessories", 899.0,
                "Travel electronics organizer pouch bag for laptop chargers, cables, mouse, power banks, and flash drives. Water-resistant fabric with elastic loops.", "Laptop Accessories", 140);

        saveProduct("macbook-extended-warranty", 106L, 1L, "AppleCare+ Extended Warranty for MacBook (2 Years)", 14900.0,
                "Extend your MacBook warranty by 2 additional years. Covers hardware repairs, accidental damage (up to 2 incidents), and battery replacement if capacity falls below 80%.", "Warranty & Protection", 999);

        // Smartphones & Accessories
        saveProduct("iphone-15", 104L, 2L, "Apple iPhone 15", 79000.0,
                "Apple iPhone 15 smartphone with 128GB storage, A16 Bionic chip, and 48MP Advanced Camera system.", "Smartphone", 40);

        saveProduct("samsung-s24-ultra", 131L, 2L, "Samsung Galaxy S24 Ultra 5G", 129999.0,
                "Flagship smartphone with Snapdragon 8 Gen 3, 12GB RAM, 256GB storage, 200MP camera with Galaxy AI features, and integrated S Pen.", "Smartphone", 35);

        saveProduct("iphone-screen-protector", 111L, 2L, "iPhone 15 Tempered Glass Screen Protector (2-Pack)", 499.0,
                "9H hardness tempered glass screen protector for iPhone 15. Edge-to-edge coverage, anti-fingerprint oleophobic coating, bubble-free installation. Includes alignment frame and cleaning kit.", "Phone Accessories", 200);

        saveProduct("iphone-silicone-case", 112L, 2L, "Apple iPhone 15 Silicone Case — Midnight", 3999.0,
                "Official Apple silicone case for iPhone 15. Soft-touch exterior with microfiber lining for added protection. Supports MagSafe wireless charging. Available in Midnight color.", "Phone Accessories", 90);

        // Audio & Monitors
        saveProduct("sony-xm5", 105L, 2L, "Sony WH-1000XM5 Noise Cancelling Headphones", 29999.0,
                "Sony over-ear wireless headphones with industry-leading noise cancellation, 30-hour battery life, and Alexa built-in.", "Headphones", 60);

        saveProduct("airpods-pro-2", 133L, 2L, "Apple AirPods Pro (2nd Generation with USB-C)", 24900.0,
                "Wireless noise-cancelling earbuds with Active Noise Cancellation, Transparency mode, Personalized Spatial Audio, and USB-C MagSafe Charging Case.", "Headphones", 80);

        saveProduct("bose-qc-ultra", 134L, 2L, "Bose QuietComfort Ultra Headphones", 35900.0,
                "World-class noise cancellation headphones with CustomTune technology, Immersive Audio, up to 24 hours of battery life, and Bluetooth 5.3.", "Headphones", 45);

        saveProduct("ipad-air-m2", 132L, 1L, "Apple iPad Air 11-inch M2", 59900.0,
                "Apple iPad Air with M2 chip, 11-inch Liquid Retina Display, 128GB Wi-Fi, landscape 12MP front camera, and Apple Pencil Pro support.", "Tablet", 40);

        saveProduct("lg-ultragear-monitor", 135L, 2L, "LG UltraGear 27-inch QHD Gaming Monitor", 27999.0,
                "27-inch QHD (2560x1440) Nano IPS gaming monitor with 165Hz refresh rate, 1ms response time, HDR10, and NVIDIA G-Sync compatibility.", "Monitors", 30);
    }

    private void saveProduct(String id, Long productId, Long merchantId, String name, Double price, String description, String category, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setProductId(productId);
        product.setMerchantId(merchantId);
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        product.setCategory(category);
        product.setStock(stock);
        product.setAttributes(new HashMap<>());
        product.setVariants(new ArrayList<>());
        product.setImages(new ArrayList<>());
        product.setUpdatedAt(LocalDateTime.now());
        productRepo.save(product);
    }
}


