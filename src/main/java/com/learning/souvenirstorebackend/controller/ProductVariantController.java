package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.ProductVariantCreationRequest;
import com.learning.souvenirstorebackend.dto.request.ProductVariantUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.ProductVariantResponse;
import com.learning.souvenirstorebackend.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductVariantController {

    ProductVariantService productVariantService;

    @PostMapping("/products/{productId}/variants")
    ApiResponse<ProductVariantResponse> createProductVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantCreationRequest request) {

        return ApiResponse.<ProductVariantResponse>builder()
                .result(productVariantService.createProductVariant(productId, request))
                .build();
    }

    @GetMapping("/products/{productId}/variants")
    ApiResponse<List<ProductVariantResponse>> getProductVariantsByProductId(@PathVariable Long productId) {

        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(productVariantService.getProductVariantsByProductId(productId))
                .build();
    }

    @GetMapping("/products/{productId}/variants/active")
    ApiResponse<List<ProductVariantResponse>> getActiveProductVariantsByProductId(@PathVariable Long productId) {

        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(productVariantService.getActiveVariantsByProductId(productId))
                .build();
    }

    @GetMapping("/variants/{variantId}")
    ApiResponse<ProductVariantResponse> getProductVariant(@PathVariable Long variantId) {

        return ApiResponse.<ProductVariantResponse>builder()
                .result(productVariantService.getProductVariantById(variantId))
                .build();
    }

    @PutMapping("/variants/{variantId}")
    ApiResponse<ProductVariantResponse> updateVariant(@PathVariable Long variantId,
                                                      @RequestBody @Valid ProductVariantUpdateRequest request) {

        return ApiResponse.<ProductVariantResponse>builder()
                .result(productVariantService.updateProductVariant(variantId, request))
                .build();
    }

    @PatchMapping("/variants/{variantId}/activate")
    ApiResponse<String> activateVariant(@PathVariable Long variantId) {

        productVariantService.activateProductVariant(variantId);

        return ApiResponse.<String>builder()
                .message("Variant activated")
                .build();
    }

    @PatchMapping("/variants/{variantId}/deactivate")
    ApiResponse<String> deactivateVariant(@PathVariable Long variantId) {
        productVariantService.deactivateProductVariant(variantId);

        return ApiResponse.<String>builder()
                .message("Variant deactivated")
                .build();
    }

    @DeleteMapping("/variants/{variantId}")
    ApiResponse<String> deleteVariant(@PathVariable Long variantId) {
        productVariantService.deleteProductVariant(variantId);

        return ApiResponse.<String>builder()
                .message("Variant deleted")
                .build();
    }
}
