package com.bookstore.service;

import com.bookstore.dto.OrderDto;

import java.util.List;

public interface OrderService {
    OrderDto checkout(Long userId, String idempotencyKey);
    OrderDto pay(Long userId, Long orderId);
    List<OrderDto> getOrdersForUser(Long userId);

    OrderDto getOrder(Long userId, Long orderId);

}
