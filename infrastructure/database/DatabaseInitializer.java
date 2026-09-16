package com.fiap.bank.atm.infrastructure.database;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseInitializer {
    private DatabaseInitializer() { }
    public static void initialize(ConnectionFactory factory) {
        try (Connection connection = factory.open()) {
            connection.setAutoCommit(false);
            try {
                executeResource(connection, "/db/schema.sql");
                executeResource(connection, "/db/seed.sql");
                connection.commit();
            } catch (SQLException | IOException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Não foi possível inicializar o banco de dados.", e);
        }
    }
    private static void executeResource(Connection connection, String resource) throws IOException, SQLException {
        try (var input = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Recurso ausente: " + resource);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            // These bundled scripts have no semicolons inside string values.
            for (String command : sql.split(";")) {
                if (!command.isBlank()) {
                    try (var statement = connection.prepareStatement(command)) { statement.execute(); }
                }
            }
        }
    }
}
