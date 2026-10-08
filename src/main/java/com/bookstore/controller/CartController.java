package com.bookstore.controller;

import com.bookstore.dto.AddToCartRequest;
import com.bookstore.dto.CartDto;
import com.bookstore.dto.UpdateCartItemRequest;
import com.bookstore.security.SecurityUtils;
import com.bookstore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService){
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(){
        return ResponseEntity.ok(cartService.getCart(SecurityUtils.currentUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody AddToCartRequest request){
        CartDto cart = cartService.addToCart(
                SecurityUtils.currentUserId(), request.bookId(), request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartDto> updateItem(@PathVariable Long bookId, @Valid @RequestBody UpdateCartItemRequest request){
        CartDto cart = cartService.updateItemQuantity(
                SecurityUtils.currentUserId(), bookId, request.quantity()
        );
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartDto> removeItem(@PathVariable Long bookId){
        CartDto cart = cartService.removeItem(SecurityUtils.currentUserId(), bookId);
        return ResponseEntity.ok(cart);
    }

}
