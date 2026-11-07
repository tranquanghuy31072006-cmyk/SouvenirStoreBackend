package com.learning.souvenirstorebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
    private Long itemId;

    private Long productVariantId;

    private Long productId;

    private String productName;

    private String variantSku;

    private String variantSize;

    private String variantColor;

    private String thumbnail;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}
