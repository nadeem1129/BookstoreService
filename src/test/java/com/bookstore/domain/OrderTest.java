package com.bookstore.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void newOrderStartsCreated() {
        assertEquals(OrderStatus.CREATED, Order.builder().build().getStatus());
    }

    @Test
    void createdOrderCanBePaidOrCancelled() {
        Order paidOrder = new Order();
        paidOrder.markPaid();
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());

        Order cancelledOrder = new Order();
        cancelledOrder.cancel();
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    void terminalOrderCannotTransitionToDifferentStatus() {
        Order paidOrder = new Order();
        paidOrder.markPaid();

        assertThrows(IllegalStateException.class, paidOrder::cancel);
        assertThrows(IllegalStateException.class, () -> paidOrder.setStatus(OrderStatus.CREATED));
    }

    @Test
    void repeatedStatusTransitionIsIdempotent() {
        Order order = new Order();

        order.markPaid();
        order.markPaid();

        assertEquals(OrderStatus.PAID, order.getStatus());
    }
}
