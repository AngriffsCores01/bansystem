package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.model.Punishment;
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
public class BanLogCommand extends AbstractPunishmentCommand {
    public BanLogCommand(
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
        if (args.length < 1) {
            this.messageService.send(sender, "usage-banlog");
            return true;
        }
        String targetName = args[0];
        this.resolveTarget(sender, targetName, false, (commandSender, target) ->
                this.punishmentService.findHistory(target.getUuid()).whenComplete((history, throwable) ->
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            if (throwable != null) {
                                this.messageService.send(commandSender, "error-database");
                                this.plugin.getLogger().severe("Banlog failed: " + throwable.getMessage());
                                return;
                            }
                            this.sendHistory(commandSender, target.getName(), history);
                        })));
        return true;
    }

    private void sendHistory(
            @NonNull CommandSender sender,
            @NonNull String playerName,
            @NonNull List<Punishment> history
    ) {
        this.messageService.send(sender, "banlog-header", "%player%", playerName);
        if (history.isEmpty()) {
            this.messageService.send(sender, "banlog-empty");
            return;
        }
        for (Punishment punishment : history) {
            String durationText;
            if (punishment.isPermanent()) {
                durationText = this.messageService.rawString("banlog-permanent");
            } else if (punishment.getExpiresAt() != null) {
                durationText = DurationParser.formatRemaining(punishment.getExpiresAt());
                if (!punishment.isCurrentlyActive()) {
                    durationText = DurationParser.format(
                            java.time.Duration.between(punishment.getCreatedAt(), punishment.getExpiresAt())
                    );
                }
            } else {
                durationText = this.messageService.rawString("banlog-permanent");
            }

            String status;
            if (punishment.getRevokedAt() != null) {
                status = this.messageService.rawString("banlog-status-revoked");
            } else if (punishment.isCurrentlyActive()) {
                status = this.messageService.rawString("banlog-status-active");
            } else {
                status = this.messageService.rawString("banlog-status-expired");
            }

            this.messageService.send(
                    sender,
                    "banlog-entry",
                    "%id%", String.valueOf(punishment.getId()),
                    "%type%", punishment.getType().name(),
                    "%reason%", punishment.getReason(),
                    "%staff%", punishment.getStaffName(),
                    "%duration%", durationText,
                    "%status%", status
            );
        }
    }
}
