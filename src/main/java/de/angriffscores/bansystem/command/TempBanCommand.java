package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import de.angriffscores.bansystem.util.DurationParser;
import java.time.Duration;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public class TempBanCommand extends AbstractPunishmentCommand {
    public TempBanCommand(
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
        if (args.length < 3) {
            this.messageService.send(sender, "usage-tempban");
            return true;
        }
        String targetName = args[0];
        String durationInput = args[args.length - 1];
        String reason = this.joinReason(args, 1, args.length - 1);
        if (reason.isEmpty()) {
            this.messageService.send(sender, "usage-tempban");
            return true;
        }
        Optional<Duration> duration = DurationParser.parse(durationInput);
        if (duration.isEmpty()) {
            this.messageService.send(sender, "invalid-duration", "%duration%", durationInput);
            return true;
        }
        this.resolveTarget(sender, targetName, true, (commandSender, target) ->
                this.runAction(
                        commandSender,
                        this.punishmentService.ban(commandSender, target, reason, duration.get())
                ));
        return true;
    }
}
