package com.ecom.Backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id; // Maps to MongoDB _id

    private Long productId; // Exact match to PostgreSQL products.id

    private Long merchantId; // Denormalized for quick filtering

    private String name;
    
    private Double price;
    
    private String description;
    
    private String category;

    private Integer stock;

    private Map<String, Object> attributes;

    private List<Variant> variants;

    private List<ProductImage> images;

    private SeoInfo seo;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variant {
        private String sku;
        private Map<String, Object> attributes;
        
        @Field("extra_price")
        private Double extraPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImage {
        private String url;
        private String alt;
        
        @Field("is_primary")
        private Boolean isPrimary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeoInfo {
        @Field("meta_title")
        private String metaTitle;
        
        @Field("meta_description")
        private String metaDescription;
    }
}
