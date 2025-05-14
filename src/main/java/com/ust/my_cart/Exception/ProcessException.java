package com.ust.my_cart.Exception;

public class ProcessException extends RuntimeException {
    private final int statusCode;
    public ProcessException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public int getStatusCode() {
        return statusCode;
    }
}
