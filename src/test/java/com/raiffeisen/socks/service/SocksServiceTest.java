package com.raiffeisen.socks.service;

import com.raiffeisen.socks.dto.Operation;
import com.raiffeisen.socks.dto.SocksRequest;
import com.raiffeisen.socks.exception.InsufficientStockException;
import com.raiffeisen.socks.repository.SocksRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocksServiceTest {

    @Mock
    private SocksRepository socksRepository;

    @InjectMocks
    private SocksService socksService;

    @Test
    void income_normalizesColorToLowerCase() {
        SocksRequest request = new SocksRequest("Red", 80, 10L);

        socksService.income(request);

        verify(socksRepository).upsertIncome("red", 80, 10L);
    }

    @Test
    void income_trimsColor() {
        SocksRequest request = new SocksRequest("  black  ", 50, 5L);

        socksService.income(request);

        verify(socksRepository).upsertIncome("black", 50, 5L);
    }

    @Test
    void outcome_success_remainingPositive_noDelete() {
        SocksRequest request = new SocksRequest("red", 80, 10L);
        when(socksRepository.decreaseQuantity("red", 80, 10L)).thenReturn(40L);

        socksService.outcome(request);

        verify(socksRepository).decreaseQuantity("red", 80, 10L);
        verify(socksRepository, never()).deleteIfZero(anyString(), anyInt());
    }

    @Test
    void outcome_remainingZero_deletesRow() {
        SocksRequest request = new SocksRequest("red", 80, 50L);
        when(socksRepository.decreaseQuantity("red", 80, 50L)).thenReturn(0L);

        socksService.outcome(request);

        verify(socksRepository).deleteIfZero("red", 80);
    }

    @Test
    void outcome_insufficient_throwsException() {
        SocksRequest request = new SocksRequest("red", 80, 100L);
        when(socksRepository.decreaseQuantity("red", 80, 100L)).thenReturn(null);

        assertThrows(InsufficientStockException.class, () -> socksService.outcome(request));
        verify(socksRepository, never()).deleteIfZero(anyString(), anyInt());
    }

    @Test
    void getTotal_normalizesColorAndPassesOperation() {
        when(socksRepository.sumByColorAndCottonPart("red", "MORE_THAN", 90)).thenReturn(10L);

        Long total = socksService.getTotal("Red", Operation.MORE_THAN, 90);

        assertEquals(10L, total);
        verify(socksRepository).sumByColorAndCottonPart("red", "MORE_THAN", 90);
    }

    @Test
    void getTotal_trimsColor() {
        when(socksRepository.sumByColorAndCottonPart("black", "LESS_THAN", 10)).thenReturn(0L);

        Long total = socksService.getTotal("  black  ", Operation.LESS_THAN, 10);

        assertEquals(0L, total);
        verify(socksRepository).sumByColorAndCottonPart("black", "LESS_THAN", 10);
    }
}
