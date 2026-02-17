package com.CAP.CAP.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CAP.CAP.component.*;
import com.CAP.CAP.dto.*;
import com.CAP.CAP.entity.Account;
import com.CAP.CAP.repository.*;
import com.CAP.CAP.service.*;
import com.CAP.CAP.wrapper.ApiResponse;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final CPAccountService cpService;
    private final APAccountService apService;
    private final AccountRepository repository;
    private final NetworkSimulator network;

    public AccountController(CPAccountService cpService,
                             APAccountService apService,
                             AccountRepository repository,
                             NetworkSimulator network) {
        this.cpService = cpService;
        this.apService = apService;
        this.repository = repository;
        this.network = network;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponseDTO>> create(
            @RequestBody CreateAccountDTO dto) {

        Account acc = new Account();
        acc.setId(UUID.randomUUID());
        acc.setBalance(dto.initialBalance());
        acc.setVersion(0L);

        repository.save(acc);

        AccountResponseDTO body = new AccountResponseDTO(
                acc.getId(),
                acc.getBalance(),
                acc.getVersion()
        );

        ApiResponse<AccountResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Account created successfully",
                        "N/A",
                        network.isPartitioned(),
                        body,
                        Instant.now()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/cp/deposit")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> depositCP(
            @PathVariable UUID id,
            @RequestBody UpdateBalanceDTO dto) {

        AccountResponseDTO result = cpService.updateBalance(id, dto.amount());

        ApiResponse<AccountResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Deposit processed under CP strategy (consistency prioritized)",
                        "CP",
                        network.isPartitioned(),
                        result,
                        Instant.now()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ap/deposit")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> depositAP(
            @PathVariable UUID id,
            @RequestBody UpdateBalanceDTO dto) {

        AccountResponseDTO result = apService.updateBalance(id, dto.amount());

        ApiResponse<AccountResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Deposit processed under AP strategy (availability prioritized)",
                        "AP",
                        network.isPartitioned(),
                        result,
                        Instant.now()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/partition/on")
    public ResponseEntity<ApiResponse<Void>> enablePartition() {

        network.enablePartition();

        ApiResponse<Void> response =
                new ApiResponse<>(
                        true,
                        "Network partition enabled",
                        null,
                        true,
                        null,
                        Instant.now()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/partition/off")
    public ResponseEntity<ApiResponse<Void>> disablePartition() {

        network.disablePartition();

        ApiResponse<Void> response =
                new ApiResponse<>(
                        true,
                        "Network partition disabled",
                        null,
                        false,
                        null,
                        Instant.now()
                );

        return ResponseEntity.ok(response);
    }
}
