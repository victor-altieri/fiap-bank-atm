package com.fiap.bank.atm.domain.repository;
import com.fiap.bank.atm.domain.model.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends ATMRepository<Account> {
    Optional<Account> findByAccountNumber(String accountNumber);
    // All accounts and their transactions must commit or roll back together.
    void saveAll(List<Account> accounts);
}