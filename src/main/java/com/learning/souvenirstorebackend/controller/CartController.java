
package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.AddCartItemRequest;
import com.learning.souvenirstorebackend.dto.request.CartItemUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.CartResponse;
import com.learning.souvenirstorebackend.service.CartService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartController {

    CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart() {

        return ApiResponse.<CartResponse>builder()
                .result(cartService.getCart())
                .build();
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItemToCart(@Valid @RequestBody AddCartItemRequest request) {

        return ApiResponse.<CartResponse>builder()
                .result(cartService.addItemToCart(request))
                .build();
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItemInCart(@PathVariable Long itemId,
                                                      @Valid @RequestBody CartItemUpdateRequest request) {

        return ApiResponse.<CartResponse>builder()
                .result(cartService.updateItemQuantity(itemId, request))
                .build();
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> deleteItemInCart(@PathVariable Long itemId) {

        return ApiResponse.<CartResponse>builder()
                .result(cartService.removeItem(itemId))
                .build();
    }
}