
package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.ProductRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.ProductResponse;
import com.learning.souvenirstorebackend.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(productRequest))
                .build();
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductById(productId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllProducts() {

        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getAllProducts())
                .build();
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long productId,
                                                      @Valid @RequestBody ProductRequest productRequest) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(productId, productRequest))
                .build();
    }

    @PutMapping("/{productId}/activate")
    public ApiResponse<ProductResponse> activateProductStatus(@PathVariable Long productId) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.activateProductStatus(productId))
                .build();
    }

    @PutMapping("/{productId}/deactivate")
    public ApiResponse<ProductResponse> deactivateProductStatus(@PathVariable Long productId) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.deactivateProductStatus(productId))
                .build();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<String> deleteProduct(@PathVariable Long productId) {

        productService.deleteProductById(productId);

        return ApiResponse.<String>builder()
                .message("Product deleted successfully")
                .build();
    }
}