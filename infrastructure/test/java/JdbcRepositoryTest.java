package com.fiap.bank.atm;
import com.fiap.bank.atm.domain.model.*;
import com.fiap.bank.atm.infrastructure.database.*;
import com.fiap.bank.atm.infrastructure.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JdbcRepositoryTest {
    @TempDir Path dir;
    ConnectionFactory factory;
    AccountRepositoryJdbcImpl repository;
    @BeforeEach void setup() {
        factory = new ConnectionFactory(dir.resolve("bank.db"));
        DatabaseInitializer.initialize(factory);
        repository = new AccountRepositoryJdbcImpl(factory);
    }
    private Account find(String number) { return repository.findByAccountNumber(number).orElseThrow(); }
    @Test void missingAndSqlInjectionInputsReturnEmpty() {
        assertTrue(repository.findByAccountNumber("inexistente").isEmpty());
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findByAccountNumber("' OR 1=1 --").isEmpty());
        assertEquals(Money.of(5000),find("12345").getBalance());
    }
    @Test void reloadRestoresTransactionFieldsWithoutDuplicates() {
        Account a = find("12345");
        a.withdraw(Money.of(20));
        Transaction tx = a.getTransactions().getLast();
        repository.save(a);
        repository.save(a);
        Account restored = new AccountRepositoryJdbcImpl(factory).findById(a.getId()).orElseThrow();
        assertEquals(4, restored.getTransactions().size());
        Transaction actual = restored.getTransactions().getLast();
        assertEquals(tx.getId(),actual.getId());
        assertEquals(tx.getTimestamp(),actual.getTimestamp());
        assertEquals(tx.getType(),actual.getType());
        assertEquals(tx.getDescription(),actual.getDescription());
        assertEquals(tx.getAmount(),actual.getAmount());
        assertEquals(Money.of(4980),restored.getBalance());
    }
    @Test void initializationDoesNotResetExistingData() {
        Account a=find("12345"); a.deposit(Money.of(70)); repository.save(a);
        DatabaseInitializer.initialize(factory);
        assertEquals(Money.of(5070),find("12345").getBalance());
        assertEquals(4,find("12345").getTransactions().size());
        assertEquals(Money.of(1500),find("123456").getBalance());
        assertTrue(find("111111").isBlocked());
    }
    @Test void staleWriteRollsBackEntireTransfer() {
        Account source=find("12345"), staleTarget=find("67890");
        Account freshTarget=find("67890");
        freshTarget.deposit(Money.of(10)); repository.save(freshTarget);
        source.transfer(staleTarget,Money.of(50));
        assertThrows(IllegalStateException.class, () -> repository.saveAll(List.of(source,staleTarget)));
        assertEquals(Money.of(5000),find("12345").getBalance());
        assertEquals(3,find("12345").getTransactions().size());
        assertEquals(Money.of(1210),find("67890").getBalance());
    }
    @Test void failedTransactionInsertRollsBackBalance() throws Exception {
        try (var c=factory.open(); var s=c.prepareStatement("""
            CREATE TRIGGER fail_test BEFORE INSERT ON tb_transaction
            WHEN NEW.type = 'WITHDRAWAL' BEGIN SELECT RAISE(ABORT, 'injected failure'); END
            """)) { s.execute(); }
        Account a=find("12345"); a.withdraw(Money.of(20));
        assertThrows(IllegalStateException.class, () -> repository.save(a));
        assertEquals(Money.of(5000),find("12345").getBalance());
        assertEquals(3,find("12345").getTransactions().size());
    }
    @Test void saveCanInsertNewAccountAndForeignKeysAreEnabled() throws Exception {
        Account a=new Account(UUID.randomUUID(),"77777","1234",Money.of(30),Money.of(100));
        repository.save(a);
        assertEquals(Money.of(30),find("77777").getBalance());
        try (var c=factory.open(); var s=c.prepareStatement("PRAGMA foreign_keys"); var r=s.executeQuery()) {
            assertTrue(r.next()); assertEquals(1,r.getInt(1));
        }
    }
}
