package com.CAP.CAP.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity
public class Account {

    @Id
    private UUID id;

    private Double balance;

    private Long version;

    // getters/setters

    public UUID getId() {
        return id;
    }

    public Double getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}