package de.angriffscores.bansystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public final class Database {
    @Getter
    private final @NonNull HikariDataSource dataSource;

    public Database(@NonNull FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 5432);
        String name = config.getString("database.name", "bansystem");
        String username = config.getString("database.username", "postgres");
        String password = config.getString("database.password", "postgres");
        int poolSize = config.getInt("database.pool-size", 10);

        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setPoolName("BanSystem-Pool");
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * @return a connection from the pool
     * @throws SQLException if a connection cannot be obtained
     */
    public @NonNull Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    /**
     * Closes the connection pool.
     */
    public void close() {
        if (!this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }
}
