package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.ProductVariantCreationRequest;
import com.learning.souvenirstorebackend.dto.request.ProductVariantUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.ProductVariantResponse;
import com.learning.souvenirstorebackend.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "effectivePrice", expression = "java(calcEffectivePrice(productVariant))")
    ProductVariantResponse toProductVariantResponse(ProductVariant productVariant);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    ProductVariant toProductVariant(ProductVariantCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductVariant(
            @MappingTarget ProductVariant productVariant,
            ProductVariantUpdateRequest request
    );

    default BigDecimal calcEffectivePrice(ProductVariant productVariant) {
        return productVariant.getSalePrice() != null
                ? productVariant.getSalePrice()
                : productVariant.getPrice();
    }
}