package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.model.Punishment;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import de.angriffscores.bansystem.util.DurationParser;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public class CheckCommand extends AbstractPunishmentCommand {
    public CheckCommand(
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
            this.messageService.send(sender, "usage-check");
            return true;
        }
        String targetName = args[0];
        this.resolveTarget(sender, targetName, false, (commandSender, target) -> {
            this.punishmentService.findActiveBan(target.getUuid()).thenCombine(
                    this.punishmentService.findActiveMute(target.getUuid()),
                    (ban, mute) -> new ActiveCheck(ban, mute)
            ).whenComplete((result, throwable) -> Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    this.messageService.send(commandSender, "error-database");
                    this.plugin.getLogger().severe("Check failed: " + throwable.getMessage());
                    return;
                }
                this.messageService.send(commandSender, "check-header", "%player%", target.getName());
                this.sendActive(commandSender, "BAN", result.ban());
                this.sendActive(commandSender, "MUTE", result.mute());
                if (result.ban().isEmpty() && result.mute().isEmpty()) {
                    this.messageService.send(commandSender, "check-none", "%player%", target.getName());
                }
            }));
        });
        return true;
    }

    private void sendActive(
            @NonNull CommandSender sender,
            @NonNull String typeLabel,
            @NonNull Optional<Punishment> optionalPunishment
    ) {
        if (optionalPunishment.isEmpty()) {
            return;
        }
        Punishment punishment = optionalPunishment.get();
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
                "check-entry",
                "%type%", typeLabel,
                "%reason%", punishment.getReason(),
                "%staff%", punishment.getStaffName(),
                "%duration%", durationText,
                "%id%", String.valueOf(punishment.getId())
        );
    }

    private record ActiveCheck(@NonNull Optional<Punishment> ban, @NonNull Optional<Punishment> mute) {
    }
}
