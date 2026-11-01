package com.learning.souvenirstoreproject.controller;

import com.learning.souvenirstoreproject.dto.request.ReviewCreationRequest;
import com.learning.souvenirstoreproject.dto.request.ReviewUpdateRequest;
import com.learning.souvenirstoreproject.dto.response.ApiResponse;
import com.learning.souvenirstoreproject.dto.response.ReviewResponse;
import com.learning.souvenirstoreproject.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {
    ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(@PathVariable Long productId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByProduct(productId))
                .build();
    }

    @PostMapping("/products/{productId}/reviews")
    public ApiResponse<ReviewResponse> createReview(@PathVariable Long productId,
                                                    @RequestBody @Valid ReviewCreationRequest request){
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(productId, request))
                .build();
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable Long reviewId,
                                                    @RequestBody @Valid ReviewUpdateRequest request){
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.updateReview(reviewId, request))
                .build();
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<String> deleteReview(@PathVariable Long reviewId){
        reviewService.deleteReview(reviewId);

        return ApiResponse.<String>builder()
                .message("The review has been successfully deleted.")
                .build();
    }
}
