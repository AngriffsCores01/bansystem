package de.angriffscores.bansystem.service;

import de.angriffscores.bansystem.model.ActivePunishmentEntry;
import de.angriffscores.bansystem.model.PlayerRecord;
import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.model.PunishmentType;
import de.angriffscores.bansystem.model.StaffBanStat;
import de.angriffscores.bansystem.repository.PunishmentRepository;
import de.angriffscores.bansystem.util.DurationParser;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class PunishmentService {
    private final @NonNull JavaPlugin plugin;
    private final @NonNull PunishmentRepository punishmentRepository;
    private final @NonNull MessageService messageService;
    private final @NonNull ConcurrentHashMap<UUID, Punishment> muteCache = new ConcurrentHashMap<>();

    /**
     * @param targetUuid player uuid
     * @return cached active mute if present
     */
    public @NonNull Optional<Punishment> getCachedMute(@NonNull UUID targetUuid) {
        Punishment punishment = this.muteCache.get(targetUuid);
        if (punishment == null) {
            return Optional.empty();
        }
        if (!punishment.isCurrentlyActive()) {
            this.muteCache.remove(targetUuid);
            return Optional.empty();
        }
        return Optional.of(punishment);
    }

    /**
     * @param targetUuid player uuid
     */
    public void clearMuteCache(@NonNull UUID targetUuid) {
        this.muteCache.remove(targetUuid);
    }

    /**
     * Reloads the mute cache entry for a player from the database.
     *
     * @param targetUuid player uuid
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> refreshMuteCache(@NonNull UUID targetUuid) {
        return this.findActiveMute(targetUuid).thenAccept(optionalMute -> {
            if (optionalMute.isPresent()) {
                this.muteCache.put(targetUuid, optionalMute.get());
            } else {
                this.muteCache.remove(targetUuid);
            }
        });
    }

    /**
     * Applies a permanent or temporary ban.
     *
     * @param sender command sender
     * @param target target player
     * @param reason punishment reason
     * @param duration null for permanent
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> ban(
            @NonNull CommandSender sender,
            @NonNull PlayerRecord target,
            @NonNull String reason,
            @Nullable Duration duration
    ) {
        return this.punish(sender, target, PunishmentType.BAN, reason, duration);
    }

    /**
     * Applies a permanent or temporary mute.
     *
     * @param sender command sender
     * @param target target player
     * @param reason punishment reason
     * @param duration null for permanent
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> mute(
            @NonNull CommandSender sender,
            @NonNull PlayerRecord target,
            @NonNull String reason,
            @Nullable Duration duration
    ) {
        return this.punish(sender, target, PunishmentType.MUTE, reason, duration);
    }

    /**
     * Revokes an active ban.
     *
     * @param sender command sender
     * @param target target player
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> unban(@NonNull CommandSender sender, @NonNull PlayerRecord target) {
        return this.revoke(sender, target, PunishmentType.BAN);
    }

    /**
     * Revokes an active mute.
     *
     * @param sender command sender
     * @param target target player
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> unmute(@NonNull CommandSender sender, @NonNull PlayerRecord target) {
        return this.revoke(sender, target, PunishmentType.MUTE);
    }

    /**
     * @param targetUuid target uuid
     * @return active ban if present
     */
    public @NonNull CompletableFuture<Optional<Punishment>> findActiveBan(@NonNull UUID targetUuid) {
        return this.findActive(targetUuid, PunishmentType.BAN);
    }

    /**
     * @param targetUuid target uuid
     * @return active mute if present
     */
    public @NonNull CompletableFuture<Optional<Punishment>> findActiveMute(@NonNull UUID targetUuid) {
        return this.findActive(targetUuid, PunishmentType.MUTE);
    }

    /**
     * @param targetUuid target uuid
     * @return full punishment history
     */
    public @NonNull CompletableFuture<List<Punishment>> findHistory(@NonNull UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.punishmentRepository.deactivateExpired();
                return this.punishmentRepository.findHistory(targetUuid);
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load banlog", exception);
            }
        });
    }

    /**
     * @param type punishment type
     * @param limit max entries
     * @return active punishments of the given type
     */
    public @NonNull CompletableFuture<List<ActivePunishmentEntry>> findActiveList(
            @NonNull PunishmentType type,
            int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.punishmentRepository.deactivateExpired();
                return this.punishmentRepository.findActiveByType(type, limit);
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load active punishments", exception);
            }
        });
    }

    /**
     * @return ban statistics per staff member
     */
    public @NonNull CompletableFuture<List<StaffBanStat>> findBanStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.punishmentRepository.findBanStatsByStaff();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load ban stats", exception);
            }
        });
    }

    /**
     * Deactivates expired punishments.
     *
     * @return completion future
     */
    public @NonNull CompletableFuture<Void> deactivateExpired() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.punishmentRepository.deactivateExpired();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to deactivate expired punishments", exception);
            }
        });
    }

    private @NonNull CompletableFuture<Optional<Punishment>> findActive(
            @NonNull UUID targetUuid,
            @NonNull PunishmentType type
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.punishmentRepository.deactivateExpired();
                return this.punishmentRepository.findActive(targetUuid, type);
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to lookup active punishment", exception);
            }
        });
    }

    private @NonNull CompletableFuture<Void> punish(
            @NonNull CommandSender sender,
            @NonNull PlayerRecord target,
            @NonNull PunishmentType type,
            @NonNull String reason,
            @Nullable Duration duration
    ) {
        UUID staffUuid = this.resolveStaffUuid(sender);
        String staffName = sender.getName();
        Instant expiresAt = null;
        if (duration != null) {
            expiresAt = Instant.now().plus(duration);
        }
        Instant finalExpiresAt = expiresAt;
        String finalDurationText;
        if (duration == null) {
            finalDurationText = this.messageService.rawString("banlog-permanent");
        } else {
            finalDurationText = DurationParser.format(duration);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                this.punishmentRepository.deactivateActive(target.getUuid(), type);
                Punishment punishment = new Punishment();
                punishment.setTargetUuid(target.getUuid());
                punishment.setType(type);
                punishment.setReason(reason);
                punishment.setStaffUuid(staffUuid);
                punishment.setStaffName(staffName);
                punishment.setCreatedAt(Instant.now());
                punishment.setExpiresAt(finalExpiresAt);
                Punishment inserted = this.punishmentRepository.insert(punishment);
                if (type == PunishmentType.MUTE) {
                    this.muteCache.put(target.getUuid(), inserted);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to apply punishment", exception);
            }
        }).thenRun(() -> Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (type == PunishmentType.BAN) {
                Player online = Bukkit.getPlayer(target.getUuid());
                if (online != null) {
                    if (duration == null) {
                        online.kick(this.messageService.raw(
                                "kick-ban",
                                "%reason%", reason,
                                "%staff%", staffName
                        ));
                    } else {
                        online.kick(this.messageService.raw(
                                "kick-tempban",
                                "%reason%", reason,
                                "%duration%", finalDurationText,
                                "%staff%", staffName
                        ));
                    }
                }
                if (duration == null) {
                    this.messageService.send(sender, "ban-success", "%player%", target.getName(), "%reason%", reason);
                    this.notifyStaff("notify-ban", sender, target.getName(), reason, null);
                } else {
                    this.messageService.send(
                            sender,
                            "tempban-success",
                            "%player%", target.getName(),
                            "%reason%", reason,
                            "%duration%", finalDurationText
                    );
                    this.notifyStaff("notify-tempban", sender, target.getName(), reason, finalDurationText);
                }
            } else {
                if (duration == null) {
                    this.messageService.send(sender, "mute-success", "%player%", target.getName(), "%reason%", reason);
                    this.notifyStaff("notify-mute", sender, target.getName(), reason, null);
                } else {
                    this.messageService.send(
                            sender,
                            "tempmute-success",
                            "%player%", target.getName(),
                            "%reason%", reason,
                            "%duration%", finalDurationText
                    );
                    this.notifyStaff("notify-tempmute", sender, target.getName(), reason, finalDurationText);
                }
            }
        }));
    }

    private @NonNull CompletableFuture<Void> revoke(
            @NonNull CommandSender sender,
            @NonNull PlayerRecord target,
            @NonNull PunishmentType type
    ) {
        UUID staffUuid = this.resolveStaffUuid(sender);
        String staffName = sender.getName();
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.punishmentRepository.deactivateExpired();
                boolean revoked = this.punishmentRepository.revokeActive(
                        target.getUuid(),
                        type,
                        staffUuid,
                        staffName
                );
                if (revoked && type == PunishmentType.MUTE) {
                    this.muteCache.remove(target.getUuid());
                }
                return revoked;
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to revoke punishment", exception);
            }
        }).thenAccept(revoked -> Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!revoked) {
                if (type == PunishmentType.BAN) {
                    this.messageService.send(sender, "unban-not-banned", "%player%", target.getName());
                } else {
                    this.messageService.send(sender, "unmute-not-muted", "%player%", target.getName());
                }
                return;
            }
            if (type == PunishmentType.BAN) {
                this.messageService.send(sender, "unban-success", "%player%", target.getName());
                this.notifyStaff("notify-unban", sender, target.getName(), null, null);
            } else {
                this.messageService.send(sender, "unmute-success", "%player%", target.getName());
                this.notifyStaff("notify-unmute", sender, target.getName(), null, null);
            }
        }));
    }

    /**
     * Kicks an online player and notifies staff with {@code bansystem.notify}.
     *
     * @param sender command sender
     * @param target online target
     * @param reason kick reason
     */
    public void kick(@NonNull CommandSender sender, @NonNull Player target, @NonNull String reason) {
        target.kick(this.messageService.raw(
                "kick-player",
                "%reason%", reason,
                "%staff%", sender.getName()
        ));
        this.messageService.send(sender, "kick-success", "%player%", target.getName(), "%reason%", reason);
        this.notifyStaff("notify-kick", sender, target.getName(), reason, null);
    }

    /**
     * Sends an alert to every online player with {@code bansystem.notify},
     * excluding the acting staff member so they only see their success message.
     *
     * @param messageKey message key
     * @param actor acting staff
     * @param targetName target name
     * @param reason reason or null
     * @param duration duration or null
     */
    public void notifyStaff(
            @NonNull String messageKey,
            @NonNull CommandSender actor,
            @NonNull String targetName,
            @Nullable String reason,
            @Nullable String duration
    ) {
        UUID actorUuid = null;
        if (actor instanceof Player actorPlayer) {
            actorUuid = actorPlayer.getUniqueId();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("bansystem.notify")) {
                continue;
            }
            if (actorUuid != null && player.getUniqueId().equals(actorUuid)) {
                continue;
            }
            if (reason != null && duration != null) {
                this.messageService.send(
                        player,
                        messageKey,
                        "%staff%", actor.getName(),
                        "%player%", targetName,
                        "%reason%", reason,
                        "%duration%", duration
                );
            } else if (reason != null) {
                this.messageService.send(
                        player,
                        messageKey,
                        "%staff%", actor.getName(),
                        "%player%", targetName,
                        "%reason%", reason
                );
            } else {
                this.messageService.send(
                        player,
                        messageKey,
                        "%staff%", actor.getName(),
                        "%player%", targetName
                );
            }
        }
    }

    private @NonNull UUID resolveStaffUuid(@NonNull CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId();
        }
        return new UUID(0L, 0L);
    }
}
