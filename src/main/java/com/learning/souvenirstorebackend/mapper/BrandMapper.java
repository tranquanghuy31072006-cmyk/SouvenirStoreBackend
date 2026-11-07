package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.BrandRequest;
import com.learning.souvenirstorebackend.dto.response.BrandResponse;
import com.learning.souvenirstorebackend.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Brand toBrand(BrandRequest brandRequest);

    BrandResponse toBrandResponse(Brand brand);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateBrand(@MappingTarget Brand brand, BrandRequest brandRequest);
}
