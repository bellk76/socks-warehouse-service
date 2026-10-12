package com.raiffeisen.socks.controller;

import com.raiffeisen.socks.dto.Operation;
import com.raiffeisen.socks.dto.SocksRequest;
import com.raiffeisen.socks.service.SocksService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/socks")
@RequiredArgsConstructor
public class SocksController {

    private final SocksService socksService;

    @PostMapping("/income")
    public ResponseEntity<Void> income(@Valid @RequestBody SocksRequest request) {
        socksService.income(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/outcome")
    public ResponseEntity<Void> outcome(@Valid @RequestBody SocksRequest request) {
        socksService.outcome(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Long> getTotal(@RequestParam String color,
                                         @RequestParam String operation,
                                         @RequestParam Integer cottonPart) {
        Long total = socksService.getTotal(color, Operation.fromParam(operation), cottonPart);
        return ResponseEntity.ok(total);
    }
}
