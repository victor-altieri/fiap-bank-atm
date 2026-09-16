package com.fiap.bank.atm.application.service;
import com.fiap.bank.atm.application.dto.*;
import com.fiap.bank.atm.application.exception.*;
import com.fiap.bank.atm.domain.model.*;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.database.*;
import com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public final class AtmService {
    private final AccountRepository repository;
    private Optional<UUID> currentAccountId = Optional.empty();

    // Public construction exposes only Java types; infrastructure stays behind application.
    public AtmService(Path database) {
        ConnectionFactory factory = new ConnectionFactory(database);
        DatabaseInitializer.initialize(factory);
        repository = new AccountRepositoryJdbcImpl(factory);
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        currentAccountId = Optional.empty();
        return boundary(() -> {
            Account account = repository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new InvalidPinException("Conta não encontrada."));
            try {
                account.authenticate(pin);
            } catch (com.fiap.bank.atm.domain.exception.InvalidPinException |
                     com.fiap.bank.atm.domain.exception.AccountBlockedException e) {
                repository.save(account);
                throw e;
            }
            repository.save(account); // Persist the reset of failed attempts as well.
            currentAccountId = Optional.of(account.getId());
            return toDto(account);
        });
    }

    public void withdraw(BigDecimal amount) {
        boundary(() -> {
            Account account = authenticatedAccount();
            account.withdraw(Money.of(amount));
            repository.save(account);
            return Boolean.TRUE;
        });
    }

    public void deposit(BigDecimal amount) {
        boundary(() -> {
            Account account = authenticatedAccount();
            account.deposit(Money.of(amount));
            repository.save(account);
            return Boolean.TRUE;
        });
    }

    public void transfer(String targetAccountNumber, BigDecimal amount) {
        boundary(() -> {
            Account source = authenticatedAccount();
            Account target = repository.findByAccountNumber(targetAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));
            source.transfer(target, Money.of(amount));
            repository.saveAll(List.of(source, target));
            return Boolean.TRUE;
        });
    }

    public BigDecimal getBalance() { return authenticatedAccount().getBalance().getAmount(); }
    public AccountInfoDTO getCurrentAccount() { return toDto(authenticatedAccount()); }
    public List<TransactionDTO> getStatement() {
        // Stable reverse order also preserves insertion order for equal timestamps.
        List<Transaction> history = new ArrayList<>(authenticatedAccount().getTransactions());
        Collections.reverse(history);
        return history.stream().map(tx -> new TransactionDTO(tx.getId(), tx.getTimestamp(),
                tx.getType().name(), tx.getType().getDescription(), tx.getAmount().getAmount(),
                tx.getDescription())).toList();
    }
    public void logout() { currentAccountId = Optional.empty(); }
    public Boolean isAuthenticated() { return currentAccountId.isPresent(); }

    private Account authenticatedAccount() {
        UUID id = currentAccountId.orElseThrow(() ->
                new IllegalStateException("Nenhum usuário está autenticado no momento."));
        return repository.findById(id).orElseThrow(() -> new IllegalStateException("Conta não encontrada."));
    }
    private AccountInfoDTO toDto(Account account) {
        return new AccountInfoDTO(account.getId(), account.getAgency(), account.getAccountNumber(),
                account.getBalance().getAmount(), account.getDailyWithdrawalLimit().getAmount(),
                account.getTotalWithdrawnToday().getAmount(), account.isBlocked());
    }
    private <T> T boundary(Supplier<T> operation) {
        try { return operation.get(); }
        catch (com.fiap.bank.atm.domain.exception.AccountBlockedException e) { throw new AccountBlockedException(e.getMessage()); }
        catch (com.fiap.bank.atm.domain.exception.InvalidPinException e) { throw new InvalidPinException(e.getMessage()); }
        catch (com.fiap.bank.atm.domain.exception.InsufficientFundsException e) { throw new InsufficientFundsException(e.getMessage()); }
        catch (com.fiap.bank.atm.domain.exception.DailyLimitExceededException e) { throw new DailyLimitExceededException(e.getMessage()); }
    }
}
