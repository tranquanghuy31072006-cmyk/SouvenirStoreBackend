package com.learning.souvenirstorebackend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {

    Long id;

    String name;

    String description;

    BigDecimal price;

    Integer stockQuantity;

    String thumbnail;

    List<ProductImageResponse> imageUrls;

    String status;

    Long categoryId;

    String categoryName;

    Long brandId;

    String brandName;
}