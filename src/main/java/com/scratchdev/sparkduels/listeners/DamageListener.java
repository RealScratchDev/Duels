package com.scratchdev.sparkduels.listeners;

import com.scratchdev.sparkduels.Duels;
import com.scratchdev.sparkduels.data.ActiveDuel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {
    private final Duels plugin;

    public DamageListener(Duels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null) {
            if (player.getWorld().getName().equals("Duels")) {
                event.setCancelled(true);
            }
            return;
        }

        if (duel.isCountdownInProgress()) {
            event.setCancelled(true);
            return;
        }

        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (hasTotem(player)) {
                return;
            }

            event.setCancelled(true);
            player.setHealth(20.0);

            Player opponent = duel.getOpponent(player);
            plugin.getDuelManager().endDuel(duel, opponent, player);
        }
    }

    private boolean hasTotem(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
               player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
}
