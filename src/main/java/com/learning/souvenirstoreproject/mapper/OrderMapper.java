package com.learning.souvenirstoreproject.mapper;

import com.learning.souvenirstoreproject.dto.response.OrderItemResponse;
import com.learning.souvenirstoreproject.dto.response.OrderResponse;
import com.learning.souvenirstoreproject.entity.Order;
import com.learning.souvenirstoreproject.entity.OrderItem;
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
