package com.fiap.bank.atm;
import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.presentation.AtmFrame;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        AtmService service = new AtmService(Path.of(System.getProperty("atm.database", "data/fiap-bank.db")));
        SwingUtilities.invokeLater(() -> new AtmFrame(service).setVisible(true));
    }
}
