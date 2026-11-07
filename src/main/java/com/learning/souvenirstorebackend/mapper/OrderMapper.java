package com.learning.souvenirstorebackend.mapper;

import com.learning.souvenirstorebackend.dto.response.OrderItemResponse;
import com.learning.souvenirstorebackend.dto.response.OrderResponse;
import com.learning.souvenirstorebackend.entity.Order;
import com.learning.souvenirstorebackend.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", source = "orderItems")
    OrderResponse toOrderResponse(Order order);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "productId", source = "productVariant.product.id")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
