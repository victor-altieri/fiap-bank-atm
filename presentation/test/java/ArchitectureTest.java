package com.fiap.bank.atm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTest {
    @Test void presentationClasspathContainsDtosButExcludesInternalLayers() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        assertNotNull(Class.forName("com.fiap.bank.atm.application.dto.AccountInfoDTO", false, loader));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.fiap.bank.atm.domain.model.Account", false, loader));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl", false, loader));
    }
}
