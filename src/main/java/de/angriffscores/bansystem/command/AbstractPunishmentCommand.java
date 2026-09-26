package de.angriffscores.bansystem.command;

import de.angriffscores.bansystem.model.PlayerRecord;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
@RequiredArgsConstructor
public abstract class AbstractPunishmentCommand implements CommandExecutor, TabCompleter {

    protected final @NonNull JavaPlugin plugin;
    protected final @NonNull PlayerService playerService;
    protected final @NonNull PunishmentService punishmentService;
    protected final @NonNull MessageService messageService;

    /**
     * @param args command arguments
     * @param from inclusive start index
     * @return joined reason string
     */
    protected @NonNull String joinReason(@NonNull String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim();
    }

    /**
     * @param args command arguments
     * @param from inclusive start
     * @param toExclusive exclusive end
     * @return joined reason string
     */
    protected @NonNull String joinReason(@NonNull String[] args, int from, int toExclusive) {
        return String.join(" ", Arrays.copyOfRange(args, from, toExclusive)).trim();
    }

    /**
     * Resolves a target and runs the callback on the main thread after async lookup.
     *
     * @param sender command sender
     * @param name target name
     * @param checkBypass whether bypass permission should block the action
     * @param onResolved callback with sender and target
     */
    protected void resolveTarget(
            @NonNull CommandSender sender,
            @NonNull String name,
            boolean checkBypass,
            @NonNull BiConsumer<CommandSender, PlayerRecord> onResolved
    ) {
        this.playerService.resolveTarget(name).whenComplete((optionalTarget, throwable) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (throwable != null) {
                        this.messageService.send(sender, "error-database");
                        this.plugin.getLogger().severe("Player resolve failed: " + throwable.getMessage());
                        return;
                    }
                    if (optionalTarget.isEmpty()) {
                        this.messageService.send(sender, "player-not-found", "%player%", name);
                        return;
                    }
                    PlayerRecord target = optionalTarget.get();
                    if (checkBypass) {
                        Player online = Bukkit.getPlayer(target.getUuid());
                        if (online != null && online.hasPermission("bansystem.bypass")) {
                            this.messageService.send(sender, "bypass-target");
                            return;
                        }
                    }
                    onResolved.accept(sender, target);
                }));
    }

    /**
     * Runs an async punishment action and reports database errors.
     *
     * @param sender command sender
     * @param action async action
     */
    protected void runAction(@NonNull CommandSender sender, @NonNull CompletableFuture<Void> action) {
        action.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    this.messageService.send(sender, "error-database");
                    this.plugin.getLogger().severe("Punishment action failed: " + throwable.getMessage());
                });
            }
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            @NonNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();
            if (playerName.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                completions.add(playerName);
            }
        }
        return completions;
    }
}
