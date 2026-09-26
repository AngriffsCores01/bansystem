package de.angriffscores.bansystem;

import de.angriffscores.bansystem.command.BanCommand;
import de.angriffscores.bansystem.command.BanListCommand;
import de.angriffscores.bansystem.command.BanLogCommand;
import de.angriffscores.bansystem.command.BanStatsCommand;
import de.angriffscores.bansystem.command.CheckCommand;
import de.angriffscores.bansystem.command.KickCommand;
import de.angriffscores.bansystem.command.MuteCommand;
import de.angriffscores.bansystem.command.MuteListCommand;
import de.angriffscores.bansystem.command.TempBanCommand;
import de.angriffscores.bansystem.command.TempMuteCommand;
import de.angriffscores.bansystem.command.UnbanCommand;
import de.angriffscores.bansystem.command.UnmuteCommand;
import de.angriffscores.bansystem.database.Database;
import de.angriffscores.bansystem.database.SchemaMigrator;
import de.angriffscores.bansystem.listener.ChatListener;
import de.angriffscores.bansystem.listener.PlayerConnectionListener;
import de.angriffscores.bansystem.repository.PlayerRepository;
import de.angriffscores.bansystem.repository.PunishmentRepository;
import de.angriffscores.bansystem.service.MessageService;
import de.angriffscores.bansystem.service.PlayerService;
import de.angriffscores.bansystem.service.PunishmentService;
import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public final class BanSystemPlugin extends JavaPlugin {
    private @Nullable Database database;
    private @Nullable PunishmentService punishmentService;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        File messagesFile = new File(this.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            this.saveResource("messages.yml", false);
        }

        try {
            this.database = new Database(this.getConfig());
            new SchemaMigrator(this.database).migrate();
        } catch (Exception exception) {
            this.getLogger().severe("Failed to connect to PostgreSQL: " + exception.getMessage());
            exception.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerRepository playerRepository = new PlayerRepository(this.database);
        PunishmentRepository punishmentRepository = new PunishmentRepository(this.database);
        String language = this.getConfig().getString("language", "de");
        MessageService messageService = new MessageService(this, language);
        PlayerService playerService = new PlayerService(playerRepository);
        this.punishmentService = new PunishmentService(this, punishmentRepository, messageService);

        this.registerCommand("ban", new BanCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("unban", new UnbanCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("mute", new MuteCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("unmute", new UnmuteCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand(
                "tempban",
                new TempBanCommand(this, playerService, this.punishmentService, messageService)
        );
        this.registerCommand(
                "tempmute",
                new TempMuteCommand(this, playerService, this.punishmentService, messageService)
        );
        this.registerCommand(
                "banlog",
                new BanLogCommand(this, playerService, this.punishmentService, messageService)
        );
        this.registerCommand("kick", new KickCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("check", new CheckCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("banlist", new BanListCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("mutelist", new MuteListCommand(this, playerService, this.punishmentService, messageService));
        this.registerCommand("banstats", new BanStatsCommand(this, playerService, this.punishmentService, messageService));

        this.getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, playerService, this.punishmentService, messageService),
                this
        );
        this.getServer().getPluginManager().registerEvents(
                new ChatListener(this.punishmentService, messageService),
                this
        );

        this.getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                () -> this.punishmentService.deactivateExpired().exceptionally(throwable -> {
                    this.getLogger().warning("Failed to deactivate expired punishments: " + throwable.getMessage());
                    return null;
                }),
                20L * 60L,
                20L * 60L
        );

        this.getLogger().info("BanSystem enabled.");
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            this.database.close();
        }
        this.getLogger().info("BanSystem disabled.");
    }

    private void registerCommand(@NonNull String name, @NonNull Object executor) {
        PluginCommand command = this.getCommand(name);
        if (command == null) {
            this.getLogger().severe("Command not found in plugin.yml: " + name);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor commandExecutor) {
            command.setExecutor(commandExecutor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }
}
