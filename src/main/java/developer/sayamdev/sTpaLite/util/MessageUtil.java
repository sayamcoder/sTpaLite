package developer.sayamdev.sTpaLite.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageUtil {
    private static FileConfiguration config;
    private static String prefix;
    private static boolean titlesEnabled;

    public static void init(JavaPlugin plugin) {
        config = plugin.getConfig();
        prefix = format(config.getString("messages.prefix", "&a[sTpaLite] &7"));
        titlesEnabled = config.getBoolean("titles.enabled", true);
    }

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(CommandSender sender, String path, String... replacements) {
        String message = config.getString(path, "&cMissing message: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(prefix + format(message));
    }

    public static void sendTitle(Player player, String path, String... replacements) {
        if (!titlesEnabled) return;

        String title = config.getString(path + ".title", "");
        String subtitle = config.getString(path + ".subtitle", "");

        for (int i = 0; i < replacements.length; i += 2) {
            title = title.replace(replacements[i], replacements[i + 1]);
            subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
        }

        player.sendTitle(format(title), format(subtitle), 10, 70, 20);
    }
}
