package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.model.ActivePunishmentEntry;
import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.model.PunishmentType;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import de.angriffscores.bansystem.util.DurationParser;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public class BanListCommand extends AbstractPunishmentCommand {
    private static final int LIST_LIMIT = 50;

    public BanListCommand(
            @NonNull JavaPlugin plugin,
            @NonNull PlayerService playerService,
            @NonNull PunishmentService punishmentService,
            @NonNull MessageService messageService
    ) {
        super(plugin, playerService, punishmentService, messageService);
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String label,
            @NonNull String[] args
    ) {
        this.punishmentService.findActiveList(PunishmentType.BAN, LIST_LIMIT).whenComplete((entries, throwable) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (throwable != null) {
                        this.messageService.send(sender, "error-database");
                        this.plugin.getLogger().severe("Banlist failed: " + throwable.getMessage());
                        return;
                    }
                    this.sendList(sender, entries);
                }));
        return true;
    }

    private void sendList(@NonNull CommandSender sender, @NonNull List<ActivePunishmentEntry> entries) {
        this.messageService.send(sender, "banlist-header", "%count%", String.valueOf(entries.size()));
        if (entries.isEmpty()) {
            this.messageService.send(sender, "banlist-empty");
            return;
        }
        for (ActivePunishmentEntry entry : entries) {
            Punishment punishment = entry.getPunishment();
            String durationText;
            if (punishment.isPermanent()) {
                durationText = this.messageService.rawString("banlog-permanent");
            } else if (punishment.getExpiresAt() != null) {
                durationText = DurationParser.formatRemaining(punishment.getExpiresAt());
            } else {
                durationText = this.messageService.rawString("banlog-permanent");
            }
            this.messageService.send(
                    sender,
                    "banlist-entry",
                    "%id%", String.valueOf(punishment.getId()),
                    "%player%", entry.getTargetName(),
                    "%reason%", punishment.getReason(),
                    "%staff%", punishment.getStaffName(),
                    "%duration%", durationText
            );
        }
    }

    @Override
    public java.util.List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            @NonNull String[] args
    ) {
        return List.of();
    }
}
