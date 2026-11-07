package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.CategoryRequest;
import com.learning.souvenirstorebackend.dto.response.CategoryResponse;
import com.learning.souvenirstorebackend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "status", ignore = true)
    Category toCategory(CategoryRequest categoryRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateCategory(@MappingTarget Category category, CategoryRequest categoryRequest);
}