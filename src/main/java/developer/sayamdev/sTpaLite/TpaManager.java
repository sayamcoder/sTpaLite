package developer.sayamdev.sTpaLite;

import developer.sayamdev.sTpaLite.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final STpaLite plugin;

    // (Key: Target UUID, Value: Requester UUID)
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> tpaHereRequests = new ConcurrentHashMap<>();

    // (Key: Player UUID, Value: BukkitTask for their warmup)
    private final Map<UUID, BukkitTask> warmupTasks = new ConcurrentHashMap<>();
    private final Set<UUID> tpaDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TpaManager(STpaLite plugin) {
        this.plugin = plugin;
    }

    public void sendTpaRequest(Player requester, Player target) {
        if (handlePreRequestChecks(requester, target)) return;

        tpaRequests.put(target.getUniqueId(), requester.getUniqueId());

        MessageUtil.sendMessage(requester, "messages.request-sent", "{target}", target.getName());
        MessageUtil.sendMessage(target, "messages.request-received", "{player}", requester.getName());
        MessageUtil.sendTitle(target, "titles.request-received", "{player}", requester.getName());

        scheduleRequestTimeout(requester, target, tpaRequests);
    }

    public void sendTpaHereRequest(Player requester, Player target) {
        if (handlePreRequestChecks(requester, target)) return;

        tpaHereRequests.put(target.getUniqueId(), requester.getUniqueId());

        MessageUtil.sendMessage(requester, "messages.request-here-sent", "{target}", target.getName());
        MessageUtil.sendMessage(target, "messages.request-here-received", "{player}", requester.getName());
        MessageUtil.sendTitle(target, "titles.request-received", "{player}", requester.getName());

        scheduleRequestTimeout(requester, target, tpaHereRequests);
    }

    public void acceptRequest(Player target) {
        UUID tpaRequesterId = tpaRequests.remove(target.getUniqueId());
        if (tpaRequesterId != null) {
            Player requester = plugin.getServer().getPlayer(tpaRequesterId);
            if (requester == null) {
                MessageUtil.sendMessage(target, "messages.player-not-found");
                return;
            }
            startWarmup(requester, target.getLocation());
            notifyAccept(requester, target);
            return;
        }

        UUID tpaHereRequesterId = tpaHereRequests.remove(target.getUniqueId());
        if (tpaHereRequesterId != null) {
            Player requester = plugin.getServer().getPlayer(tpaHereRequesterId);
            if (requester == null) {
                MessageUtil.sendMessage(target, "messages.player-not-found");
                return;
            }
            startWarmup(target, requester.getLocation());
            notifyAccept(requester, target);
            return;
        }

        MessageUtil.sendMessage(target, "messages.no-pending-request");
    }

    public void denyRequest(Player target) {
        UUID tpaRequesterId = tpaRequests.remove(target.getUniqueId());
        if (tpaRequesterId != null) {
            notifyDeny(plugin.getServer().getPlayer(tpaRequesterId), target);
            return;
        }

        UUID tpaHereRequesterId = tpaHereRequests.remove(target.getUniqueId());
        if (tpaHereRequesterId != null) {
            notifyDeny(plugin.getServer().getPlayer(tpaHereRequesterId), target);
            return;
        }

        MessageUtil.sendMessage(target, "messages.no-pending-request");
    }

    public void cancelRequest(Player requester) {
        boolean requestCancelled = false;

        // Find and cancel /tpa request
        UUID targetIdTpa = tpaRequests.entrySet().stream()
                .filter(entry -> entry.getValue().equals(requester.getUniqueId()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (targetIdTpa != null) {
            tpaRequests.remove(targetIdTpa);
            Player target = plugin.getServer().getPlayer(targetIdTpa);
            if (target != null) {
                MessageUtil.sendMessage(requester, "messages.request-cancelled-sender", "{target}", target.getName());
                MessageUtil.sendMessage(target, "messages.request-cancelled-target", "{player}", requester.getName());
            }
            requestCancelled = true;
        }

        // Find and cancel /tpahere request
        UUID targetIdTpaHere = tpaHereRequests.entrySet().stream()
                .filter(entry -> entry.getValue().equals(requester.getUniqueId()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (targetIdTpaHere != null) {
            tpaHereRequests.remove(targetIdTpaHere);
            Player target = plugin.getServer().getPlayer(targetIdTpaHere);
            if (target != null) {
                MessageUtil.sendMessage(requester, "messages.request-cancelled-sender", "{target}", target.getName());
                MessageUtil.sendMessage(target, "messages.request-cancelled-target", "{player}", requester.getName());
            }
            requestCancelled = true;
        }

        if (!requestCancelled) {
            MessageUtil.sendMessage(requester, "messages.no-outgoing-request");
        }
    }

    public void startWarmup(Player player, Location destination) {
        int warmupTime = plugin.getConfig().getInt("teleport.warmup", 3);

        if (warmupTime <= 0 || player.hasPermission("stpalite.bypass.warmup")) {
            teleportPlayer(player, destination);
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private int countdown = warmupTime;
            private final Location startLocation = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (startLocation.getWorld().equals(player.getWorld()) && startLocation.distanceSquared(player.getLocation()) > 1.0) {
                    cancelWarmup(player, "messages.teleport-cancelled-move");
                    return;
                }

                if (countdown > 0) {
                    MessageUtil.sendTitle(player, "titles.warmup-countdown", "{time}", String.valueOf(countdown));
                    countdown--;
                } else {
                    teleportPlayer(player, destination);
                    warmupTasks.remove(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        warmupTasks.put(player.getUniqueId(), task);
    }

    public void cancelWarmup(Player player, String reasonPath) {
        if (warmupTasks.containsKey(player.getUniqueId())) {
            warmupTasks.get(player.getUniqueId()).cancel();
            warmupTasks.remove(player.getUniqueId());
            MessageUtil.sendMessage(player, reasonPath);
            MessageUtil.sendTitle(player, "titles.cancelled", "");
        }
    }

    private void teleportPlayer(Player player, Location destination) {
        // Unsafe location check
        if (plugin.getConfig().getBoolean("teleport.prevent-unsafe-teleport", true)) {
            Location safeLoc = findSafeLocation(destination);
            if (safeLoc == null) {
                MessageUtil.sendMessage(player, "&cTeleport failed: Destination is unsafe.");
                return;
            }
            destination = safeLoc;
        }

        player.teleport(destination);
        MessageUtil.sendTitle(player, "titles.success", "");
        setCooldown(player);
    }

    private void setCooldown(Player player) {
        if (player.hasPermission("stpalite.bypass.cooldown")) return;
        int cooldownTime = plugin.getConfig().getInt("teleport.cooldown", 30);
        if (cooldownTime > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownTime * 1000L));
        }
    }

    private boolean handlePreRequestChecks(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            MessageUtil.sendMessage(requester, "messages.cannot-request-self");
            return true;
        }

        if (tpaDisabled.contains(target.getUniqueId())) {
            MessageUtil.sendMessage(requester, "messages.target-toggled-off");
            return true;
        }

        long remainingCooldown = (cooldowns.getOrDefault(requester.getUniqueId(), 0L) - System.currentTimeMillis()) / 1000;
        if (remainingCooldown > 0 && !requester.hasPermission("stpalite.bypass.cooldown")) {
            MessageUtil.sendMessage(requester, "messages.cooldown", "{time}", String.valueOf(remainingCooldown));
            return true;
        }

        return false;
    }

    private void scheduleRequestTimeout(Player requester, Player target, Map<UUID, UUID> requestMap) {
        long timeout = plugin.getConfig().getLong("request-timeout", 60) * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (requestMap.get(target.getUniqueId()) == requester.getUniqueId()) {
                requestMap.remove(target.getUniqueId());
                MessageUtil.sendMessage(requester, "&cYour request to &e" + target.getName() + "&c has expired.");
                MessageUtil.sendMessage(target, "&cThe request from &e" + requester.getName() + "&c has expired.");
            }
        }, timeout);
    }

    public void toggleTpa(Player player) {
        if (tpaDisabled.contains(player.getUniqueId())) {
            tpaDisabled.remove(player.getUniqueId());
            MessageUtil.sendMessage(player, "messages.tpa-toggled-on");
        } else {
            tpaDisabled.add(player.getUniqueId());
            MessageUtil.sendMessage(player, "messages.tpa-toggled-off");
        }
    }

    public void cleanUpPlayer(UUID uuid) {
        tpaRequests.remove(uuid);
        tpaHereRequests.remove(uuid);
        tpaRequests.values().removeIf(v -> v.equals(uuid));
        tpaHereRequests.values().removeIf(v -> v.equals(uuid));
        if (warmupTasks.containsKey(uuid)) {
            warmupTasks.get(uuid).cancel();
            warmupTasks.remove(uuid);
        }
    }

    private void notifyAccept(Player requester, Player target) {
        int warmup = plugin.getConfig().getInt("teleport.warmup");
        MessageUtil.sendMessage(requester, "messages.request-accepted-sender", "{target}", target.getName(), "{time}", String.valueOf(warmup));
        MessageUtil.sendMessage(target, "messages.request-accepted-target", "{player}", requester.getName());
    }

    private void notifyDeny(Player requester, Player target) {
        if (requester != null) {
            MessageUtil.sendMessage(requester, "messages.request-denied-sender", "{target}", target.getName());
        }
        MessageUtil.sendMessage(target, "messages.request-denied-target", "{player}", requester != null ? requester.getName() : "Someone");
    }

    private Location findSafeLocation(Location loc) {
        Location checkLoc = loc.clone();
        for (int i = 0; i < 5; i++) { // Check up to 5 blocks up
            Location ground = checkLoc.clone();
            ground.setY(ground.getBlockY()); // Start at the integer Y level
            while (ground.getY() > loc.getWorld().getMinHeight() && !ground.getBlock().getType().isSolid()) {
                ground.subtract(0, 1, 0);
            }
            if (ground.getBlock().getType().isSolid()) {
                Location spot1 = ground.clone().add(0, 1, 0);
                Location spot2 = ground.clone().add(0, 2, 0);
                if (!spot1.getBlock().isLiquid() && !spot1.getBlock().getType().isSolid() &&
                        !spot2.getBlock().isLiquid() && !spot2.getBlock().getType().isSolid()) {
                    return spot1.add(0.5, 0, 0.5); // Center on the block
                }
            }
            checkLoc.add(0, 1, 0); // Move up and try again
        }
        return null; // No safe spot found
    }

    public void clearAllRequests() {
        tpaRequests.clear();
        tpaHereRequests.clear();
        warmupTasks.values().forEach(BukkitTask::cancel);
        warmupTasks.clear();
        cooldowns.clear();
        tpaDisabled.clear();
    }
}