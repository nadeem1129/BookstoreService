package com.bookstore.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_items_cart_book", columnNames = {"cart_id", "book_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    private int quantity;

    public CartItem(Cart cart, Book book, int quantity) {
        this.cart = cart;
        this.book = book;
        setQuantity(quantity);
    }

    public void setQuantity(int quantity) {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
                this.quantity = quantity;
    }

    public void increaseQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }
    public BigDecimal getLineTotal() {
        return book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }


}
