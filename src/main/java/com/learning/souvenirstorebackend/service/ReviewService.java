package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.ReviewCreationRequest;
import com.learning.souvenirstorebackend.dto.request.ReviewUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ReviewResponse;
import com.learning.souvenirstorebackend.entity.Product;
import com.learning.souvenirstorebackend.entity.Review;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.ReviewMapper;
import com.learning.souvenirstorebackend.repository.OrderRepository;
import com.learning.souvenirstorebackend.repository.ProductRepository;
import com.learning.souvenirstorebackend.repository.ReviewRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        if (productRepository.findById(productId).isEmpty())
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(reviewMapper::toReviewResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse createReview(Long productId, ReviewCreationRequest request) {
        User user = getCurrentUser();

        Product product = productRepository.findById(productId).orElseThrow(()
                -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!orderRepository.hasUserPurchasedProduct(user.getId(), productId))
            throw new AppException(ErrorCode.REVIEW_NOT_PURCHASED);

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId))
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        User user = getCurrentUser();

        Review review = reviewRepository.findByIdAndUserId(reviewId, user.getId()).orElseThrow(()
                -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        reviewMapper.updateReview(review, request);

        reviewRepository.save(review);

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();

        Review review = reviewRepository.findByIdAndUserId(reviewId, user.getId()).orElseThrow(()
                -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        reviewRepository.delete(review);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        return userRepository.findByUsername(username).orElseThrow(()
                -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}
