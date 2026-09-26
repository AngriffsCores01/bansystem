package de.angriffscores.bansystem.listener;

import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PunishmentService;
import de.angriffscores.bansystem.util.DurationParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final @NonNull PunishmentService punishmentService;
    private final @NonNull MessageService messageService;

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NonNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("bansystem.bypass")) {
            return;
        }
        Optional<Punishment> mute = this.punishmentService.getCachedMute(player.getUniqueId());
        if (mute.isEmpty()) {
            return;
        }
        Punishment punishment = mute.get();
        if (!punishment.isCurrentlyActive()) {
            this.punishmentService.clearMuteCache(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        if (punishment.isPermanent()) {
            this.messageService.send(player, "mute-chat", "%reason%", punishment.getReason());
        } else if (punishment.getExpiresAt() != null) {
            this.messageService.send(
                    player,
                    "mute-chat-temp",
                    "%reason%", punishment.getReason(),
                    "%duration%", DurationParser.formatRemaining(punishment.getExpiresAt())
            );
        }
    }
}
