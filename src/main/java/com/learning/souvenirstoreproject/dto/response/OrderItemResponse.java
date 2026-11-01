package com.learning.souvenirstoreproject.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {

    Long orderId;

    Long productId;

    Long productVariantId;

    String productName;

    String productImage;

    String size;

    String color;

    Integer quantity;

    BigDecimal price;

    BigDecimal totalPrice;
}