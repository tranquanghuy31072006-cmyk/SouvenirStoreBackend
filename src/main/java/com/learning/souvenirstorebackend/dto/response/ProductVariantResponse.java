package com.learning.souvenirstorebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse {
    Long id;

    String sku;

    String size;

    String color;

    Integer stockQuantity;

    BigDecimal price;

    BigDecimal salePrice;

    BigDecimal effectivePrice;

    String image;

    ProductVariantStatus status;

    Long productId;

    String productName;

    LocalDateTime createdAt;
}
