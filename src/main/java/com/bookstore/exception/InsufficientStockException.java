package com.bookstore.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String bookTitle, int requested, int available){
        super("Insufficient stock for : "+bookTitle+" requested : "+requested + " but only "+available + " available");
    }
}
