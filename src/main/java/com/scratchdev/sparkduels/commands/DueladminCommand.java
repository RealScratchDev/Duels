package com.scratchdev.sparkduels.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.scratchdev.sparkduels.Duels;
import com.scratchdev.sparkduels.data.DuelMap;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@CommandAlias("dueladmin")
public class DueladminCommand extends BaseCommand {
    private final Duels plugin;

    public DueladminCommand(Duels plugin) {
        this.plugin = plugin;
    }

    @Subcommand("createkit")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("name @items")
    @Syntax("<name> <icon>")
    public void onCreateKit(Player player, String name, Material icon) {
        if (plugin.getKitManager().getKit(name) != null) {
            player.sendMessage(ChatColor.RED + "A kit with that name already exists!");
            return;
        }

        if (!icon.isItem()) {
            player.sendMessage(ChatColor.RED + "Invalid icon material!");
            return;
        }

        plugin.getKitManager().createKit(player, name, icon);
    }

    @Subcommand("createmap")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("name @items")
    @Syntax("<name> <icon>")
    public void onCreateMap(Player player, String name, Material icon) {
        if (plugin.getMapManager().getMap(name) != null) {
            player.sendMessage(ChatColor.RED + "A map with that name already exists!");
            return;
        }

        if (!icon.isItem()) {
            player.sendMessage(ChatColor.RED + "Invalid icon material!");
            return;
        }

        plugin.getMapManager().createMap(player, name, icon);
    }

    @Subcommand("deletekit")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("@kits")
    @Syntax("<name>")
    public void onDeleteKit(Player player, String name) {
        if (plugin.getKitManager().getKit(name) == null) {
            player.sendMessage(ChatColor.RED + "A kit with that name does not exist!");
            return;
        }

        plugin.getKitManager().deleteKit(name);
        player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' deleted successfully!");
    }

    @Subcommand("deletemap")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<name>")
    public void onDeleteMap(Player player, String name) {
        if (plugin.getMapManager().getMap(name) == null) {
            player.sendMessage(ChatColor.RED + "A map with that name does not exist!");
            return;
        }

        plugin.getMapManager().deleteMap(name);
        player.sendMessage(ChatColor.GREEN + "Map '" + name + "' deleted successfully!");
    }

    @Subcommand("listkits")
    @CommandPermission("spark.duels.admin")
    @Syntax("")
    public void onListKits(Player player) {
        var kits = plugin.getKitManager().getAllKits();

        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No kits available.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Available Kits ===");
        for (var kit : kits) {
            player.sendMessage(ChatColor.GREEN + "- " + kit.name());
        }
    }

    @Subcommand("listmaps")
    @CommandPermission("spark.duels.admin")
    @Syntax("")
    public void onListMaps(Player player) {
        var maps = plugin.getMapManager().getAllMaps();

        if (maps.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No maps available.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Available Maps ===");
        for (var map : maps) {
            player.sendMessage(ChatColor.GREEN + "- " + map.name());
        }
    }

    @Subcommand("setspawn1")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<map>")
    public void onSetSpawn1(Player player, String mapName) {
        DuelMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage(ChatColor.RED + "Map not found!");
            return;
        }

        Location loc = player.getLocation();
        BlockVector3 spawn = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (!isInside(spawn, map.min(), map.max())) {
            player.sendMessage(ChatColor.RED + "The spawn point must be inside the map's region!");
            return;
        }

        plugin.getMapManager().setSpawn1(mapName, spawn);
        player.sendMessage(ChatColor.GREEN + "Spawn 1 for map '" + mapName + "' set to your current location!");
    }

    @Subcommand("setspawn2")
    @CommandPermission("spark.duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<map>")
    public void onSetSpawn2(Player player, String mapName) {
        DuelMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage(ChatColor.RED + "Map not found!");
            return;
        }

        Location loc = player.getLocation();
        BlockVector3 spawn = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (!isInside(spawn, map.min(), map.max())) {
            player.sendMessage(ChatColor.RED + "The spawn point must be inside the map's region!");
            return;
        }

        plugin.getMapManager().setSpawn2(mapName, spawn);
        player.sendMessage(ChatColor.GREEN + "Spawn 2 for map '" + mapName + "' set to your current location!");
    }

    private boolean isInside(BlockVector3 pt, BlockVector3 min, BlockVector3 max) {
        return pt.getX() >= min.getX() && pt.getX() <= max.getX() &&
                pt.getY() >= min.getY() && pt.getY() <= max.getY() &&
                pt.getZ() >= min.getZ() && pt.getZ() <= max.getZ();
    }
}
