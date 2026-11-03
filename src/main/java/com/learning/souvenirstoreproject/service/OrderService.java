package com.learning.souvenirstoreproject.service;

import com.learning.souvenirstoreproject.dto.request.OrderCreationRequest;
import com.learning.souvenirstoreproject.dto.response.OrderResponse;
import com.learning.souvenirstoreproject.entity.*;
import com.learning.souvenirstoreproject.enums.OrderStatus;
import com.learning.souvenirstoreproject.enums.PaymentMethod;
import com.learning.souvenirstoreproject.enums.PaymentStatus;
import com.learning.souvenirstoreproject.enums.ProductVariantStatus;
import com.learning.souvenirstoreproject.exception.AppException;
import com.learning.souvenirstoreproject.exception.ErrorCode;
import com.learning.souvenirstoreproject.mapper.OrderMapper;
import com.learning.souvenirstoreproject.mapper.PaymentMapper;
import com.learning.souvenirstoreproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    CartRepository cartRepository;
    UserRepository userRepository;
    ProductVariantRepository productVariantRepository;
    OrderMapper orderMapper;
    PaymentMapper paymentMapper;

    static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");
    static final BigDecimal ZERO = BigDecimal.ZERO;

    @Transactional
    public OrderResponse createOrder(OrderCreationRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);

        List<CartItem> itemsToCheckout = new ArrayList<>();

        if (request.getBuyNowProductVariantId() != null) {
            ProductVariant variant = productVariantRepository.findById(request.getBuyNowProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            CartItem tempItem = CartItem.builder()
                    .productVariant(variant)
                    .quantity(request.getBuyNowQuantity())
                    .price(variant.getSalePrice() != null ? variant.getSalePrice() : variant.getPrice())
                    .build();

            itemsToCheckout.add(tempItem);
        }
        else {
            if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty())
                throw new AppException(ErrorCode.CART_EMPTY);

            if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
                for (CartItem item : cart.getCartItems()) {
                    if (request.getCartItemIds().contains(item.getId()))
                        itemsToCheckout.add(item);
                }
            } else
                itemsToCheckout.addAll(cart.getCartItems());
        }

        if (itemsToCheckout.isEmpty())
            throw new AppException(ErrorCode.CART_EMPTY);

        Order order = Order.builder()
                .user(user)
                .orderCode("ORD-" + UUID.randomUUID())
                .receiverName(request.getReceiverName())
                .receiverAddress(request.getReceiverAddress())
                .receiverPhone(request.getReceiverPhone())
                .note(request.getNote())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(ZERO)
                .build();

        BigDecimal totalAmount = ZERO;

        for (CartItem item : itemsToCheckout) {
            ProductVariant variant = item.getProductVariant();

            if (variant.getStatus() != ProductVariantStatus.ACTIVE)
                throw new AppException(ErrorCode.PRODUCT_VARIANT_INACTIVE);

            int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

            if (item.getQuantity() > stock)
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);

            variant.setStockQuantity(stock - item.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productVariant(variant)
                    .productName(variant.getProduct().getName())
                    .productImage(variant.getProduct().getThumbnail())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();

            order.getOrderItems().add(orderItem);

            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        order.setTotalAmount(totalAmount);

        order.setFinalAmount(totalAmount.add(SHIPPING_FEE));

        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .transactionCode(UUID.randomUUID().toString())
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        if (cart != null && request.getBuyNowProductVariantId() == null) {
            cart.getCartItems().removeAll(itemsToCheckout);

            cartRepository.save(cart);
        }

        OrderResponse response = orderMapper.toOrderResponse(order);

        response.setPayment(paymentMapper.toPaymentResponse(payment));

        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderId(Long orderId) {
        User user = getCurrentUser();

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId()).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderResponse response = orderMapper.toOrderResponse(order);

        paymentRepository.findByOrderId(order.getId()).ifPresent(
                payment -> response.setPayment(paymentMapper.toPaymentResponse(payment)));

        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId() {
        User user = getCurrentUser();

        return orderRepository.findByUserId(user.getId()).stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId()).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.PENDING)
            throw new AppException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);

        for (OrderItem orderItem : order.getOrderItems()) {
            ProductVariant variant = orderItem.getProductVariant();

            int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

            variant.setStockQuantity(stock + orderItem.getQuantity());
        }

        PaymentStatus newPaymentStatus = order.getPaymentStatus() == PaymentStatus.PAID
                ? PaymentStatus.REFUNDED : PaymentStatus.FAILED;

        order.setOrderStatus(OrderStatus.CANCELLED);

        order.setPaymentStatus(newPaymentStatus);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

        if (payment != null)
            payment.setStatus(newPaymentStatus);

        OrderResponse response = orderMapper.toOrderResponse(order);

        if (payment != null)
            response.setPayment(paymentMapper.toPaymentResponse(payment));

        return response;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() == OrderStatus.CANCELLED)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        if (order.getOrderStatus() == OrderStatus.CONFIRMED)
            throw new AppException(ErrorCode.ORDER_ALREADY_IN_STATUS);

        if (order.getOrderStatus() != OrderStatus.PENDING)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        order.setOrderStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() == OrderStatus.CANCELLED)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        if (order.getOrderStatus() == OrderStatus.SHIPPING)
            throw new AppException(ErrorCode.ORDER_ALREADY_IN_STATUS);

        if (order.getOrderStatus() != OrderStatus.CONFIRMED)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        order.setOrderStatus(OrderStatus.SHIPPING);

        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() == OrderStatus.CANCELLED)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        if (order.getOrderStatus() == OrderStatus.COMPLETED)
            throw new AppException(ErrorCode.ORDER_ALREADY_IN_STATUS);

        if (order.getOrderStatus() != OrderStatus.SHIPPING)
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        order.setOrderStatus(OrderStatus.COMPLETED);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow(()
                -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentMethod() == PaymentMethod.COD || payment.getStatus() == PaymentStatus.PENDING) {

            payment.setStatus(PaymentStatus.PAID);

            payment.setPaidAt(LocalDateTime.now());

            order.setPaymentStatus(PaymentStatus.PAID);

            paymentRepository.save(payment);
        }

        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public List<OrderResponse> getAllOrdersForManage() {

        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(orderMapper::toOrderResponse).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public OrderResponse getOrderByIdForManage(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()
                -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderResponse response = orderMapper.toOrderResponse(order);

        paymentRepository.findByOrderId(order.getId()).ifPresent(
                payment -> response.setPayment(paymentMapper.toPaymentResponse(payment)));

        return response;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}