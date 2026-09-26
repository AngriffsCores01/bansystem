package de.angriffscores.bansystem.service;

import de.angriffscores.bansystem.model.PlayerRecord;
import de.angriffscores.bansystem.repository.PlayerRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class PlayerService {

    private final @NonNull PlayerRepository playerRepository;

    /**
     * Stores or updates a player in the database.
     *
     * @param uuid player uuid
     * @param name player name
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> upsert(@NonNull UUID uuid, @NonNull String name) {
        return CompletableFuture.runAsync(() -> {
            try {
                this.playerRepository.upsert(uuid, name, Instant.now());
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to upsert player", exception);
            }
        });
    }

    /**
     * Resolves a target player by name for punishment commands.
     *
     * @param name player name
     * @return resolved player record if found
     */
    public @NonNull CompletableFuture<Optional<PlayerRecord>> resolveTarget(@NonNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) {
                try {
                    this.playerRepository.upsert(online.getUniqueId(), online.getName(), Instant.now());
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to upsert online player", exception);
                }
                return Optional.of(new PlayerRecord(online.getUniqueId(), online.getName(), Instant.now()));
            }

            try {
                Optional<PlayerRecord> fromDatabase = this.playerRepository.findByName(name);
                if (fromDatabase.isPresent()) {
                    return fromDatabase;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to lookup player", exception);
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (offlinePlayer.getName() != null && offlinePlayer.hasPlayedBefore()) {
                UUID uuid = offlinePlayer.getUniqueId();
                String resolvedName = offlinePlayer.getName();
                try {
                    this.playerRepository.upsert(uuid, resolvedName, Instant.now());
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to upsert offline player", exception);
                }
                return Optional.of(new PlayerRecord(uuid, resolvedName, Instant.now()));
            }
            return Optional.empty();
        });
    }
}
