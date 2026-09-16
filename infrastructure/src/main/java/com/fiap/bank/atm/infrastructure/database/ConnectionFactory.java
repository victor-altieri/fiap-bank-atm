package com.fiap.bank.atm.infrastructure.database;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {
    private final String url;
    public ConnectionFactory(Path database) {
        try {
            Path absolute = database.toAbsolutePath().normalize();
            Files.createDirectories(absolute.getParent());
            url = "jdbc:sqlite:" + absolute;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Não foi possível preparar a pasta do banco.", e);
        }
    }
    // The caller owns the connection and always closes it with try-with-resources.
    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try {
            try (var statement = connection.prepareStatement("PRAGMA foreign_keys = ON")) { statement.execute(); }
            try (var statement = connection.prepareStatement("PRAGMA busy_timeout = 5000")) { statement.execute(); }
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }
}
