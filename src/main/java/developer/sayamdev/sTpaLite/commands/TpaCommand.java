package developer.sayamdev.sTpaLite.commands;

import developer.sayamdev.sTpaLite.TpaManager;
import developer.sayamdev.sTpaLite.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "messages.not-a-player");
            return true;
        }

        if (args.length == 0) {
            return false; // Shows the usage message from plugin.yml
        }

        Player requester = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            MessageUtil.sendMessage(requester, "messages.player-not-found");
            return true;
        }

        tpaManager.sendTpaRequest(requester, target);
        return true;
    }
}