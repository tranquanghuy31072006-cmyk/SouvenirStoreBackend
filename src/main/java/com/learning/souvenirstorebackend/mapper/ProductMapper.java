package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.request.ProductRequest;
import com.learning.souvenirstorebackend.dto.response.ProductResponse;
import com.learning.souvenirstorebackend.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "productImages", target = "imageUrls")
    @Mapping(target = "stockQuantity", expression = "java(calculateStockQuantity(product))")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productVariants", ignore = true)
    @Mapping(target = "productImages", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toProduct(ProductRequest productRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productVariants", ignore = true)
    @Mapping(target = "productImages", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductRequest productRequest);

    List<ProductResponse> toProductResponseList(List<Product> productList);

    default Integer calculateStockQuantity(Product product) {
        if (product.getProductVariants() == null || product.getProductVariants().isEmpty()) {
            return 0;
        }

        return product.getProductVariants().stream()
                .mapToInt(v -> v.getStockQuantity() == null ? 0 : v.getStockQuantity())
                .sum();
    }
}