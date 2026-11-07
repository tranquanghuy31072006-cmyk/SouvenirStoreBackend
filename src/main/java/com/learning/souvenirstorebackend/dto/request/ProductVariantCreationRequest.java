package com.learning.souvenirstorebackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantCreationRequest {
    @NotBlank(message = "SKU must not be empty")
    String sku;

    @NotBlank(message = "Size must not be empty")
    String size;

    @NotBlank(message = "Color must not be empty")
    String color;

    @NotNull(message = "Stock quantity must be not empty.")
    @Min(value = 0, message = "Stock quantity must be >= 0")
    Integer stockQuantity;

    @NotNull(message = "Price must not be empty")
    @DecimalMin("0")
    BigDecimal price;

    BigDecimal salePrice;

    String image;
}
