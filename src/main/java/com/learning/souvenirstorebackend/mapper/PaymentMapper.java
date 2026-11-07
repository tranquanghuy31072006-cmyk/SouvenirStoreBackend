package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.response.PaymentResponse;
import com.learning.souvenirstorebackend.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "paymentStatus", ignore = true)
    PaymentResponse toPaymentResponse(Payment payment);
}
