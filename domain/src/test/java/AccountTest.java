package com.fiap.bank.atm;
import com.fiap.bank.atm.domain.model.*;
import com.fiap.bank.atm.domain.exception.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private Account account() { return new Account(UUID.randomUUID(), "1", "1234", Money.of(2000), Money.of(500)); }
    @Test void dailyLimitUsesOnlyTodaysWithdrawals() {
        Account a = account();
        a.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(1), TransactionType.WITHDRAWAL, Money.of(500), "ontem"));
        a.deposit(Money.of(20));
        a.withdraw(Money.of(500));
        assertEquals(Money.of(500), a.getTotalWithdrawnToday());
        assertThrows(DailyLimitExceededException.class, () -> a.withdraw(Money.of(1)));
    }
    @Test void rejectsNonPositiveValuesWithoutChangingHistory() {
        Account a = account();
        assertThrows(IllegalArgumentException.class, () -> a.deposit(Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> a.withdraw(Money.of(-10)));
        assertEquals(Money.of(2000), a.getBalance());
        assertTrue(a.getTransactions().isEmpty());
    }
    @Test void transferPreservesCombinedBalance() {
        Account a=account(), b=new Account(UUID.randomUUID(), "2", "1234", Money.of(100), Money.of(500));
        a.transfer(b, Money.of(50));
        assertEquals(Money.of(2100), a.getBalance().plus(b.getBalance()));
        assertEquals(TransactionType.TRANSFER_OUT, a.getTransactions().getFirst().getType());
        assertEquals(TransactionType.TRANSFER_IN, b.getTransactions().getFirst().getType());
    }
    @Test void rejectsSelfTransferAndInsufficientFunds() {
        Account a=account();
        assertThrows(IllegalArgumentException.class, () -> a.transfer(a, Money.of(10)));
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(Money.of(3000)));
    }
}
