package de.angriffscores.bansystem.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public final class SchemaMigrator {

    private final @NonNull Database database;

    /**
     * Creates required tables if they do not exist.
     *
     * @throws SQLException if schema creation fails
     */
    public void migrate() throws SQLException {
        try (Connection connection = this.database.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid UUID PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        last_seen TIMESTAMPTZ
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_players_name_lower
                    ON players (LOWER(name))
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id BIGSERIAL PRIMARY KEY,
                        target_uuid UUID NOT NULL,
                        type VARCHAR(16) NOT NULL,
                        reason TEXT NOT NULL,
                        staff_uuid UUID NOT NULL,
                        staff_name VARCHAR(16) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL,
                        expires_at TIMESTAMPTZ,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        revoked_at TIMESTAMPTZ,
                        revoked_by_uuid UUID,
                        revoked_by_name VARCHAR(16)
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_target_type_active
                    ON punishments (target_uuid, type, active)
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_punishments_target_created
                    ON punishments (target_uuid, created_at DESC)
                    """);
        }
    }
}
