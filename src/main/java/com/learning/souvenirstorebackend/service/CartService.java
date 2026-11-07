package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.AddCartItemRequest;
import com.learning.souvenirstorebackend.dto.request.CartItemUpdateRequest;
import com.learning.souvenirstorebackend.dto.response.CartResponse;
import com.learning.souvenirstorebackend.entity.Cart;
import com.learning.souvenirstorebackend.entity.CartItem;
import com.learning.souvenirstorebackend.entity.ProductVariant;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.CartMapper;
import com.learning.souvenirstorebackend.repository.CartItemRepository;
import com.learning.souvenirstorebackend.repository.CartRepository;
import com.learning.souvenirstorebackend.repository.ProductVariantRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    UserRepository userRepository;
    ProductVariantRepository productVariantRepository;
    CartMapper cartMapper;

    @Transactional
    public CartResponse getCart() {

        Cart cart = getCurrentCart();

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(AddCartItemRequest request) {

        Cart cart = getCurrentCart();

        ProductVariant productVariant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (productVariant.getStatus() != ProductVariantStatus.ACTIVE)
            throw new AppException(ErrorCode.PRODUCT_VARIANT_INACTIVE);

        BigDecimal effectivePrice = productVariant.getSalePrice() != null
                ? productVariant.getSalePrice()
                : productVariant.getPrice();

        int stock = productVariant.getStockQuantity() == null
                ? 0
                : productVariant.getStockQuantity();

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), request.getProductVariantId());

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();

            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if (newQuantity > stock)
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);

            cartItem.setQuantity(newQuantity);
            cartItem.setPrice(effectivePrice);

        } else {
            if (request.getQuantity() > stock)
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);

            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(productVariant)
                    .quantity(request.getQuantity())
                    .price(effectivePrice)
                    .build();

            cart.getCartItems().add(cartItem);
        }

        cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long itemId, CartItemUpdateRequest request) {

        Cart cart = getCurrentCart();

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_OWNED);
        }

        ProductVariant productVariant = cartItem.getProductVariant();

        int stock = productVariant.getStockQuantity() == null ? 0 : productVariant.getStockQuantity();

        if (request.getQuantity() > stock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        cartItem.setQuantity(request.getQuantity());

        cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {

        Cart cart = getCurrentCart();

        CartItem cartItem = cartItemRepository.findById(itemId).orElseThrow(()
                -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_OWNED);
        }

        cart.getCartItems().remove(cartItem);

        cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    private User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    private Cart getCurrentCart() {

        User user = getCurrentUser();

        return cartRepository.findByUserId(user.getId()).orElseGet(()
                -> cartRepository.save(Cart.builder().user(user).build()));
    }
}