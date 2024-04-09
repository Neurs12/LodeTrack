package dev.neurs.lodetrack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LodeTrackCommands implements CommandExecutor {
    private final JavaPlugin plugin;
    private final LodeTrackPlayerEvents lodeEvents;

    private final Map<Player, BukkitRunnable> trackingPlayersTasks = new HashMap<>();
    private final Map<Player, Player> trackingPlayersQuery = new HashMap<>();

    LodeTrackCommands(LodeTrackPlayerEvents lodeEvents, JavaPlugin plugin) {
        this.plugin = plugin;
        this.lodeEvents = lodeEvents;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 0) {
                Player player = (Player) sender;
                PlayerInventory inventory = player.getInventory();

                ItemStack loadCompass = getLodeStack();

                inventory.addItem(loadCompass);

                return true;
            }
            else if (args[0].equals("accept")) {
                Player trackerPlayer = Bukkit.getPlayer(args[1]);
                Player trackedPlayer = (Player) sender;

                if (trackerPlayer == null) return true;

                String trackedPlayerFromRequest = this.lodeEvents.requestTrackTable.get(trackerPlayer.getName());
                if (trackedPlayerFromRequest != null) {
                    if (trackedPlayerFromRequest.equals(trackedPlayer.getName())) {
                        try {
                            this.trackingPlayersTasks.get(trackerPlayer).cancel();
                            this.trackingPlayersQuery.get(trackerPlayer).sendMessage(ChatColor.GREEN + trackedPlayer.getName() + ChatColor.WHITE + " is no longer tracking you.");
                        } catch (Exception ignored) { }

                        BukkitRunnable trackerTask = getBukkitRunnable(trackerPlayer, trackedPlayer);
                        this.trackingPlayersTasks.put(trackerPlayer, trackerTask);

                        if (trackerTask == null) return true;
                        trackerTask.runTaskTimerAsynchronously(this.plugin, 0L, 20L);

                        trackedPlayer.sendMessage("\nYou're now being tracked by " + ChatColor.GREEN + args[1]);
                        trackerPlayer.sendMessage(ChatColor.GREEN + trackedPlayer.getName() + ChatColor.WHITE + " has accepted your request, now pointing to their location!");

                        this.trackingPlayersQuery.put(trackerPlayer, trackedPlayer);

                        this.lodeEvents.requestTrackTable.remove(trackerPlayer.getName());

                        return true;
                    }
                }
            } else if (args[0].equals("cancel")) {
                Player trackerPlayer = Bukkit.getPlayer(args[1]);
                Player trackedPlayer = (Player) sender;

                Player trackedPlayerFromQuery = this.trackingPlayersQuery.get(trackerPlayer);
                if (trackedPlayerFromQuery != null) {
                    if (trackedPlayerFromQuery.equals(trackedPlayer)) {
                        try {
                            this.trackingPlayersTasks.get(trackerPlayer).cancel();
                        } catch (Exception ignored) { }
                        try {
                            this.trackingPlayersTasks.remove(trackerPlayer);
                        } catch (Exception ignored) { }
                        this.trackingPlayersQuery.remove(trackerPlayer);

                        trackedPlayer.sendMessage("\nYou have canceled " + ChatColor.GREEN + args[1] + ChatColor.WHITE + "'s tracking compass.");
                        if (trackerPlayer == null) return true;
                        trackerPlayer.sendMessage(ChatColor.GREEN + trackedPlayer.getName() + ChatColor.WHITE + " canceled your tracking.");

                        return true;
                    }
                }

                if (trackerPlayer == null) return true;
                String trackedPlayerFromRequest = this.lodeEvents.requestTrackTable.get(trackerPlayer.getName());
                if (trackedPlayerFromRequest != null) {
                    if (trackedPlayerFromRequest.equals(trackedPlayer.getName())) {
                        trackedPlayer.sendMessage("\nYou have canceled " + ChatColor.GREEN + args[1] + ChatColor.WHITE + "'s request.");
                        trackerPlayer.sendMessage(ChatColor.GREEN + trackedPlayer.getName() + ChatColor.WHITE + " canceled your request.");

                        this.lodeEvents.requestTrackTable.remove(trackerPlayer.getName());

                        return true;
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.RED + "Unknown request...");
        return true;
    }

    private static ItemStack getLodeStack() {
        ItemStack loadCompass = new ItemStack(Material.COMPASS);

        CompassMeta meta = (CompassMeta) loadCompass.getItemMeta();
        assert meta != null;

        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.RESET + "LodeTrack special compass that can track anyone!");
        meta.setDisplayName(ChatColor.RESET + "LodeTrack compass");
        meta.setLore(lore);

        meta.setLodestoneTracked(false);

        loadCompass.setItemMeta(meta);
        return loadCompass;
    }

    private static BukkitRunnable getBukkitRunnable(Player trackerPlayer, Player trackedPlayer) {
        ItemStack lodeTracking = trackerPlayer.getInventory().getItemInMainHand();
        CompassMeta lodeMeta = (CompassMeta) lodeTracking.getItemMeta();

        if (lodeMeta == null) return null;

        lodeMeta.setLodestoneTracked(false);
        lodeTracking.setItemMeta(lodeMeta);

        return new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedPlayer == null) {
                    trackerPlayer.sendMessage(ChatColor.RED + "Something went wrong... Compass is now pointing at the last succeeded location pull.");
                    cancel();
                    return;
                }
                if (trackerPlayer.isOnline() && trackedPlayer.isOnline()) {
                    if (trackerPlayer.getWorld().equals(trackedPlayer.getWorld())) {
                        lodeMeta.setLodestone(trackedPlayer.getLocation());
                        lodeTracking.setItemMeta(lodeMeta);
                    } else {
                        trackerPlayer.sendMessage(trackedPlayer.getName() + " is no longer in your dimension. Compass is now pointing at the portal's location.");
                        cancel();
                    }
                } else {
                    try {
                        trackerPlayer.sendMessage(ChatColor.GREEN + trackedPlayer.getName() + ChatColor.WHITE + " went offline. Compass is now pointing at their last online location.");
                    } catch (Exception ignored) { }
                    try {
                        trackedPlayer.sendMessage(ChatColor.GREEN + trackerPlayer.getName() + ChatColor.WHITE + " went offline. You are not being tracked by them.");
                    } catch (Exception ignored) { }
                    cancel();
                }
            }
        };
    }
}
