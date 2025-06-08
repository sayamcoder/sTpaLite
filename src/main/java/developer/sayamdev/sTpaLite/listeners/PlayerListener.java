package developer.sayamdev.sTpaLite.listeners;

import developer.sayamdev.sTpaLite.TpaManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerListener implements Listener {
    private final TpaManager tpaManager;
    private final boolean cancelOnDamage;

    public PlayerListener(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
        this.cancelOnDamage = JavaPlugin.getPlugin(developer.sayamdev.sTpaLite.STpaLite.class)
                .getConfig().getBoolean("teleport.cancel-on-damage", true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            tpaManager.cancelWarmup(event.getPlayer(), "messages.teleport-cancelled-move");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!cancelOnDamage || event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        tpaManager.cancelWarmup(player, "messages.teleport-cancelled-damage");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tpaManager.cleanUpPlayer(event.getPlayer().getUniqueId());
    }
}
