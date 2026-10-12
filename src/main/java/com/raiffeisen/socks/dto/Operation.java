package com.raiffeisen.socks.dto;

import java.util.Locale;

/**
 * Оператор сравнения для GET /api/socks.
 * Значения в URL: moreThan, lessThan, equal.
 */
public enum Operation {
    MORE_THAN,
    LESS_THAN,
    EQUAL;

    /**
     * Разбирает query-параметр operation (moreThan/lessThan/equal).
     * Бросает IllegalArgumentException при неизвестном значении → маппится в 400.
     */
    public static Operation fromParam(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("параметр operation отсутствует");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "morethan" -> MORE_THAN;
            case "lessthan" -> LESS_THAN;
            case "equal" -> EQUAL;
            default -> throw new IllegalArgumentException(
                    "недопустимая операция: " + value + ". Допустимо: moreThan, lessThan, equal");
        };
    }
}
