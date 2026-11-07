package com.learning.souvenirstorebackend.support;

import com.learning.souvenirstorebackend.entity.Product;
import com.learning.souvenirstorebackend.entity.ProductVariant;
import com.learning.souvenirstorebackend.entity.Role;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.ProductStatus;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import com.learning.souvenirstorebackend.enums.UserStatus;

import java.math.BigDecimal;

public class TestDataFactory {

    public static User createUser(String username, String roleName) {
        Role role = Role.builder()
                .name(roleName)
                .build();

        return  User.builder()
                .username(username)
                .password("password123")
                .email(username + "@gmail.com")
                .phone("0911111111")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static Product createProduct(Long id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal("200000"))
                .status(ProductStatus.ACTIVE)
                .build();
    }

    public static ProductVariant createProductVariant(Long id, Product product, int stock) {
        return ProductVariant.builder()
                .id(id)
                .product(product)
                .stockQuantity(stock)
                .price(new BigDecimal("100000"))
                .sku("SKU-" + id)
                .status(ProductVariantStatus.ACTIVE)
                .build();
    }
}
