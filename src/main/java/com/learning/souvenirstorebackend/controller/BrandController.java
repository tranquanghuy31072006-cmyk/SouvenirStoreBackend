package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.BrandRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.BrandResponse;
import com.learning.souvenirstorebackend.service.BrandService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandController {

    BrandService brandService;

    @PostMapping
    ApiResponse<BrandResponse> createBrand(@Valid @RequestBody BrandRequest brandRequest) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.createBrand(brandRequest))
                .build();
    }

    // Lấy toàn bộ thương hiệu (ADMIN/STAFF)
    @GetMapping
    ApiResponse<List<BrandResponse>> getAllBrands() {
        return ApiResponse.<List<BrandResponse>>builder()
                .result(brandService.getAllBrands())
                .build();
    }

    @GetMapping("/{brandId}")
    ApiResponse<BrandResponse> getBrandById(@PathVariable Long brandId) {

        return ApiResponse.<BrandResponse>builder()
                .result(brandService.getBrandById(brandId))
                .build();
    }

    @GetMapping("/active")
    ApiResponse<List<BrandResponse>> getActiveBrands() {

        return ApiResponse.<List<BrandResponse>>builder()
                .result(brandService.getActiveBrands())
                .build();
    }

    @PutMapping("/{brandId}")
    ApiResponse<BrandResponse> updateBrand(@PathVariable Long brandId,
                                           @Valid @RequestBody BrandRequest brandRequest) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.updateBrand(brandId, brandRequest))
                .build();
    }

    @PutMapping("/{brandId}/activate")
    ApiResponse<BrandResponse> activateBrand(@PathVariable Long brandId) {

        return ApiResponse.<BrandResponse>builder()
                .result(brandService.activateBrand(brandId))
                .build();
    }

    @PutMapping("/{brandId}/deactivate")
    ApiResponse<BrandResponse> deactivateBrand(@PathVariable Long brandId) {

        return ApiResponse.<BrandResponse>builder()
                .result(brandService.deactivateBrand(brandId))
                .build();
    }

    @DeleteMapping("/{brandId}")
    ApiResponse<String> deleteBrand(@PathVariable Long brandId) {

        brandService.deleteBrand(brandId);

        return ApiResponse.<String>builder()
                .message("Brand with id = " + brandId + " has been deleted")
                .build();
    }
}