package com.CAP.CAP.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.CAP.CAP.component.NetworkSimulator;
import com.CAP.CAP.dto.AccountResponseDTO;
import com.CAP.CAP.entity.Account;
import com.CAP.CAP.repository.AccountRepository;

import jakarta.transaction.Transactional;

@Service
public class APAccountService {

    private final AccountRepository repository;
    private final NetworkSimulator network;

    private final Map<UUID, Double> replica = new ConcurrentHashMap<>();

    public APAccountService(AccountRepository repository,
                            NetworkSimulator network) {
        this.repository = repository;
        this.network = network;
    }

    @Transactional
    public AccountResponseDTO updateBalance(UUID id, Double amount) {

        Account acc = repository.findById(id)
                .orElseThrow();

        acc.setBalance(acc.getBalance() + amount);
        acc.setVersion(acc.getVersion() + 1);

        repository.save(acc);

        if (network.isPartitioned()) {
            // Replica no recibe actualización
            return new AccountResponseDTO(
                    acc.getId(),
                    acc.getBalance(),
                    acc.getVersion()
            );
        }

        replica.put(id, acc.getBalance());

        return new AccountResponseDTO(
                acc.getId(),
                acc.getBalance(),
                acc.getVersion()
        );
    }

    public Double readFromReplica(UUID id) {
        return replica.get(id);
    }
}
