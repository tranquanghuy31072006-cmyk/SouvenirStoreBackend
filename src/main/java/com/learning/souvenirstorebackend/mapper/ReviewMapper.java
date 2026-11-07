package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.ReviewUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ReviewResponse;
import com.learning.souvenirstorebackend.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullName")
    ReviewResponse toReviewResponse(Review review);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateReview(@MappingTarget Review review, ReviewUpdateRequest request);
}
