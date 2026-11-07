package com.learning.souvenirstorebackend.dto.response;

import com.learning.souvenirstorebackend.enums.PaymentMethod;
import com.learning.souvenirstorebackend.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {

    Long id;

    PaymentMethod paymentMethod;

    String transactionCode;

    BigDecimal amount;

    PaymentStatus paymentStatus;

    LocalDateTime paidAt;
}