package developer.sayamdev.sTpaLite.commands;

import developer.sayamdev.sTpaLite.TpaManager;
import developer.sayamdev.sTpaLite.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpToggleCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpToggleCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "messages.not-a-player");
            return true;
        }

        Player player = (Player) sender;
        tpaManager.toggleTpa(player);
        return true;
    }
}

