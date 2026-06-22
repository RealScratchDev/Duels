package com.scratchdev.sparkduels.gui;

import com.scratchdev.sparkduels.Duels;
import com.scratchdev.sparkduels.data.DuelMap;
import com.scratchdev.sparkduels.data.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MapSelectionGUI {
    private final Duels plugin;
    private final Player player;
    private final Player target;
    private final Kit kit;
    private final Inventory inventory;

    public MapSelectionGUI(Duels plugin, Player player, Player target, Kit kit) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.kit = kit;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Select a Map");

        setupInventory();
    }

    public void setupInventory() {
        inventory.clear();

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        List<DuelMap> maps = new ArrayList<>(plugin.getMapManager().getAllMaps());
        int count = Math.min(maps.size(), 7);
        int startSlot = 10 + (7 - count) / 2;

        for (int i = 0; i < count; i++) {
            DuelMap map = maps.get(i);
            ItemStack item = new ItemStack(map.icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + map.name());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to select");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inventory.setItem(startSlot + i, item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getTarget() {
        return target;
    }

    public Kit getKit() {
        return kit;
    }
}
