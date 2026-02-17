package com.CAP.CAP.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CAP.CAP.entity.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
