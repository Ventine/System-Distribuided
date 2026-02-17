package com.CAP.CAP.controller;

import java.util.UUID;

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
    public AccountResponseDTO create(@RequestBody CreateAccountDTO dto) {
        Account acc = new Account();
        acc.setId(UUID.randomUUID());
        acc.setBalance(dto.initialBalance());
        acc.setVersion(0L);
        repository.save(acc);

        return new AccountResponseDTO(
                acc.getId(),
                acc.getBalance(),
                acc.getVersion()
        );
    }

    @PostMapping("/{id}/cp/deposit")
    public AccountResponseDTO depositCP(@PathVariable UUID id,
                                        @RequestBody UpdateBalanceDTO dto) {
        return cpService.updateBalance(id, dto.amount());
    }

    @PostMapping("/{id}/ap/deposit")
    public AccountResponseDTO depositAP(@PathVariable UUID id,
                                        @RequestBody UpdateBalanceDTO dto) {
        return apService.updateBalance(id, dto.amount());
    }

    @PostMapping("/partition/on")
    public void enablePartition() {
        network.enablePartition();
    }

    @PostMapping("/partition/off")
    public void disablePartition() {
        network.disablePartition();
    }
}
