package com.bookstore.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Setter
@Getter
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Builder
    public Cart(User user){
        this.user = user;
    }


    public CartItem addItem(Book book, int quantity) {
        CartItem existing = findItemByBook(book.getId()).orElse(null);
        if (existing != null) {
            existing.increaseQuantity(quantity);
            return existing;
        }
        CartItem item = new CartItem(this, book, quantity);
        items.add(item);
        return item;
    }
    public void updateItemQuantity(Long bookId, int quantity) {
        CartItem item = findItemByBook(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not in cart: " + bookId));
        item.setQuantity(quantity);
    }

    public void removeItem(Long bookId) {
        items.removeIf(i -> i.getBook().getId().equals(bookId));
    }
    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
    public Optional<CartItem> findItemByBook(Long bookId) {
        return items.stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst();
    }
    public BigDecimal getsubtotal() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }



}
