package dev.neurs.lodetrack;

import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LodeTrackPlayerEvents implements Listener {

    public final Map<String, String> requestTrackTable = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            ItemStack item = event.getItem();
            if (item == null) return;
            if (item.getType() == Material.COMPASS) {
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                if (meta == null) return;
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore == null) return;
                    if (lore.contains("LodeTrack special compass that can track anyone!")) {
                        Player player = event.getPlayer();

                        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

                        int rows = (int) Math.ceil((onlinePlayers.size() - 1) / 9.0);
                        if (rows < 1) rows = 1;

                        Inventory inventory = Bukkit.createInventory(player, rows * 9, "Choose a player");

                        for (Player onlinePlayer : onlinePlayers) {
                            if (player == onlinePlayer) {
                                continue;
                            }
                            ItemStack skullItem = createSkullItem(onlinePlayer);
                            inventory.addItem(skullItem);
                        }

                        player.openInventory(inventory);
                    }
                }
            }
        }
    }

    private ItemStack createSkullItem(Player player) {
        ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) skullItem.getItemMeta();
        assert meta != null;
        meta.setOwningPlayer(player);
        meta.setDisplayName(player.getName());

        skullItem.setItemMeta(meta);
        return skullItem;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Choose a player")) {
            event.setCancelled(true);

            ItemStack skull = event.getCurrentItem();

            if (skull == null || skull.getType() != Material.PLAYER_HEAD) return;

            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta == null) return;

            Player player = (Player) event.getWhoClicked();
            Player trackingPlayer = Bukkit.getPlayer(skullMeta.getDisplayName());

//            try {
//                trackingPlayers.get(player).cancel();
//            } catch (Exception ignored) { }
//
//            BukkitRunnable tracker = getBukkitRunnable(player, trackingPlayer);
//            if (tracker == null) return;
//            tracker.runTaskTimerAsynchronously(this.plugin, 0L, 20L);
//            trackingPlayers.put(player, tracker);

            if (trackingPlayer == null) {
                player.sendMessage(ChatColor.RED + "The player you're trying to track does not exist.");
                return;
            }

            if (!trackingPlayer.isOnline()) {
                player.sendMessage(ChatColor.RED + "The player you're trying to track does not appears to be online.");
                return;
            }

            if (!player.getWorld().equals(trackingPlayer.getWorld())) {
                player.sendMessage(ChatColor.RED + "You're not in the same dimension with " + ChatColor.GREEN + trackingPlayer.getName());
                return;
            }

            requestTrackTable.put(player.getName(), trackingPlayer.getName());

            TextComponent acceptRequest = new TextComponent(ChatColor.GREEN + "" + ChatColor.UNDERLINE + ChatColor.BOLD + "ACCEPT");
            acceptRequest.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Accept tracking request from " + player.getName())));
            acceptRequest.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lodetrack accept " + player.getName()));

            TextComponent denyRequest = new TextComponent(ChatColor.RED + "" + ChatColor.UNDERLINE + ChatColor.BOLD + "DENY");
            denyRequest.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Deny tracking request from " + player.getName())));
            denyRequest.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lodetrack cancel " + player.getName()));

            ComponentBuilder requestMessage = new ComponentBuilder();
            requestMessage.append(ChatColor.GREEN + player.getName() + ChatColor.WHITE + " is requesting you to track your location.\n\n");
            requestMessage.append(acceptRequest);
            requestMessage.append("  ");
            requestMessage.append(denyRequest);

            trackingPlayer.spigot().sendMessage(requestMessage.create());

            player.sendMessage("Tracking request sent to " + ChatColor.GREEN + trackingPlayer.getName() + ChatColor.WHITE + "! Waiting for response...");

            player.closeInventory();
        }
    }
}
