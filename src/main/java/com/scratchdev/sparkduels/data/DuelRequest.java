package com.scratchdev.sparkduels.data;

import org.bukkit.entity.Player;

public record DuelRequest(Player sender, Player target, Kit kit, DuelMap map) {
}
