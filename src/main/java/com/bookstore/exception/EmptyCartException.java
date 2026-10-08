package com.bookstore.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(){
        super("Can not checkout with Empty Cart");
    }
}
