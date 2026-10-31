package com.learning.souvenirstoreproject.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {

    USER_NOT_FOUND(1001, HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(1002, HttpStatus.CONFLICT, "User already exists"),
    USERNAME_ALREADY_EXISTS(1003, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_ALREADY_EXISTS(1004, HttpStatus.CONFLICT, "Email already exists"),
    PHONE_ALREADY_EXISTS(1005, HttpStatus.CONFLICT, "Phone number already exists"),
    PASSWORD_INCORRECT(1006, HttpStatus.BAD_REQUEST, "Password is incorrect"),
    INVALID_CREDENTIALS(1007, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    USER_BLOCKED(1008, HttpStatus.FORBIDDEN, "Your account has been blocked"),
    USER_ALREADY_ACTIVE(1009, HttpStatus.CONFLICT, "User is already active"),
    USER_ALREADY_BLOCKED(1010, HttpStatus.CONFLICT, "User is already blocked"),
    ROLE_NOT_FOUND(1011, HttpStatus.NOT_FOUND, "Role not found"),


    CATEGORY_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "Category not found"),
    CATEGORY_ALREADY_EXISTS(2002, HttpStatus.CONFLICT, "Category already exists"),
    CATEGORY_HAS_PRODUCTS(2003, HttpStatus.CONFLICT, "Category has products, cannot delete"),
    CATEGORY_ALREADY_ACTIVE(2004, HttpStatus.CONFLICT, "Category is already active"),
    CATEGORY_ALREADY_INACTIVE(2005, HttpStatus.CONFLICT, "Category is already inactive"),
    CATEGORY_PARENT_NOT_FOUND(2006, HttpStatus.BAD_REQUEST, "Parent category not found"),
    CATEGORY_CIRCULAR_REFERENCE(2007, HttpStatus.BAD_REQUEST, "Category cannot be its own ancestor"),


    BRAND_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "Brand not found"),
    BRAND_ALREADY_EXISTS(3002, HttpStatus.CONFLICT, "Brand already exists"),
    BRAND_ALREADY_ACTIVE(3003, HttpStatus.CONFLICT, "Brand is already active"),
    BRAND_ALREADY_INACTIVE(3004, HttpStatus.CONFLICT, "Brand is already inactive"),


    PRODUCT_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "Product not found"),
    PRODUCT_ALREADY_EXISTS(4002, HttpStatus.CONFLICT, "Product already exists"),
    PRODUCT_INACTIVE(4003, HttpStatus.BAD_REQUEST, "Product is inactive"),
    PRODUCT_ALREADY_ACTIVE(4004, HttpStatus.CONFLICT, "Product is already active"),
    PRODUCT_ALREADY_INACTIVE(4005, HttpStatus.CONFLICT, "Product is already inactive"),
    INVALID_SALE_PRICE(4006, HttpStatus.BAD_REQUEST, "Sale price must not exceed regular price"),
    PRODUCT_VARIANT_NOT_FOUND(4007, HttpStatus.NOT_FOUND, "Product variant not found"),
    PRODUCT_VARIANT_SKU_ALREADY_EXISTS(4008, HttpStatus.CONFLICT, "Product variant SKU already exists"),
    PRODUCT_VARIANT_INACTIVE(4009, HttpStatus.BAD_REQUEST, "Product variant is inactive"),
    PRODUCT_VARIANT_IN_USE(4010, HttpStatus.BAD_REQUEST, "Product variant cannot be deleted"),
    PRODUCT_VARIANT_ALREADY_ACTIVE(4011, HttpStatus.CONFLICT, "Product variant is already active"),
    PRODUCT_VARIANT_ALREADY_INACTIVE(4012, HttpStatus.CONFLICT, "Product variant is already inactive"),


    CART_ITEM_NOT_FOUND(5001, HttpStatus.NOT_FOUND, "Cart item not found"),
    CART_EMPTY(5002, HttpStatus.BAD_REQUEST, "Cart is empty"),
    OUT_OF_STOCK(5003, HttpStatus.BAD_REQUEST, "Product is out of stock"),
    INSUFFICIENT_STOCK(5004, HttpStatus.BAD_REQUEST, "Insufficient stock"),
    CART_NOT_FOUND(5005, HttpStatus.NOT_FOUND, "Cart not found"),
    CART_ITEM_NOT_OWNED(5006, HttpStatus.FORBIDDEN, "Cart item does not belong to you"),

    ORDER_NOT_FOUND(6001, HttpStatus.NOT_FOUND, "Order not found"),
    INVALID_ORDER_STATUS(6002, HttpStatus.BAD_REQUEST, "Invalid order status transition"),
    ORDER_CANNOT_BE_CANCELLED(6003, HttpStatus.CONFLICT, "Order cannot be cancelled"),
    ORDER_ALREADY_CANCELLED(6004, HttpStatus.CONFLICT, "Order is already cancelled"),
    ORDER_NOT_OWNED(6005, HttpStatus.FORBIDDEN, "Order does not belong to you"),
    ORDER_ALREADY_IN_STATUS(6006, HttpStatus.CONFLICT, "Order is already in this status"),


    PAYMENT_NOT_FOUND(7001, HttpStatus.NOT_FOUND, "Payment not found"),
    PAYMENT_ALREADY_PAID(7002, HttpStatus.CONFLICT, "Payment has already been completed"),
    PAYMENT_FAILED(7003, HttpStatus.BAD_REQUEST, "Payment failed"),


    REVIEW_NOT_FOUND(8001, HttpStatus.NOT_FOUND, "Review not found"),
    REVIEW_NOT_PURCHASED(8002, HttpStatus.FORBIDDEN, "You must purchase this product before reviewing"),
    REVIEW_ALREADY_EXISTS(8003, HttpStatus.CONFLICT, "You have already reviewed this product"),


    UNAUTHENTICATED(9001, HttpStatus.UNAUTHORIZED, "Unauthenticated"),
    ACCESS_DENIED(9002, HttpStatus.FORBIDDEN, "Access denied"),
    VALIDATION_FAILED(9003, HttpStatus.BAD_REQUEST, "Validation failed"),
    UNCATEGORIZED(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized exception"),
    ;

    int code;
    HttpStatus httpStatus;
    String message;
}