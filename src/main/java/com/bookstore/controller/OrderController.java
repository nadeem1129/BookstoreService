package com.bookstore.controller;

import com.bookstore.dto.OrderDto;
import com.bookstore.security.SecurityUtils;
import com.bookstore.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation. PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderDto> checkout(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencykey) {
        OrderDto order = orderService.checkout(SecurityUtils.currentUserId(), idempotencykey);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderDto> pay(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.pay(SecurityUtils.currentUserId(), id));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders() {
        return ResponseEntity.ok(orderService.getOrdersForUser(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(SecurityUtils.currentUserId(), id));
    }
}