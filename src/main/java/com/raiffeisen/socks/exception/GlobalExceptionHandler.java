package com.raiffeisen.socks.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        // Не прокидываем клиенту внутренние имена пакетов/классов
        if (msg != null && msg.contains("com.raiffeisen")) {
            return ResponseEntity.badRequest()
                    .body("Некорректный параметр запроса. Допустимые значения операции: moreThan, lessThan, equal");
        }
        return ResponseEntity.badRequest().body(msg);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Нарушение ограничений БД: quantity >= 0 (гонка при расходе) → 400.
     * Прочие нарушения целостности → 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getRootCause() != null
                ? ex.getRootCause().getMessage()
                : ex.getMessage();
        if (rootMessage != null && rootMessage.contains("ck_socks_quantity")) {
            return ResponseEntity.badRequest()
                    .body("Некорректное количество: превышены остатки на складе");
        }
        log.error("Нарушение целостности данных", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка целостности данных");
    }

    /**
     * Маршрут не найден (например, GET / или неизвестный путь) → 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Маршрут не найден. Доступны: POST /api/socks/income, "
                        + "POST /api/socks/outcome, GET /api/socks");
    }

    /**
     * Не указан обязательный query-параметр → 400.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body("Не указан обязательный параметр: " + ex.getParameterName());
    }

    /**
     * Неверный HTTP-метод для существующего маршрута → 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("HTTP-метод " + ex.getMethod() + " не поддерживается. "
                        + "Используйте POST для /api/socks/income и /api/socks/outcome, GET для /api/socks");
    }

    /**
     * Битое/пустое тело запроса (не JSON) → 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body("Некорректное тело запроса. Ожидается JSON вида {color, cottonPart, quantity}");
    }

    /**
     * Нечисловое значение параметра (например, cottonPart=abc) → 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body("Некорректное значение параметра: " + ex.getName());
    }

    /**
     * Неверный Content-Type (ожидается application/json) → 400.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.badRequest()
                .body("Некорректный Content-Type. Ожидается application/json");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        log.error("Внутренняя ошибка сервера", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Внутренняя ошибка сервера");
    }
}
