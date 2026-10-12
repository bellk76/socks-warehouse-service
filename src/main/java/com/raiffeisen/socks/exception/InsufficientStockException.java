package com.raiffeisen.socks.exception;

/**
 * Бизнес-ошибка: на складе недостаточно носков для списания.
 * Маппится в HTTP 400.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
