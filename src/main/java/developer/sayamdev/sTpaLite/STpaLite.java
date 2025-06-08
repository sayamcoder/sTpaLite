package developer.sayamdev.sTpaLite;

import developer.sayamdev.sTpaLite.commands.*;
import developer.sayamdev.sTpaLite.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class STpaLite extends JavaPlugin {

    private TpaManager tpaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.tpaManager = new TpaManager(this);

        // Register Commands
        getCommand("tpa").setExecutor(new TpaCommand(this.tpaManager));
        getCommand("tpahere").setExecutor(new TpaHereCommand(this.tpaManager));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this.tpaManager));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this.tpaManager));
        getCommand("tpcancel").setExecutor(new TpCancelCommand(this.tpaManager));
        getCommand("tptoggle").setExecutor(new TpToggleCommand(this.tpaManager));
        getCommand("stpalite").setExecutor(new AdminCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this.tpaManager), this);

        getLogger().info("sTpaLite has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) {
            tpaManager.clearAllRequests();
        }
        getLogger().info("sTpaLite has been disabled.");
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }
}