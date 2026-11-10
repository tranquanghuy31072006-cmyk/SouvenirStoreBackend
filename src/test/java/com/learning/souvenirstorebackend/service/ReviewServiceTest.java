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
import com.learning.souvenirstorebackend.support.SecurityTestHelper;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_ROLE = "CUSTOMER";
    private static final long USER_ID = 1L;
    private static final long PRODUCT_ID = 1L;
    private static final long REVIEW_ID = 100L;
    private static final int RATING = 5;
    private static final String COMMENT = "Good";

    @Mock
    ReviewRepository reviewRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ReviewMapper reviewMapper;

    @InjectMocks
    ReviewService reviewService;

    private User user;
    private Product product;
    private Review review;
    private ReviewResponse reviewResponse;
    private ReviewCreationRequest createRequest;
    private ReviewUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(TEST_USERNAME, TEST_ROLE);

        user.setId(USER_ID);

        product = TestDataFactory.createProduct(PRODUCT_ID, "TestProduct");

        review = Review.builder()
                .id(REVIEW_ID)
                .user(user)
                .product(product)
                .rating(RATING)
                .comment(COMMENT)
                .build();

        reviewResponse = ReviewResponse.builder().id(REVIEW_ID).rating(RATING).build();

        createRequest = ReviewCreationRequest.builder()
                .rating(RATING)
                .comment(COMMENT)
                .build();

        updateRequest = ReviewUpdateRequest.builder()
                .rating(4)
                .comment("Updated comment")
                .build();

        SecurityTestHelper.mockCurrentUser(TEST_USERNAME, TEST_ROLE);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.clearSecurityContext();
    }

    private void stubCurrentUser() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    }

    @Test
    void getReviewsByProduct_whenProductExists_returnsMappedList() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PRODUCT_ID))
                .thenReturn(List.of(review));

        when(reviewMapper.toReviewResponse(review)).thenReturn(reviewResponse);

        var result = reviewService.getReviewsByProduct(PRODUCT_ID);

        assertThat(result).hasSize(1).containsExactly(reviewResponse);

        verify(reviewRepository).findByProductIdOrderByCreatedAtDesc(PRODUCT_ID);
    }

    @Test
    void getReviewsByProduct_whenProductNotFound_throwsProductNotFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewsByProduct(PRODUCT_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(reviewRepository, never()).findByProductIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void getReviewsByProduct_whenEmpty_returnsEmptyList() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PRODUCT_ID)).thenReturn(List.of());

        var result = reviewService.getReviewsByProduct(PRODUCT_ID);

        assertThat(result).isEmpty();

        verify(reviewMapper, never()).toReviewResponse(any());
    }

    @Test
    void createReview_whenPurchasedAndNotReviewed_savesAndReturns() {
        stubCurrentUser();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        when(orderRepository.hasUserPurchasedProduct(USER_ID, PRODUCT_ID)).thenReturn(true);

        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);

        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(reviewResponse);

        var result = reviewService.createReview(PRODUCT_ID, createRequest);

        assertThat(result).isEqualTo(reviewResponse);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(productRepository, never()).findById(anyLong());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenProductNotFound_throwsProductNotFound() {
        stubCurrentUser();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(orderRepository, never()).hasUserPurchasedProduct(anyLong(), anyLong());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenNotPurchased_throwsReviewNotPurchased() {
        stubCurrentUser();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        when(orderRepository.hasUserPurchasedProduct(USER_ID, PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_PURCHASED);

        verify(reviewRepository, never()).existsByUserIdAndProductId(anyLong(), anyLong());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_whenAlreadyReviewed_throwsReviewAlreadyExists() {
        stubCurrentUser();

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        when(orderRepository.hasUserPurchasedProduct(USER_ID, PRODUCT_ID)).thenReturn(true);

        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(PRODUCT_ID, createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenOwned_callsMapperUpdateAndSaves() {
        stubCurrentUser();

        when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review));

        when(reviewRepository.save(review)).thenReturn(review);

        when(reviewMapper.toReviewResponse(review)).thenReturn(reviewResponse);

        var result = reviewService.updateReview(REVIEW_ID, updateRequest);

        assertThat(result).isEqualTo(reviewResponse);

        verify(reviewMapper).updateReview(review, updateRequest);

        verify(reviewRepository).save(review);
    }

    @Test
    void updateReview_whenNotFound_throwsReviewNotFound() {
        stubCurrentUser();

        when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);

        verify(reviewMapper, never()).updateReview(any(), any());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(REVIEW_ID, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(reviewRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void deleteReview_whenOwned_deletes() {
        stubCurrentUser();

        when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review));

        reviewService.deleteReview(REVIEW_ID);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_whenNotFound_throwsReviewNotFound() {
        stubCurrentUser();

        when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(reviewRepository, never()).findByIdAndUserId(anyLong(), anyLong());

        verify(reviewRepository, never()).delete(any());
    }
}