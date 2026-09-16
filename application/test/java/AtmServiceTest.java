package com.fiap.bank.atm;
import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.application.dto.*;
import com.fiap.bank.atm.application.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

class AtmServiceTest {
    @TempDir Path dir;
    private AtmService service() { return new AtmService(dir.resolve("bank.db")); }
    @Test void depositWithdrawalAndStatementSurviveRestart() {
        AtmService s=service(); s.authenticate("12345","1234");
        s.deposit(new BigDecimal("50.25")); s.withdraw(new BigDecimal("20.00"));
        AtmService restarted=service(); restarted.authenticate("12345","1234");
        assertEquals(new BigDecimal("5030.25"),restarted.getBalance());
        assertEquals("WITHDRAWAL",restarted.getStatement().getFirst().type());
        assertEquals(5,restarted.getStatement().size());
        assertEquals(new BigDecimal("20.00"),restarted.getCurrentAccount().totalWithdrawnToday());
    }
    @Test void failureDoesNotLeakMutatedSessionState() {
        AtmService s=service(); s.authenticate("12345","1234");
        assertThrows(InsufficientFundsException.class, () -> s.withdraw(new BigDecimal("6000")));
        assertEquals(new BigDecimal("5000.00"),s.getBalance());
        assertEquals(3,s.getStatement().size());
    }
    @Test void attemptsAndBlockedStatusSurviveRestart() {
        assertThrows(InvalidPinException.class, () -> service().authenticate("12345","0000"));
        assertThrows(InvalidPinException.class, () -> service().authenticate("12345","0000"));
        assertThrows(AccountBlockedException.class, () -> service().authenticate("12345","0000"));
        assertThrows(AccountBlockedException.class, () -> service().authenticate("12345","1234"));
    }
    @Test void successfulLoginPersistsAttemptReset() {
        assertThrows(InvalidPinException.class, () -> service().authenticate("12345","0000"));
        service().authenticate("12345","1234");
        assertThrows(InvalidPinException.class, () -> service().authenticate("12345","0000"));
        assertThrows(InvalidPinException.class, () -> service().authenticate("12345","0000"));
        service().authenticate("12345","1234");
    }
    @Test void dailyLimitSurvivesRestart() {
        AtmService s=service(); s.authenticate("12345","1234"); s.withdraw(new BigDecimal("1500"));
        AtmService restarted=service(); restarted.authenticate("12345","1234");
        assertThrows(DailyLimitExceededException.class, () -> restarted.withdraw(BigDecimal.ONE));
    }
    @Test void transferPersistsBothSides() {
        AtmService s=service(); s.authenticate("12345","1234"); s.transfer("67890",new BigDecimal("100"));
        assertEquals(new BigDecimal("4900.00"),s.getBalance());
        AtmService target=service(); target.authenticate("67890","5678");
        assertEquals(new BigDecimal("1300.00"),target.getBalance());
        assertEquals("TRANSFER_IN",target.getStatement().getFirst().type());
    }
    @Test void logoutAndFailedAuthenticationClearSession() {
        AtmService s=service(); s.authenticate("12345","1234"); s.logout();
        assertFalse(s.isAuthenticated());
        assertThrows(IllegalStateException.class, s::getBalance);
        s.authenticate("12345","1234");
        assertThrows(InvalidPinException.class, () -> s.authenticate("missing","1234"));
        assertFalse(s.isAuthenticated());
    }
    @Test void dtoRecordsAndPublicSignaturesDoNotExposeInternalTypes() {
        assertTrue(AccountInfoDTO.class.isRecord()); assertTrue(TransactionDTO.class.isRecord());
        for (Method m : AtmService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                assertSafe(m.getGenericReturnType());
                for (Type t : m.getGenericParameterTypes()) assertSafe(t);
            }
        }
        for (Constructor<?> c : AtmService.class.getConstructors()) {
            for (Type t : c.getGenericParameterTypes()) assertSafe(t);
        }
    }
    private void assertSafe(Type t) {
        assertFalse(t.getTypeName().contains(".domain."), t.toString());
        assertFalse(t.getTypeName().contains(".infrastructure."), t.toString());
    }
}
