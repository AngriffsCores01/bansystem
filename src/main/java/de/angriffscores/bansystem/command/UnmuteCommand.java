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
public class UnmuteCommand extends AbstractPunishmentCommand {
    public UnmuteCommand(
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
            this.messageService.send(sender, "usage-unmute");
            return true;
        }
        String targetName = args[0];
        this.resolveTarget(sender, targetName, false, (commandSender, target) ->
                this.runAction(commandSender, this.punishmentService.unmute(commandSender, target)));
        return true;
    }
}
