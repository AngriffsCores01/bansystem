package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.model.StaffBanStat;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
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
public class BanStatsCommand extends AbstractPunishmentCommand {
    public BanStatsCommand(
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
        this.punishmentService.findBanStats().whenComplete((stats, throwable) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (throwable != null) {
                        this.messageService.send(sender, "error-database");
                        this.plugin.getLogger().severe("Banstats failed: " + throwable.getMessage());
                        return;
                    }
                    this.sendStats(sender, stats);
                }));
        return true;
    }

    private void sendStats(@NonNull CommandSender sender, @NonNull List<StaffBanStat> stats) {
        this.messageService.send(sender, "banstats-header");
        if (stats.isEmpty()) {
            this.messageService.send(sender, "banstats-empty");
            return;
        }
        int rank = 1;
        for (StaffBanStat stat : stats) {
            this.messageService.send(
                    sender,
                    "banstats-entry",
                    "%rank%", String.valueOf(rank),
                    "%staff%", stat.getStaffName(),
                    "%count%", String.valueOf(stat.getBanCount())
            );
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            @NonNull String[] args
    ) {
        return List.of();
    }
}
