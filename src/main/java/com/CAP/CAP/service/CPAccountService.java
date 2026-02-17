package com.CAP.CAP.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.CAP.CAP.component.NetworkSimulator;
import com.CAP.CAP.dto.AccountResponseDTO;
import com.CAP.CAP.entity.Account;
import com.CAP.CAP.repository.AccountRepository;

import jakarta.transaction.Transactional;

@Service
public class CPAccountService {

    private final AccountRepository repository;
    private final NetworkSimulator network;

    public CPAccountService(AccountRepository repository,
                            NetworkSimulator network) {
        this.repository = repository;
        this.network = network;
    }

    @Transactional
    public AccountResponseDTO updateBalance(UUID id, Double amount) {

        if (network.isPartitioned()) {
            throw new RuntimeException("Partition detected - rejecting for consistency");
        }

        Account acc = repository.findById(id)
                .orElseThrow();

        acc.setBalance(acc.getBalance() + amount);
        acc.setVersion(acc.getVersion() + 1);

        repository.save(acc);

        return new AccountResponseDTO(
                acc.getId(),
                acc.getBalance(),
                acc.getVersion()
        );
    }
}
