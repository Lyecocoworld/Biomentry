package fr.lye.biomentry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.lye.biomentry.Helpers.MessageHelper;
import fr.lye.biomentry.Models.PlayerPreferences;

public class ParentCommand implements CommandExecutor, TabCompleter {

    private final Biomentry plugin;
    
    // Cache of completions to avoid recalculating them
    private static final List<String> MAIN_COMMANDS = Arrays.asList("reload", "debug", "version", "help", "toggle", "language");
    private static final List<String> DEBUG_OPTIONS = Arrays.asList("true", "false");
    private static final List<String> LANGUAGE_OPTIONS = Arrays.asList("fr", "en");

    public ParentCommand(Biomentry plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
                
            case "debug":
                return handleDebugCommand(sender, args);
                
            case "version":
                return handleVersionCommand(sender);
                
            case "toggle":
                return handleToggleCommand(sender);
                
            case "language":
                return handleLanguageCommand(sender, args);
                
            case "help":
                sendHelpMessage(sender);
                return true;
                
            default:
                sender.sendMessage(MessageHelper.FormatMessage("&6Unknown command. &eUse /biomentry help"));
                return true;
        }
    }

    private boolean handleToggleCommand(CommandSender sender) {
            if (!(sender instanceof Player)) {
            sender.sendMessage(MessageHelper.FormatMessage("&6This command can only be used by a player."));
            return true;
        }

        Player player = (Player) sender;
        PlayerPreferences.toggleNotifications(player.getUniqueId());
        boolean enabled = PlayerPreferences.areNotificationsEnabled(player.getUniqueId());
        
        sender.sendMessage(MessageHelper.FormatMessage(
            String.format("&eBiome notifications %s.", enabled ? "&6enabled" : "&6disabled")));
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("biomentry.reload")) {
            sender.sendMessage(MessageHelper.FormatMessage("&cYou do not have permission to use this command."));
            return true;
        }

        try {
            long startTime = System.currentTimeMillis();
            plugin.reloadPluginConfig();
            long reloadTime = System.currentTimeMillis() - startTime;
            
            sender.sendMessage(MessageHelper.FormatMessage(
                String.format("&eConfiguration successfully reloaded in &6%dms&e.", reloadTime)));
        } catch (Exception e) {
            sender.sendMessage(MessageHelper.FormatMessage("&6Error reloading configuration."));
            if (Biomentry.DEBUG_MODE) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("biomentry.debug")) {
            sender.sendMessage(MessageHelper.FormatMessage("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageHelper.FormatMessage("&cUsage: /biomentry debug <true|false>"));
            return true;
        }

        String debugValue = args[1].toLowerCase();
        if (!debugValue.equals("true") && !debugValue.equals("false")) {
            sender.sendMessage(MessageHelper.FormatMessage("&cUsage: /biomentry debug <true|false>"));
            return true;
        }

        Biomentry.DEBUG_MODE = Boolean.parseBoolean(debugValue);
            sender.sendMessage(MessageHelper.FormatMessage(
            String.format("&eDebug mode %s.", Biomentry.DEBUG_MODE ? "&6enabled" : "&6disabled")));
        return true;
    }

    private boolean handleVersionCommand(CommandSender sender) {
        sender.sendMessage(MessageHelper.FormatMessage(
            String.format("&6Biomentry &ev%s &fby Lye", Biomentry.getPluginVersion())));
        return true;
    }

    private boolean handleLanguageCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("biomentry.language")) {
            sender.sendMessage(MessageHelper.FormatMessage("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageHelper.FormatMessage("&eUsage: &f/biomentry language <fr|en>"));
            return true;
        }

        String language = args[1].toLowerCase();
        if (!LANGUAGE_OPTIONS.contains(language)) {
            sender.sendMessage(MessageHelper.FormatMessage("&6Unsupported language. &eUse 'fr' or 'en'."));
            return true;
        }

        // Update language in configuration
        plugin.getConfig().set("general.language", language);
        plugin.saveConfig();
        
        // Reload configuration to apply new language
        plugin.reloadPluginConfig();
        
            sender.sendMessage(MessageHelper.FormatMessage(
            String.format("&eLanguage set to: &6%s", language)));
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(MessageHelper.FormatMessage("&6=== Biomentry Help ==="));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry reload &f- Reload the configuration"));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry debug <true|false> &f- Enable/disable debug mode"));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry version &f- Show plugin version"));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry toggle &f- Enable/disable biome notifications"));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry language <fr|en> &f- Change plugin language"));
        sender.sendMessage(MessageHelper.FormatMessage("&e/biomentry help &f- Show this help"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Filter commands based on permissions
            String input = args[0].toLowerCase();
            
            for (String cmd : MAIN_COMMANDS) {
                if (cmd.startsWith(input)) {
                    switch (cmd) {
                        case "reload":
                            if (sender.hasPermission("biomentry.reload")) {
                                completions.add(cmd);
                            }
                            break;
                        case "debug":
                            if (sender.hasPermission("biomentry.debug")) {
                                completions.add(cmd);
                            }
                            break;
                        case "language":
                            if (sender.hasPermission("biomentry.language")) {
                                completions.add(cmd);
                            }
                            break;
                        default:
                            completions.add(cmd);
                    }
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("debug")) {
                // Auto-completion for debug command
                completions.addAll(DEBUG_OPTIONS.stream()
                    .filter(option -> option.startsWith(input))
                    .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("language")) {
                // Auto-completion for language command
                completions.addAll(LANGUAGE_OPTIONS.stream()
                    .filter(option -> option.startsWith(input))
                    .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
