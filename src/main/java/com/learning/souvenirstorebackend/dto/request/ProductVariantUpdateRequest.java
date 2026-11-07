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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantUpdateRequest {

    @NotBlank(message = "Size must not be empty")
    String size;

    @NotBlank(message = "Color must not be empty")
    String color;

    @NotNull(message = "The quantity must be greater than 0 and cannot be empty.")
    @Min(0)
    Integer stockQuantity;

    @NotNull(message = "Price must not be empty")
    @DecimalMin("0")
    BigDecimal price;

    BigDecimal salePrice;

    String image;
}
