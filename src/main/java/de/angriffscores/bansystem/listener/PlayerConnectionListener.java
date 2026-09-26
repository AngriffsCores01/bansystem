package de.angriffscores.bansystem.listener;

import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import de.angriffscores.bansystem.util.DurationParser;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class PlayerConnectionListener implements Listener {

    private final @NonNull JavaPlugin plugin;
    private final @NonNull PlayerService playerService;
    private final @NonNull PunishmentService punishmentService;
    private final @NonNull MessageService messageService;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(@NonNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            CompletableFuture<Optional<Punishment>> future =
                    this.punishmentService.findActiveBan(event.getUniqueId());
            Optional<Punishment> ban = future.join();
            if (ban.isEmpty()) {
                return;
            }
            Punishment punishment = ban.get();
            if (punishment.isPermanent()) {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        this.messageService.raw(
                                "kick-ban",
                                "%reason%", punishment.getReason(),
                                "%staff%", punishment.getStaffName()
                        )
                );
            } else if (punishment.getExpiresAt() != null) {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        this.messageService.raw(
                                "kick-tempban",
                                "%reason%", punishment.getReason(),
                                "%duration%", DurationParser.formatRemaining(punishment.getExpiresAt()),
                                "%staff%", punishment.getStaffName()
                        )
                );
            }
        } catch (RuntimeException exception) {
            this.plugin.getLogger().severe("Failed to check ban on login: " + exception.getMessage());
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    this.messageService.raw("error-database")
            );
        }
    }

    @EventHandler
    public void onJoin(@NonNull PlayerJoinEvent event) {
        this.playerService.upsert(event.getPlayer().getUniqueId(), event.getPlayer().getName())
                .exceptionally(throwable -> {
                    this.plugin.getLogger().severe("Failed to upsert player: " + throwable.getMessage());
                    return null;
                });
        this.punishmentService.refreshMuteCache(event.getPlayer().getUniqueId())
                .exceptionally(throwable -> {
                    this.plugin.getLogger().severe("Failed to refresh mute cache: " + throwable.getMessage());
                    return null;
                });
    }
}
