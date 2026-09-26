package de.angriffscores.bansystem.repository;

import de.angriffscores.bansystem.database.Database;
import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.model.PunishmentType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class PunishmentRepository {

    private final @NonNull Database database;

    /**
     * Deactivates all active punishments of the given type for a target.
     *
     * @param targetUuid target player
     * @param type punishment type
     * @throws SQLException if the query fails
     */
    public void deactivateActive(@NonNull UUID targetUuid, @NonNull PunishmentType type) throws SQLException {
        String sql = """
                UPDATE punishments
                SET active = FALSE
                WHERE target_uuid = ?
                  AND type = ?
                  AND active = TRUE
                """;
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            statement.setString(2, type.name());
            statement.executeUpdate();
        }
    }

    /**
     * Inserts a new punishment and returns it with generated id.
     *
     * @param punishment punishment without id
     * @return inserted punishment
     * @throws SQLException if the query fails
     */
    public @NonNull Punishment insert(@NonNull Punishment punishment) throws SQLException {
        String sql = """
                INSERT INTO punishments (
                    target_uuid, type, reason, staff_uuid, staff_name,
                    created_at, expires_at, active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
                RETURNING id
                """;
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, punishment.getTargetUuid());
            statement.setString(2, punishment.getType().name());
            statement.setString(3, punishment.getReason());
            statement.setObject(4, punishment.getStaffUuid());
            statement.setString(5, punishment.getStaffName());
            statement.setTimestamp(6, Timestamp.from(punishment.getCreatedAt()));
            if (punishment.getExpiresAt() == null) {
                statement.setNull(7, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(7, Timestamp.from(punishment.getExpiresAt()));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Failed to insert punishment");
                }
                punishment.setId(resultSet.getLong("id"));
                punishment.setActive(true);
                return punishment;
            }
        }
    }

    /**
     * Revokes the active punishment of a type for a target.
     *
     * @param targetUuid target player
     * @param type punishment type
     * @param revokedByUuid staff uuid
     * @param revokedByName staff name
     * @return true if a row was updated
     * @throws SQLException if the query fails
     */
    public boolean revokeActive(
            @NonNull UUID targetUuid,
            @NonNull PunishmentType type,
            @NonNull UUID revokedByUuid,
            @NonNull String revokedByName
    ) throws SQLException {
        String sql = """
                UPDATE punishments
                SET active = FALSE,
                    revoked_at = ?,
                    revoked_by_uuid = ?,
                    revoked_by_name = ?
                WHERE target_uuid = ?
                  AND type = ?
                  AND active = TRUE
                  AND (expires_at IS NULL OR expires_at > NOW())
                """;
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setObject(2, revokedByUuid);
            statement.setString(3, revokedByName);
            statement.setObject(4, targetUuid);
            statement.setString(5, type.name());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * @param targetUuid target player
     * @param type punishment type
     * @return currently active punishment if any
     * @throws SQLException if the query fails
     */
    public @NonNull Optional<Punishment> findActive(
            @NonNull UUID targetUuid,
            @NonNull PunishmentType type
    ) throws SQLException {
        String sql = """
                SELECT *
                FROM punishments
                WHERE target_uuid = ?
                  AND type = ?
                  AND active = TRUE
                  AND (expires_at IS NULL OR expires_at > NOW())
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    /**
     * @param targetUuid target player
     * @return all punishments newest first
     * @throws SQLException if the query fails
     */
    public @NonNull List<Punishment> findHistory(@NonNull UUID targetUuid) throws SQLException {
        String sql = """
                SELECT *
                FROM punishments
                WHERE target_uuid = ?
                ORDER BY created_at DESC
                """;
        List<Punishment> punishments = new ArrayList<>();
        try (Connection connection = this.database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, targetUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    punishments.add(this.map(resultSet));
                }
            }
        }
        return punishments;
    }

    /**
     * Marks expired active punishments as inactive.
     *
     * @throws SQLException if the query fails
     */
    public void deactivateExpired() throws SQLException {
        String sql = """
                UPDATE punishments
                SET active = FALSE
                WHERE active = TRUE
                  AND expires_at IS NOT NULL
                  AND expires_at <= NOW()
                """;
        try (Connection connection = this.database.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private @NonNull Punishment map(@NonNull ResultSet resultSet) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(resultSet.getLong("id"));
        punishment.setTargetUuid((UUID) resultSet.getObject("target_uuid"));
        punishment.setType(PunishmentType.valueOf(resultSet.getString("type")));
        punishment.setReason(resultSet.getString("reason"));
        punishment.setStaffUuid((UUID) resultSet.getObject("staff_uuid"));
        punishment.setStaffName(resultSet.getString("staff_name"));
        punishment.setCreatedAt(resultSet.getTimestamp("created_at").toInstant());
        Timestamp expiresAt = resultSet.getTimestamp("expires_at");
        if (expiresAt != null) {
            punishment.setExpiresAt(expiresAt.toInstant());
        }
        punishment.setActive(resultSet.getBoolean("active"));
        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        if (revokedAt != null) {
            punishment.setRevokedAt(revokedAt.toInstant());
        }
        Object revokedByUuid = resultSet.getObject("revoked_by_uuid");
        if (revokedByUuid != null) {
            punishment.setRevokedByUuid((UUID) revokedByUuid);
        }
        punishment.setRevokedByName(resultSet.getString("revoked_by_name"));
        return punishment;
    }
}
