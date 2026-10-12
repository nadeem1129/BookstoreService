package com.bookstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "book_title", nullable = false)
    @Setter(AccessLevel.NONE)
    private String bookTitle;

    @Column(name = "book_isbn")
    @Setter(AccessLevel.NONE)
    private String bookIsbn;

    public OrderItem(Book book, int quantity, BigDecimal unitPrice){
        this.book = book;
        this.bookTitle = book.getTitle();
        this.bookIsbn = book.getIsbn();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    public BigDecimal getLineTotal(){
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}