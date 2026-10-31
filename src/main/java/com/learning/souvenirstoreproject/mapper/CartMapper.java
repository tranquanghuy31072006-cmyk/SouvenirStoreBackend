package com.learning.souvenirstoreproject.mapper;

import com.learning.souvenirstoreproject.dto.response.CartItemResponse;
import com.learning.souvenirstoreproject.dto.response.CartResponse;
import com.learning.souvenirstoreproject.entity.Cart;
import com.learning.souvenirstoreproject.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "items", source = "cartItems")
    @Mapping(target = "totalItems", expression = "java(calculateTotalItems(cart))")
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(cart))")
    CartResponse toCartResponse(Cart cart);

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "productId", source = "productVariant.product.id")
    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "variantSku", source = "productVariant.sku")
    @Mapping(target = "variantSize", source = "productVariant.size")
    @Mapping(target = "variantColor", source = "productVariant.color")
    @Mapping(target = "thumbnail", source = "productVariant.product.thumbnail")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    CartItemResponse toCartItemResponse(CartItem item);

    default Integer calculateTotalItems(Cart cart) {
        if (cart.getCartItems() == null)
            return 0;

        return cart.getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    default BigDecimal calculateTotalPrice(Cart cart) {
        if (cart.getCartItems() == null)
            return BigDecimal.ZERO;

        return cart.getCartItems().stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal calculateSubtotal(CartItem item) {
        if (item.getPrice() == null || item.getQuantity() == null)
            return BigDecimal.ZERO;

        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
