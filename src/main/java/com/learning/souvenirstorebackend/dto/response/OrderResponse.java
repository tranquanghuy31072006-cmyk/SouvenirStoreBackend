package com.learning.souvenirstorebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.souvenirstorebackend.enums.OrderStatus;
import com.learning.souvenirstorebackend.enums.PaymentMethod;
import com.learning.souvenirstorebackend.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    Long id;

    String orderCode;

    String receiverName;

    String receiverPhone;

    String receiverAddress;

    BigDecimal totalAmount;

    BigDecimal shippingFee;

    BigDecimal discountAmount;

    BigDecimal finalAmount;

    PaymentStatus paymentStatus;

    PaymentMethod paymentMethod;

    OrderStatus orderStatus;

    String note;

    LocalDateTime createdAt;

    List<OrderItemResponse> items;

    PaymentResponse payment;
}
