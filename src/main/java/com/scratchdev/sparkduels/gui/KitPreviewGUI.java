package com.scratchdev.sparkduels.gui;

import com.scratchdev.sparkduels.data.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitPreviewGUI {
    private final Player player;
    private final Kit kit;
    private final Inventory inventory;

    public KitPreviewGUI(Player player, Kit kit) {
        this.player = player;
        this.kit = kit;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Kit Preview: " + kit.name());

        setupInventory();
    }

    private void setupInventory() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack[] armor = kit.armor();
        ItemStack offhand = kit.offhand();
        ItemStack[] contents = kit.contents();

        ItemStack separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName(" ");
        separator.setItemMeta(sepMeta);

        setItemWithLabel(1, armor[3], ChatColor.YELLOW + "Helmet");
        setItemWithLabel(2, armor[2], ChatColor.YELLOW + "Chestplate");
        setItemWithLabel(3, armor[1], ChatColor.YELLOW + "Leggings");
        setItemWithLabel(4, armor[0], ChatColor.YELLOW + "Boots");
        inventory.setItem(5, separator);
        setItemWithLabel(6, offhand, ChatColor.YELLOW + "Offhand");

        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                inventory.setItem(i + 18, contents[i]);
            }
        }
    }

    private void setItemWithLabel(int slot, ItemStack item, String label) {
        if (item == null || item.getType().isAir()) {
            ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_GRAY + "Empty");
            empty.setItemMeta(meta);
            inventory.setItem(slot, empty);
        } else {
            inventory.setItem(slot, item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Kit getKit() {
        return kit;
    }
}
