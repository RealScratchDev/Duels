package com.scratchdev.sparkduels.managers;

import com.scratchdev.sparkduels.Duels;
import com.scratchdev.sparkduels.data.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.*;

public class DuelManager {
    private final Duels plugin;
    private final Map<UUID, DuelRequest> pendingRequests;
    private final Map<UUID, ActiveDuel> activeDuels;

    public DuelManager(Duels plugin) {
        this.plugin = plugin;
        this.pendingRequests = new HashMap<>();
        this.activeDuels = new HashMap<>();
    }

    public void sendDuelRequest(Player sender, Player target, Kit kit, DuelMap map) {
        DuelRequest request = new DuelRequest(sender, target, kit, map);
        pendingRequests.put(target.getUniqueId(), request);

        sender.sendMessage(ChatColor.GREEN + "Duel request sent to " + target.getName() + "!");

        TextComponent message = new TextComponent(ChatColor.GOLD + sender.getName() + " has challenged you to a duel!");

        TextComponent acceptButton = new TextComponent(ChatColor.GREEN + " [ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to accept").create()));

        TextComponent denyButton = new TextComponent(ChatColor.RED + " [DENY]");
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel deny " + sender.getName()));
        denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to deny").create()));

        message.addExtra(acceptButton);
        message.addExtra(denyButton);
        target.spigot().sendMessage(message);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(target.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    if (sender.isOnline()) {
                        sender.sendMessage(ChatColor.RED + "Duel request to " + target.getName() + " expired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage(ChatColor.RED + "Duel request from " + sender.getName() + " expired.");
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    public void acceptDuel(DuelRequest request) {
        Player sender = request.sender();
        Player target = request.target();

        pendingRequests.remove(target.getUniqueId());
        startDuel(sender, target, request.kit(), request.map());
    }

    public void denyDuel(DuelRequest request) {
        pendingRequests.remove(request.target().getUniqueId());
        request.sender().sendMessage(ChatColor.RED + request.target().getName() + " denied your duel request.");
        request.target().sendMessage(ChatColor.RED + "You denied the duel request from " + request.sender().getName() + ".");
    }

    private void startDuel(Player player1, Player player2, Kit kit, DuelMap map) {
        if (!map.isSpawnsSet()) {
            player1.sendMessage(ChatColor.RED + "This map is not fully setup yet!");
            player2.sendMessage(ChatColor.RED + "This map is not fully setup yet!");
            return;
        }

        plugin.getPlayerDataManager().savePlayerData(player1);
        plugin.getPlayerDataManager().savePlayerData(player2);

        PregeneratedArena arena = plugin.getMapManager().getAvailableArena(map);

        if (arena == null) {
            player1.sendMessage(ChatColor.RED + "Failed to create arena!");
            player2.sendMessage(ChatColor.RED + "Failed to create arena!");
            plugin.getPlayerDataManager().restorePlayerData(player1);
            plugin.getPlayerDataManager().restorePlayerData(player2);
            return;
        }

        String arenaId = arena.getArenaId();
        Location[] spawnPoints = arena.getSpawnPoints();

        ActiveDuel duel = new ActiveDuel(player1, player2, kit, map, arenaId, spawnPoints);
        activeDuels.put(player1.getUniqueId(), duel);
        activeDuels.put(player2.getUniqueId(), duel);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location loc1 = spawnPoints[0].clone();
                Location loc2 = spawnPoints[1].clone();
                loc1.setDirection(loc2.toVector().subtract(loc1.toVector()));
                loc2.setDirection(loc1.toVector().subtract(loc2.toVector()));

                CompletableFuture<Boolean> tp1 = player1.teleportAsync(loc1);
                CompletableFuture<Boolean> tp2 = player2.teleportAsync(loc2);

                CompletableFuture.allOf(tp1, tp2).thenCompose(v ->
                    CompletableFuture.allOf(
                        loadChunksAroundAsync(player1),
                        loadChunksAroundAsync(player2)
                    )
                ).thenRun(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            kit.applyToPlayer(player1);
                            kit.applyToPlayer(player2);

                            player1.sendMessage(ChatColor.GREEN + "Duel started against " + player2.getName() + "!");
                            player2.sendMessage(ChatColor.GREEN + "Duel started against " + player1.getName() + "!");

                            startCountdown(duel);
                        }
                    }.runTask(plugin);
                });
            }
        }.runTaskLater(plugin, 10L);
    }

    private void startCountdown(ActiveDuel duel) {
        final int[] countdown = {5};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    String title = ChatColor.GOLD + "" + countdown[0];
                    String subtitle = ChatColor.GRAY + "Get ready!";

                    duel.getPlayer1().sendTitle(title, subtitle, 0, 20, 5);
                    duel.getPlayer2().sendTitle(title, subtitle, 0, 20, 5);

                    countdown[0]--;
                } else {
                    duel.getPlayer1().sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.GRAY + "Duel started!", 0, 20, 5);
                    duel.getPlayer2().sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.GRAY + "Duel started!", 0, 20, 5);

                    duel.setCountdownInProgress(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endDuel(ActiveDuel duel, Player winner, Player loser) {
        activeDuels.remove(duel.getPlayer1().getUniqueId());
        activeDuels.remove(duel.getPlayer2().getUniqueId());

        String winnerName = winner.getName();
        String loserName = loser.getName();

        if (winner.isOnline()) {
            winner.getInventory().clear();
            winner.getInventory().setArmorContents(new ItemStack[4]);
            winner.getInventory().setItemInOffHand(null);
            spawnWinFireworks(winner);
        }

        if (loser.isOnline()) {
            loser.getInventory().clear();
            loser.getInventory().setArmorContents(new ItemStack[4]);
            loser.getInventory().setItemInOffHand(null);
        }

        if (winner.isOnline()) {
            winner.sendTitle(ChatColor.GREEN + "YOU WON!", ChatColor.GRAY + "Against " + loserName, 10, 70, 20);
        }
        if (loser.isOnline()) {
            loser.sendTitle(ChatColor.RED + "YOU LOST!", ChatColor.GRAY + "Against " + winnerName, 10, 70, 20);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (winner.isOnline()) {
                    plugin.getPlayerDataManager().restorePlayerData(winner);
                }
                if (loser.isOnline()) {
                    plugin.getPlayerDataManager().restorePlayerData(loser);
                }

                plugin.getMapManager().deleteArena(duel.getMap(), duel.getArenaId());
            }
        }.runTaskLater(plugin, 100L);
    }

    public void endAllDuels() {
        for (ActiveDuel duel : new ArrayList<>(activeDuels.values())) {
            plugin.getPlayerDataManager().restorePlayerData(duel.getPlayer1());
            plugin.getPlayerDataManager().restorePlayerData(duel.getPlayer2());
            plugin.getMapManager().deleteArena(duel.getMap(), duel.getArenaId());
        }
        activeDuels.clear();
    }

    public Map<UUID, ActiveDuel> getActiveDuels() {
        return activeDuels;
    }

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    public ActiveDuel getDuel(Player player) {
        return activeDuels.get(player.getUniqueId());
    }

    public DuelRequest getRequest(Player sender, Player target) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        if (request != null && request.sender().equals(sender)) {
            return request;
        }
        return null;
    }

    public void removePendingRequest(Player player) {
        List<UUID> keysToRemove = new ArrayList<>();
        for (Map.Entry<UUID, DuelRequest> entry : pendingRequests.entrySet()) {
            DuelRequest request = entry.getValue();
            if (request.target().equals(player) || request.sender().equals(player)) {
                keysToRemove.add(entry.getKey());
            }
        }
        keysToRemove.forEach(pendingRequests::remove);
    }

    private void spawnWinFireworks(Player player) {
        Location loc = player.getLocation().add(0, 3, 0);
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(1);
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }

    private CompletableFuture<Void> loadChunksAroundAsync(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return CompletableFuture.completedFuture(null);

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        int radius = 1;
        for (int x = chunkX - radius; x <= chunkX + radius; x++) {
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                futures.add(world.getChunkAtAsync(x, z));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
