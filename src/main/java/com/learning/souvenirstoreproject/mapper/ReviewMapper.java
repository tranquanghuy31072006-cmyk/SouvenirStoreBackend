package com.learning.souvenirstoreproject.mapper;

import com.learning.souvenirstoreproject.dto.request.ReviewUpdateRequest;
import com.learning.souvenirstoreproject.dto.response.ReviewResponse;
import com.learning.souvenirstoreproject.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullName")
    ReviewResponse toReviewResponse(Review review);

    void updateReview(@MappingTarget Review review, ReviewUpdateRequest request);
}
