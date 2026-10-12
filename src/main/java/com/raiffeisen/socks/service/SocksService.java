package com.raiffeisen.socks.service;

import com.raiffeisen.socks.dto.Operation;
import com.raiffeisen.socks.dto.SocksRequest;
import com.raiffeisen.socks.exception.InsufficientStockException;
import com.raiffeisen.socks.repository.SocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocksService {

    private final SocksRepository socksRepository;

    /**
     * Приход: атомарный upsert (INSERT ... ON CONFLICT DO UPDATE).
     */
    @Transactional
    public void income(SocksRequest request) {
        String color = normalizeColor(request.getColor());
        socksRepository.upsertIncome(color, request.getCottonPart(), request.getQuantity());
        log.info("Приход: color={}, cottonPart={}, quantity={}",
                color, request.getCottonPart(), request.getQuantity());
    }

    /**
     * Расход: уменьшает количество; при нуле удаляет строку (очистка).
     */
    @Transactional
    public void outcome(SocksRequest request) {
        String color = normalizeColor(request.getColor());
        Long remaining = socksRepository.decreaseQuantity(
                color, request.getCottonPart(), request.getQuantity());

        if (remaining == null) {
            throw new InsufficientStockException(
                    "Недостаточно носков на складе: " + color + ", "
                            + request.getCottonPart() + "% хлопка, запрошено " + request.getQuantity());
        }
        if (remaining == 0) {
            socksRepository.deleteIfZero(color, request.getCottonPart());
        }
        log.info("Расход: color={}, cottonPart={}, quantity={}, остаток={}",
                color, request.getCottonPart(), request.getQuantity(), remaining);
    }

    /**
     * Суммарное количество по цвету и фильтру процентного содержания хлопка.
     */
    @Transactional(readOnly = true)
    public Long getTotal(String color, Operation operation, Integer cottonPart) {
        String normalizedColor = normalizeColor(color);
        return socksRepository.sumByColorAndCottonPart(
                normalizedColor, operation.name(), cottonPart);
    }

    /**
     * Нормализация цвета перед любыми операциями с БД
     * (единый регистр для уникальности и фильтрации).
     */
    private String normalizeColor(String color) {
        return color.toLowerCase(Locale.ROOT).trim();
    }
}
