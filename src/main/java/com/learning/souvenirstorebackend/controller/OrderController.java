
package com.learning.souvenirstorebackend.controller;

import com.learning.souvenirstorebackend.dto.request.OrderCreationRequest;
import com.learning.souvenirstorebackend.dto.response.ApiResponse;
import com.learning.souvenirstorebackend.dto.response.OrderResponse;
import com.learning.souvenirstorebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreationRequest orderCreationRequest) {

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(orderCreationRequest))
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderByOrderId(orderId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getMyOrders() {

        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getOrdersByUserId())
                .build();
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId) {

        return ApiResponse.<OrderResponse>builder()
                .result(orderService.cancelOrder(orderId))
                .build();
    }

    @PatchMapping("/{orderId}/confirm")
    public ApiResponse<OrderResponse> confirmOrder(@PathVariable Long orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.confirmOrder(orderId))
                .build();
    }

    @PatchMapping("/{orderId}/ship")
    public ApiResponse<OrderResponse> shipOrder(@PathVariable Long orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.shipOrder(orderId))
                .build();
    }

    @PatchMapping("/{orderId}/complete")
    public ApiResponse<OrderResponse> completeOrder(@PathVariable Long orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.completeOrder(orderId))
                .build();
    }

    @GetMapping("/manage")
    public ApiResponse<List<OrderResponse>> getAllOrdersForManage() {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getAllOrdersForManage())
                .build();
    }

    @GetMapping("/manage/{orderId}")
    public ApiResponse<OrderResponse> getOrderByIdForManage(@PathVariable Long orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderByIdForManage(orderId))
                .build();
    }
}