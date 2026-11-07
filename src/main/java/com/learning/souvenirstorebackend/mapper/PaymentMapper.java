package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.response.PaymentResponse;
import com.learning.souvenirstorebackend.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);
}
