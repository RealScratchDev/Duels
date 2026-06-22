package com.scratchdev.sparkduels.listeners;

import com.scratchdev.sparkduels.Duels;
import com.scratchdev.sparkduels.data.ActiveDuel;
import com.scratchdev.sparkduels.data.Kit;
import com.scratchdev.sparkduels.data.DuelMap;
import com.scratchdev.sparkduels.gui.KitSelectionGUI;
import com.scratchdev.sparkduels.gui.MapSelectionGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {
    private final Duels plugin;
    private final Map<UUID, KitSelectionGUI> kitSelectionGUIMap = new HashMap<>();
    private final Map<UUID, MapSelectionGUI> mapSelectionGUIMap = new HashMap<>();

    public DuelListener(Duels plugin) {
        this.plugin = plugin;
    }

    public void registerKitSelectionGUI(Player player, KitSelectionGUI gui) {
        kitSelectionGUIMap.put(player.getUniqueId(), gui);
    }

    public void registerMapSelectionGUI(Player player, MapSelectionGUI gui) {
        mapSelectionGUIMap.put(player.getUniqueId(), gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.AQUA + "Kit Preview:")) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(ChatColor.GOLD + "Select a Kit")) {
            event.setCancelled(true);

            if (event.getCurrentItem().getItemMeta() == null) return;

            String kitName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            Kit kit = plugin.getKitManager().getKit(kitName);

            if (kit == null) return;

            KitSelectionGUI gui = kitSelectionGUIMap.get(player.getUniqueId());
            if (gui == null) {
                player.closeInventory();
                return;
            }

            Player target = gui.getTarget();
            player.closeInventory();
            MapSelectionGUI mapGui = new MapSelectionGUI(plugin, player, target, kit);
            registerMapSelectionGUI(player, mapGui);
            mapGui.open();

        } else if (title.equals(ChatColor.GOLD + "Select a Map")) {
            event.setCancelled(true);

            if (event.getCurrentItem().getItemMeta() == null) return;

            String mapName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            DuelMap map = plugin.getMapManager().getMap(mapName);

            if (map == null) return;

            if (!map.isSpawnsSet()) {
                player.sendMessage(ChatColor.RED + "This map is not fully setup yet! (Spawns not set)");
                return;
            }

            MapSelectionGUI mapGui = mapSelectionGUIMap.get(player.getUniqueId());
            if (mapGui == null) {
                player.closeInventory();
                return;
            }

            Player target = mapGui.getTarget();
            Kit kit = mapGui.getKit();

            if (kit == null) {
                player.closeInventory();
                return;
            }

            player.closeInventory();
            mapSelectionGUIMap.remove(player.getUniqueId());

            plugin.getDuelManager().sendDuelRequest(player, target, kit, map);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player opponent = duel.getOpponent(player);
        plugin.getDuelManager().endDuel(duel, opponent, player);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        ActiveDuel duel = plugin.getDuelManager().getDuel(player);
        if (duel != null && duel.isCountdownInProgress()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null) return;

        if (duel.isCountdownInProgress()) {
            event.setCancelled(true);
            return;
        }

        if (!duel.isInArena(event.getTo())) {
            if (player.equals(duel.getPlayer1())) {
                player.teleport(duel.getSpawnPoints()[0]);
            } else if (player.equals(duel.getPlayer2())) {
                player.teleport(duel.getSpawnPoints()[1]);
            }
            player.sendMessage(ChatColor.RED + "You cannot leave the arena during a duel!");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDuelManager().isInDuel(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use commands during a duel!");
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDuelManager().isInDuel(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot change your gamemode during a duel!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean restored = plugin.getPlayerDataManager().restorePlayerData(player);
        if (!restored && player.getWorld().getName().equals("Duels")) {
            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            player.sendMessage(ChatColor.RED + "You were in a duel that ended. You have been returned to spawn.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel != null) {
            plugin.getPlayerDataManager().restorePlayerData(player, false);
            Player opponent = duel.getOpponent(player);
            plugin.getDuelManager().endDuel(duel, opponent, player);
            opponent.sendMessage(ChatColor.GREEN + "Your opponent disconnected. You win!");
            return;
        }

        kitSelectionGUIMap.remove(player.getUniqueId());
        mapSelectionGUIMap.remove(player.getUniqueId());
        plugin.getDuelManager().removePendingRequest(player);
    }
}
