package com.fiap.bank.atm.infrastructure.persistence;
import com.fiap.bank.atm.domain.model.*;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.database.ConnectionFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class AccountRepositoryJdbcImpl implements AccountRepository {
    private final ConnectionFactory factory;
    private static final String FIND_ID = "SELECT * FROM tb_account WHERE id = ?";
    private static final String FIND_NUMBER = "SELECT * FROM tb_account WHERE replace(number, '-', '') = ?";
    private static final String SAVE_ACCOUNT = """
        INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_limit, failed_attempts, version)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
        ON CONFLICT(id) DO UPDATE SET balance = excluded.balance, status = excluded.status,
            failed_attempts = excluded.failed_attempts, version = tb_account.version + 1
        WHERE tb_account.version = ?
        """;
    private static final String SAVE_TRANSACTION = """
        INSERT INTO tb_transaction (id, account_id, type, amount, created_at, description)
        VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING
        """;

    public AccountRepositoryJdbcImpl(ConnectionFactory factory) { this.factory = factory; }
    @Override public Optional<Account> findById(UUID id) { return find(FIND_ID, id.toString()); }
    @Override public Optional<Account> findByAccountNumber(String number) {
        return find(FIND_NUMBER, Objects.requireNonNull(number).replace("-", "").trim());
    }

    private Optional<Account> find(String sql, String value) {
        try (Connection connection = factory.open()) {
            // Keep account and statement in the same database snapshot.
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                Optional<Account> result;
                try (ResultSet rs = statement.executeQuery()) {
                    result = rs.next() ? Optional.of(mapAccount(connection, rs)) : Optional.empty();
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) { throw failure(e); }
    }

    private Account mapAccount(Connection connection, ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        return Account.restore(id, rs.getString("agency"), rs.getString("number"), rs.getString("pin"),
                Money.of(rs.getBigDecimal("balance")), Money.of(rs.getBigDecimal("daily_limit")),
                "BLOCKED".equals(rs.getString("status")), rs.getInt("failed_attempts"),
                rs.getLong("version"), history(connection, id));
    }

    private List<Transaction> history(Connection connection, UUID id) throws SQLException {
        List<Transaction> history = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tb_transaction WHERE account_id = ? ORDER BY created_at ASC, rowid ASC")) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    history.add(new Transaction(UUID.fromString(rs.getString("id")),
                            LocalDateTime.parse(rs.getString("created_at")),
                            TransactionType.valueOf(rs.getString("type")),
                            Money.of(rs.getBigDecimal("amount")), rs.getString("description")));
                }
            }
        }
        return history;
    }

    @Override public void save(Account account) { saveAll(List.of(account)); }

    @Override public void saveAll(List<Account> accounts) {
        Objects.requireNonNull(accounts);
        if (accounts.stream().map(Account::getId).distinct().count() != accounts.size()) {
            throw new IllegalArgumentException("Contas repetidas na mesma operação.");
        }
        try (Connection connection = factory.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement accountStatement = connection.prepareStatement(SAVE_ACCOUNT);
                 PreparedStatement txStatement = connection.prepareStatement(SAVE_TRANSACTION)) {
                for (Account account : accounts) {
                    accountStatement.setString(1, account.getId().toString());
                    accountStatement.setString(2, account.getAgency());
                    accountStatement.setString(3, account.getAccountNumber());
                    accountStatement.setBigDecimal(4, account.getBalance().getAmount());
                    accountStatement.setString(5, account.isBlocked() ? "BLOCKED" : "ACTIVE");
                    accountStatement.setString(6, account.getPin());
                    accountStatement.setBigDecimal(7, account.getDailyWithdrawalLimit().getAmount());
                    accountStatement.setInt(8, account.getFailedAttempts());
                    accountStatement.setLong(9, account.getVersion());
                    if (accountStatement.executeUpdate() != 1) {
                        throw new IllegalStateException("A conta foi alterada por outra sessão. Tente novamente.");
                    }
                    for (Transaction tx : account.getTransactions()) {
                        txStatement.setString(1, tx.getId().toString());
                        txStatement.setString(2, account.getId().toString());
                        txStatement.setString(3, tx.getType().name());
                        txStatement.setBigDecimal(4, tx.getAmount().getAmount());
                        txStatement.setString(5, tx.getTimestamp().toString());
                        txStatement.setString(6, tx.getDescription());
                        txStatement.addBatch();
                    }
                    txStatement.executeBatch();
                    txStatement.clearBatch();
                }
                connection.commit();
                accounts.forEach(Account::markPersisted);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) { throw failure(e); }
    }

    private IllegalStateException failure(SQLException e) {
        return new IllegalStateException("Falha ao acessar o banco de dados. Operação não concluída.", e);
    }
}
