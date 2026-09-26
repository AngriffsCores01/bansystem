package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public class BanCommand extends AbstractPunishmentCommand {
    public BanCommand(
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
        if (args.length < 2) {
            this.messageService.send(sender, "usage-ban");
            return true;
        }
        String targetName = args[0];
        String reason = this.joinReason(args, 1);
        if (reason.isEmpty()) {
            this.messageService.send(sender, "usage-ban");
            return true;
        }
        this.resolveTarget(sender, targetName, true, (commandSender, target) ->
                this.runAction(commandSender, this.punishmentService.ban(commandSender, target, reason, null)));
        return true;
    }
}
