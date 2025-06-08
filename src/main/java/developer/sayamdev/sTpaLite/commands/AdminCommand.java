package developer.sayamdev.sTpaLite.commands;

import developer.sayamdev.sTpaLite.STpaLite;
import developer.sayamdev.sTpaLite.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final STpaLite plugin;

    public AdminCommand(STpaLite plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stpalite.admin.reload")) {
            // Silently fail for non-admins to not reveal the command existence
            MessageUtil.sendMessage(sender, "&cUnknown command. Type \"/help\" for help.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            MessageUtil.init(plugin); // Re-initialize the MessageUtil with the new config values
            plugin.getTpaManager().clearAllRequests(); // Clear active requests to prevent issues
            MessageUtil.sendMessage(sender, "messages.reload-success");
        } else {
            sender.sendMessage(MessageUtil.format("&a[sTpaLite] &7Version " + plugin.getDescription().getVersion()));
            sender.sendMessage(MessageUtil.format("&7Usage: /stpalite reload"));
        }
        return true;
    }
}