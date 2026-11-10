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
import com.learning.souvenirstorebackend.support.SecurityTestHelper;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_ROLE = "CUSTOMER";
    private static final long USER_ID = 1L;
    private static final long CART_ID = 100L;
    private static final long VARIANT_ID = 10L;
    private static final long CART_ITEM_ID = 200L;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100000");

    @Mock
    CartRepository cartRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ProductVariantRepository productVariantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CartMapper cartMapper;

    @InjectMocks
    CartService cartService;

    private User user;
    private Cart cart;
    private ProductVariant variant;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(TEST_USERNAME, TEST_ROLE);
        user.setId(USER_ID);

        var product = TestDataFactory.createProduct(1L, "TestProduct");

        variant = TestDataFactory.createProductVariant(VARIANT_ID, product, 50);

        cart = Cart.builder()
                .id(CART_ID)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        cartResponse = CartResponse.builder().id(CART_ID).build();

        SecurityTestHelper.mockCurrentUser(TEST_USERNAME, TEST_ROLE);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.clearSecurityContext();
    }

    private void stubCurrentUserAndCart() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
    }

    private AddCartItemRequest addRequest(int quantity) {
        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductVariantId(VARIANT_ID);
        req.setQuantity(quantity);
        return req;
    }

    private CartItem buildItem(Cart owner, int quantity) {
        return CartItem.builder()
                .id(CART_ITEM_ID)
                .cart(owner)
                .productVariant(variant)
                .quantity(quantity)
                .price(UNIT_PRICE)
                .build();
    }


    @Test
    void getCart_whenCartExists_returnsMappedResponse() {
        stubCurrentUserAndCart();

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        var result = cartService.getCart();

        assertThat(result).isEqualTo(cartResponse);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_whenCartNotExist_createsNewCart() {
        Cart newCart = Cart.builder().id(999L).user(user).cartItems(new ArrayList<>()).build();

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        when(cartMapper.toCartResponse(newCart)).thenReturn(cartResponse);

        var result = cartService.getCart();

        assertThat(result).isEqualTo(cartResponse);

        verify(cartRepository).save(any(Cart.class));

        verify(cartMapper).toCartResponse(newCart);
    }

    @Test
    void getCart_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(cartRepository, never()).findByUserId(any());

        verify(cartRepository, never()).save(any());
    }


    @Test
    void addItemToCart_whenVariantActiveAndStockEnough_addsItemWithBasePrice() {
        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.empty());

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        cartService.addItemToCart(addRequest(2));

        assertThat(cart.getCartItems()).hasSize(1);

        CartItem item = cart.getCartItems().getFirst();

        assertThat(item.getQuantity()).isEqualTo(2);

        assertThat(item.getPrice()).isEqualByComparingTo("100000");

        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_whenVariantHasSalePrice_usesSalePrice() {
        variant.setSalePrice(new BigDecimal("80000"));

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.empty());

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        cartService.addItemToCart(addRequest(2));

        assertThat(cart.getCartItems().getFirst().getPrice()).isEqualByComparingTo("80000");
    }

    @Test
    void addItemToCart_whenVariantNotFound_throwsVariantNotFound() {
        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_whenVariantInactive_throwsVariantInactive() {
        variant.setStatus(ProductVariantStatus.INACTIVE);

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_INACTIVE);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_whenQuantityExceedsStock_throwsInsufficientStock() {
        variant.setStockQuantity(5);

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(10)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_whenStockNull_treatedAsZeroAndRejected() {
        variant.setStockQuantity(null);

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_whenVariantAlreadyInCart_incrementsQuantity() {
        CartItem existing = buildItem(cart, 1);

        cart.getCartItems().add(existing);

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.of(existing));

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        cartService.addItemToCart(addRequest(2));

        assertThat(existing.getQuantity()).isEqualTo(3);

        assertThat(cart.getCartItems()).hasSize(1);

        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_whenSumExceedsStock_throwsInsufficientStock() {
        variant.setStockQuantity(5);

        CartItem existing = buildItem(cart, 3);

        cart.getCartItems().add(existing);

        stubCurrentUserAndCart();

        when(productVariantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));

        when(cartItemRepository.findByCartIdAndProductVariantId(CART_ID, VARIANT_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(5)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        assertThat(existing.getQuantity()).isEqualTo(3);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(addRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(productVariantRepository, never()).findById(any());

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_whenOwnerAndStockEnough_updatesQuantity() {
        CartItem item = buildItem(cart, 1);

        cart.getCartItems().add(item);

        CartItemUpdateRequest req = new CartItemUpdateRequest();

        req.setQuantity(5);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        cartService.updateItemQuantity(CART_ITEM_ID, req);

        assertThat(item.getQuantity()).isEqualTo(5);

        verify(cartRepository).save(cart);
    }

    @Test
    void updateItemQuantity_whenItemNotFound_throwsCartItemNotFound() {
        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

        CartItemUpdateRequest req = new CartItemUpdateRequest();

        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.updateItemQuantity(CART_ITEM_ID, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_whenItemBelongsToAnotherUser_throwsCartItemNotOwned() {
        Cart otherCart = Cart.builder().id(999L).user(user).cartItems(new ArrayList<>()).build();

        CartItem item = buildItem(otherCart, 1);

        CartItemUpdateRequest req = new CartItemUpdateRequest();

        req.setQuantity(2);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItemQuantity(CART_ITEM_ID, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_OWNED);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_whenNewQuantityExceedsStock_throwsInsufficientStock() {
        variant.setStockQuantity(5);

        CartItem item = buildItem(cart, 1);

        cart.getCartItems().add(item);

        CartItemUpdateRequest req = new CartItemUpdateRequest();

        req.setQuantity(10);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItemQuantity(CART_ITEM_ID, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        assertThat(item.getQuantity()).isEqualTo(1);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_whenStockNull_rejected() {
        variant.setStockQuantity(null);

        CartItem item = buildItem(cart, 1);

        cart.getCartItems().add(item);

        CartItemUpdateRequest req = new CartItemUpdateRequest();

        req.setQuantity(1);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItemQuantity(CART_ITEM_ID, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItem_whenOwner_removesAndSaves() {
        CartItem item = buildItem(cart, 1);

        cart.getCartItems().add(item);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

        cartService.removeItem(CART_ITEM_ID);

        assertThat(cart.getCartItems()).isEmpty();

        verify(cartRepository).save(cart);
    }

    @Test
    void removeItem_whenItemNotFound_throwsCartItemNotFound() {
        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(CART_ITEM_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItem_whenItemBelongsToAnotherUser_throwsCartItemNotOwned() {
        Cart otherCart = Cart.builder().id(999L).user(user).cartItems(new ArrayList<>()).build();

        CartItem item = buildItem(otherCart, 1);

        stubCurrentUserAndCart();

        when(cartItemRepository.findById(CART_ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItem(CART_ITEM_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_OWNED);

        verify(cartRepository, never()).save(any());
    }
}