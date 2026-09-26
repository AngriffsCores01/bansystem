package de.angriffscores.bansystem.repository;

import de.angriffscores.bansystem.database.Database;
import de.angriffscores.bansystem.model.PlayerRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class PlayerRepository {
    private final @NonNull Database database;

    /**
     * Inserts or updates a player record.
     *
     * @param uuid player uuid
     * @param name player name
     * @param lastSeen last seen timestamp
     * @throws SQLException if the query fails
     */
    public void upsert(@NonNull UUID uuid, @NonNull String name, @NonNull Instant lastSeen) throws SQLException {
        String sql = """
                INSERT INTO players (uuid, name, last_seen)
                VALUES (?, ?, ?)
                ON CONFLICT (uuid) DO UPDATE
                SET name = EXCLUDED.name,
                    last_seen = EXCLUDED.last_seen
                """;
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, name);
            statement.setTimestamp(3, Timestamp.from(lastSeen));
            statement.executeUpdate();
        }
    }

    /**
     * @param uuid player uuid
     * @return the player if present
     * @throws SQLException if the query fails
     */
    public @NonNull Optional<PlayerRecord> findByUuid(@NonNull UUID uuid) throws SQLException {
        String sql = "SELECT uuid, name, last_seen FROM players WHERE uuid = ?";
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    /**
     * @param name player name (case-insensitive)
     * @return the player if present
     * @throws SQLException if the query fails
     */
    public @NonNull Optional<PlayerRecord> findByName(@NonNull String name) throws SQLException {
        String sql = "SELECT uuid, name, last_seen FROM players WHERE LOWER(name) = LOWER(?)";
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    private @NonNull PlayerRecord map(@NonNull ResultSet resultSet) throws SQLException {
        UUID uuid = (UUID) resultSet.getObject("uuid");
        String name = resultSet.getString("name");
        Timestamp lastSeen = resultSet.getTimestamp("last_seen");
        Instant lastSeenInstant = null;
        if (lastSeen != null) {
            lastSeenInstant = lastSeen.toInstant();
        }
        return new PlayerRecord(uuid, name, lastSeenInstant);
    }
}
