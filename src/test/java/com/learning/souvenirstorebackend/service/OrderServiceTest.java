package com.learning.souvenirstorebackend.service;

import com.learning.souvenirstorebackend.dto.request.OrderCreationRequest;
import com.learning.souvenirstorebackend.dto.response.OrderResponse;
import com.learning.souvenirstorebackend.dto.response.PaymentResponse;
import com.learning.souvenirstorebackend.entity.Cart;
import com.learning.souvenirstorebackend.entity.CartItem;
import com.learning.souvenirstorebackend.entity.Order;
import com.learning.souvenirstorebackend.entity.OrderItem;
import com.learning.souvenirstorebackend.entity.Payment;
import com.learning.souvenirstorebackend.entity.Product;
import com.learning.souvenirstorebackend.entity.ProductVariant;
import com.learning.souvenirstorebackend.entity.User;
import com.learning.souvenirstorebackend.enums.OrderStatus;
import com.learning.souvenirstorebackend.enums.PaymentMethod;
import com.learning.souvenirstorebackend.enums.PaymentStatus;
import com.learning.souvenirstorebackend.enums.ProductVariantStatus;
import com.learning.souvenirstorebackend.exception.AppException;
import com.learning.souvenirstorebackend.exception.ErrorCode;
import com.learning.souvenirstorebackend.mapper.OrderMapper;
import com.learning.souvenirstorebackend.mapper.PaymentMapper;
import com.learning.souvenirstorebackend.repository.CartRepository;
import com.learning.souvenirstorebackend.repository.OrderRepository;
import com.learning.souvenirstorebackend.repository.PaymentRepository;
import com.learning.souvenirstorebackend.repository.ProductVariantRepository;
import com.learning.souvenirstorebackend.repository.UserRepository;
import com.learning.souvenirstorebackend.support.SecurityTestHelper;
import com.learning.souvenirstorebackend.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    private static final String TEST_USERNAME = "testUser";
    private static final long USER_ID = 1L;
    private static final long CART_ID = 100L;
    private static final long ORDER_ID = 500L;
    private static final long VARIANT1_ID = 10L;
    private static final long VARIANT2_ID = 20L;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100000");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");

    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    ProductVariantRepository productVariantRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OrderMapper orderMapper;
    @Mock
    PaymentMapper paymentMapper;

    @InjectMocks
    OrderService orderService;

    @Captor
    ArgumentCaptor<Order> orderCaptor;
    @Captor
    ArgumentCaptor<Payment> paymentCaptor;

    private User user;
    private Product product;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private Cart cart;
    private OrderResponse orderResponse;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(TEST_USERNAME, "CUSTOMER");
        user.setId(USER_ID);

        product = TestDataFactory.createProduct(1L, "TestProduct");
        variant1 = TestDataFactory.createProductVariant(VARIANT1_ID, product, 50);
        variant2 = TestDataFactory.createProductVariant(VARIANT2_ID, product, 30);

        cart = Cart.builder().id(CART_ID).user(user).cartItems(new ArrayList<>()).build();

        orderResponse = new OrderResponse();
        paymentResponse = new PaymentResponse();

        SecurityTestHelper.mockCurrentUser(TEST_USERNAME, "CUSTOMER");
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.clearSecurityContext();
    }

    private static PaymentMethod nonCodMethod() {
        return Arrays.stream(PaymentMethod.values())
                .filter(m -> m != PaymentMethod.COD)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Enum PaymentMethod has only COD; add another"));
    }

    private OrderCreationRequest buyNowRequest(int qty) {
        OrderCreationRequest r = new OrderCreationRequest();
        r.setBuyNowProductVariantId(VARIANT1_ID);
        r.setBuyNowQuantity(qty);
        r.setReceiverName("Receiver");
        r.setReceiverPhone("0901234567");
        r.setReceiverAddress("Address");
        r.setPaymentMethod(PaymentMethod.COD);
        return r;
    }

    private OrderCreationRequest cartRequest(List<Long> cartItemIds) {
        OrderCreationRequest r = new OrderCreationRequest();
        r.setCartItemIds(cartItemIds);
        r.setReceiverName("Receiver");
        r.setReceiverPhone("0901234567");
        r.setReceiverAddress("Address");
        r.setPaymentMethod(PaymentMethod.COD);
        return r;
    }

    private CartItem cartItem(long id, ProductVariant v, int qty, BigDecimal price) {
        return CartItem.builder()
                .id(id).productVariant(v).quantity(qty).price(price).cart(cart).build();
    }

    private Order orderWithStatus(OrderStatus orderStatus) {
        return Order.builder()
                .id(ORDER_ID)
                .user(user)
                .orderItems(new ArrayList<>())
                .orderStatus(orderStatus)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }

    private OrderItem orderItem(ProductVariant v, int qty) {
        return OrderItem.builder()
                .productVariant(v)
                .quantity(qty)
                .price(v.getPrice())
                .totalPrice(v.getPrice().multiply(BigDecimal.valueOf(qty)))
                .build();
    }

    private Payment paymentFor(Order order, PaymentStatus status, PaymentMethod method) {
        return Payment.builder()
                .id(1L)
                .order(order)
                .status(status)
                .paymentMethod(method)
                .amount(BigDecimal.ZERO)
                .build();
    }

    private void stubCreateOrderHappyPath() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(orderResponse);
        when(paymentMapper.toPaymentResponse(any(Payment.class))).thenReturn(paymentResponse);
    }

    @Test
    void createOrder_buyNow_whenValid_createsOrderItemAndPendingPayment() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));
        stubCreateOrderHappyPath();

        orderService.createOrder(buyNowRequest(2));

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();

        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getShippingFee()).isEqualByComparingTo("30000");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("200000");
        assertThat(saved.getFinalAmount()).isEqualByComparingTo("230000");

        assertThat(saved.getOrderItems()).hasSize(1);
        OrderItem oi = saved.getOrderItems().getFirst();
        assertThat(oi.getProductName()).isEqualTo(product.getName());
        assertThat(oi.getQuantity()).isEqualTo(2);
        assertThat(oi.getPrice()).isEqualByComparingTo("100000");
        assertThat(oi.getTotalPrice()).isEqualByComparingTo("200000");

        assertThat(variant1.getStockQuantity()).isEqualTo(48);

        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getAmount()).isEqualByComparingTo("230000");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getOrder()).isSameAs(saved);

        verify(cartRepository, never()).save(any(Cart.class));
        assertThat(cart.getCartItems()).isEmpty();
    }

    @Test
    void createOrder_buyNow_whenSalePriceNotNull_usesSalePrice() {
        variant1.setSalePrice(new BigDecimal("80000"));
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));
        stubCreateOrderHappyPath();

        orderService.createOrder(buyNowRequest(2));

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("160000");
        assertThat(saved.getFinalAmount()).isEqualByComparingTo("190000");
    }

    @Test
    void createOrder_buyNow_whenStockExactlyEnough_endsWithZeroStock() {
        variant1.setStockQuantity(2);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));
        stubCreateOrderHappyPath();

        orderService.createOrder(buyNowRequest(2));

        assertThat(variant1.getStockQuantity()).isZero();
    }

    @Test
    void createOrder_buyNow_whenVariantNotFound_throwsVariantNotFound() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buyNowRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_NOT_FOUND);

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createOrder_buyNow_whenVariantInactive_throwsVariantInactive() {
        variant1.setStatus(ProductVariantStatus.INACTIVE);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));

        assertThatThrownBy(() -> orderService.createOrder(buyNowRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_INACTIVE);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_buyNow_whenStockNull_throwsInsufficientStock() {
        variant1.setStockQuantity(null);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));

        assertThatThrownBy(() -> orderService.createOrder(buyNowRequest(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_buyNow_whenQuantityExceedsStock_throwsInsufficientStock() {
        variant1.setStockQuantity(5);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));

        assertThatThrownBy(() -> orderService.createOrder(buyNowRequest(10)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        assertThat(variant1.getStockQuantity()).isEqualTo(5);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_buyNow_doesNotRemoveItemsFromExistingCart() {
        cart.getCartItems().add(cartItem(999L, variant2, 1, variant2.getPrice()));
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(VARIANT1_ID)).thenReturn(Optional.of(variant1));
        stubCreateOrderHappyPath();

        orderService.createOrder(buyNowRequest(2));

        assertThat(cart.getCartItems()).hasSize(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }


    @Test
    void createOrder_fromCart_whenNoSelectedIds_checksOutAllItems() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));
        cart.getCartItems().add(cartItem(202L, variant2, 1, UNIT_PRICE));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        stubCreateOrderHappyPath();

        orderService.createOrder(cartRequest(null));

        assertThat(cart.getCartItems()).isEmpty();
        assertThat(variant1.getStockQuantity()).isEqualTo(48);
        assertThat(variant2.getStockQuantity()).isEqualTo(29);
        verify(cartRepository).save(cart);
    }

    @Test
    void createOrder_fromCart_whenSelectedIds_onlyChecksOutSelectedItems() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));
        cart.getCartItems().add(cartItem(202L, variant2, 1, UNIT_PRICE));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        stubCreateOrderHappyPath();

        orderService.createOrder(cartRequest(List.of(201L)));

        assertThat(cart.getCartItems()).hasSize(1);
        assertThat(cart.getCartItems().getFirst().getId()).isEqualTo(202L);
        assertThat(variant1.getStockQuantity()).isEqualTo(48);
        assertThat(variant2.getStockQuantity()).isEqualTo(30);
    }

    @Test
    void createOrder_fromCart_whenEmpty_throwsCartEmpty() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(cartRequest(null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_fromCart_whenCartNotExist_throwsCartEmpty() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(cartRequest(null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_fromCart_whenSelectedIdsMatchNone_throwsCartEmpty() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(cartRequest(List.of(999L))))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_EMPTY);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_fromCart_whenOneVariantInactive_throwsVariantInactive() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));
        cart.getCartItems().add(cartItem(202L, variant2, 1, UNIT_PRICE));
        variant2.setStatus(ProductVariantStatus.INACTIVE);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(cartRequest(null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_VARIANT_INACTIVE);

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createOrder_fromCart_whenOneVariantStockInsufficient_throwsInsufficientStock() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));
        cart.getCartItems().add(cartItem(202L, variant2, 5, UNIT_PRICE));
        variant2.setStockQuantity(1);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(cartRequest(null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_fromCart_withMultipleVariants_computesTotalCorrectly() {
        cart.getCartItems().add(cartItem(201L, variant1, 2, UNIT_PRICE));
        cart.getCartItems().add(cartItem(202L, variant2, 1, UNIT_PRICE));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        stubCreateOrderHappyPath();

        orderService.createOrder(cartRequest(null));

        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("300000");
        assertThat(saved.getFinalAmount()).isEqualByComparingTo("330000");
        assertThat(saved.getOrderItems()).hasSize(2);
    }

    @Test
    void getOrderByOrderId_whenOwned_returnsResponseWithPayment() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        Payment payment = paymentFor(order, PaymentStatus.PENDING, PaymentMethod.COD);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(paymentResponse);

        var result = orderService.getOrderByOrderId(ORDER_ID);

        assertThat(result).isSameAs(orderResponse);
        verify(paymentMapper).toPaymentResponse(payment);
    }

    @Test
    void getOrderByOrderId_whenNotFound_throwsOrderNotFound() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByOrderId(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);

        verify(paymentRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void getOrderByOrderId_whenNoPayment_returnsResponseWithoutPayment() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        var result = orderService.getOrderByOrderId(ORDER_ID);

        assertThat(result).isSameAs(orderResponse);
        verify(paymentMapper, never()).toPaymentResponse(any());
    }

    @Test
    void getOrderByOrderId_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByOrderId(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);

        verify(orderRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void getOrdersByUserId_returnsMappedListOfCurrentUser() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(order));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        var result = orderService.getOrdersByUserId();

        assertThat(result).hasSize(1).containsExactly(orderResponse);
        verify(orderRepository).findByUserId(USER_ID);
    }

    @Test
    void getOrdersByUserId_whenEmpty_returnsEmptyList() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of());

        var result = orderService.getOrdersByUserId();

        assertThat(result).isEmpty();
    }

    @Test
    void getOrdersByUserId_whenUserNotExists_throwsUnauthenticated() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrdersByUserId())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void cancelOrder_whenPending_setsCancelledAndRestoresStock() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.getOrderItems().add(orderItem(variant1, 2));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(variant1.getStockQuantity()).isEqualTo(52);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_whenStockNull_treatsAsZeroAndAddsQuantity() {
        variant1.setStockQuantity(null);
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.getOrderItems().add(orderItem(variant1, 2));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(variant1.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void cancelOrder_whenPaymentPaid_setsPaymentRefunded() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PAID);
        Payment payment = paymentFor(order, PaymentStatus.PAID, PaymentMethod.COD);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(paymentResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentMapper).toPaymentResponse(payment);
    }

    @Test
    void cancelOrder_whenPaymentPending_setsPaymentFailed() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        Payment payment = paymentFor(order, PaymentStatus.PENDING, PaymentMethod.COD);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(paymentResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cancelOrder_whenNoPayment_stillCancelsOrder() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentMapper, never()).toPaymentResponse(any());
    }

    @Test
    void cancelOrder_whenNotFound_throwsOrderNotFound() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void cancelOrder_whenConfirmed_throwsOrderCannotBeCancelled() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);

        verify(paymentRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void cancelOrder_whenShipping_throwsOrderCannotBeCancelled() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    @Test
    void cancelOrder_whenCompleted_throwsOrderCannotBeCancelled() {
        Order order = orderWithStatus(OrderStatus.COMPLETED);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    @Test
    void cancelOrder_whenAlreadyCancelled_throwsOrderCannotBeCancelled() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    @Test
    void cancelOrder_withMultipleItems_restoresEachVariant() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.getOrderItems().add(orderItem(variant1, 2));
        order.getOrderItems().add(orderItem(variant2, 3));

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.cancelOrder(ORDER_ID);

        assertThat(variant1.getStockQuantity()).isEqualTo(52);
        assertThat(variant2.getStockQuantity()).isEqualTo(33);
    }

    @Test
    void confirmOrder_whenPending_setsConfirmedAndSaves() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.confirmOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void confirmOrder_whenNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrder_whenCancelled_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrder_whenAlreadyConfirmed_throwsOrderAlreadyInStatus() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_IN_STATUS);
    }

    @Test
    void confirmOrder_whenShipping_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void confirmOrder_whenCompleted_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void shipOrder_whenConfirmed_setsShippingAndSaves() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.shipOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPING);
        verify(orderRepository).save(order);
    }

    @Test
    void shipOrder_whenNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.shipOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void shipOrder_whenCancelled_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void shipOrder_whenAlreadyShipping_throwsOrderAlreadyInStatus() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_IN_STATUS);
    }

    @Test
    void shipOrder_whenPending_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void shipOrder_whenCompleted_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.shipOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void completeOrder_whenShippingAndCod_setsCompletedAndPaymentPaid() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        Payment payment = paymentFor(order, PaymentStatus.PENDING, PaymentMethod.COD);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.completeOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
    }

    @Test
    void completeOrder_whenNonCodAndPaymentPending_alsoMarksPaid() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        Payment payment = paymentFor(order, PaymentStatus.PENDING, nonCodMethod());

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.completeOrder(ORDER_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void completeOrder_whenNonCodAndPaymentAlreadyPaid_completesWithoutTouchingPayment() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        Payment payment = paymentFor(order, PaymentStatus.PAID, nonCodMethod());

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        orderService.completeOrder(ORDER_ID);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completeOrder_whenNoPayment_throwsPaymentNotFound() {
        Order order = orderWithStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeOrder_whenNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void completeOrder_whenCancelled_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void completeOrder_whenAlreadyCompleted_throwsOrderAlreadyInStatus() {
        Order order = orderWithStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_IN_STATUS);
    }

    @Test
    void completeOrder_whenPending_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void completeOrder_whenConfirmed_throwsInvalidOrderStatus() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    void getAllOrdersForManage_returnsMappedList() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        var result = orderService.getAllOrdersForManage();

        assertThat(result).hasSize(1).containsExactly(orderResponse);
        verify(orderRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllOrdersForManage_whenEmpty_returnsEmptyList() {
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(orderService.getAllOrdersForManage()).isEmpty();
    }


    @Test
    void getOrderByIdForManage_whenExists_returnsResponseWithPayment() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        Payment payment = paymentFor(order, PaymentStatus.PENDING, PaymentMethod.COD);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(paymentResponse);

        var result = orderService.getOrderByIdForManage(ORDER_ID);

        assertThat(result).isSameAs(orderResponse);
        verify(paymentMapper).toPaymentResponse(payment);
    }

    @Test
    void getOrderByIdForManage_whenNotFound_throwsOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByIdForManage(ORDER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void getOrderByIdForManage_whenNoPayment_returnsResponseWithoutPayment() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);

        var result = orderService.getOrderByIdForManage(ORDER_ID);

        assertThat(result).isSameAs(orderResponse);
        verify(paymentMapper, never()).toPaymentResponse(any());
    }
}