package com.scratchdev.sparkduels;

import co.aikar.commands.PaperCommandManager;
import com.scratchdev.sparkduels.commands.DuelCommand;
import com.scratchdev.sparkduels.commands.DueladminCommand;
import com.scratchdev.sparkduels.data.DuelMap;
import com.scratchdev.sparkduels.data.Kit;
import com.scratchdev.sparkduels.listeners.DamageListener;
import com.scratchdev.sparkduels.listeners.DuelListener;
import com.scratchdev.sparkduels.managers.*;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Duels extends JavaPlugin {
    private static Duels instance;
    private DuelManager duelManager;
    private KitManager kitManager;
    private MapManager mapManager;
    private WorldManager worldManager;
    private PlayerDataManager playerDataManager;
    private DuelListener duelListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        worldManager = new WorldManager(this);
        worldManager.createDuelsWorld();

        kitManager = new KitManager(this);
        mapManager = new MapManager(this);
        playerDataManager = new PlayerDataManager();
        duelManager = new DuelManager(this);

        kitManager.loadKits();
        mapManager.loadMaps();
        mapManager.loadPregeneratedArenas();
        mapManager.pregenerateAll();

        registerCommands();
        registerListeners();

        getLogger().info("SparkDuels plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.endAllDuels();
        }
        if (kitManager != null) {
            kitManager.saveKits();
        }
        if (mapManager != null) {
            mapManager.saveMaps();
            mapManager.savePregeneratedArenas();
        }
        getLogger().info("SparkDuels plugin disabled!");
    }

    private void registerCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);

        commandManager.getCommandCompletions().registerAsyncCompletion("kits", c ->
            kitManager.getAllKits().stream().map(Kit::name).toList()
        );
        commandManager.getCommandCompletions().registerAsyncCompletion("maps", c ->
            mapManager.getAllMaps().stream().map(DuelMap::name).toList()
        );
        commandManager.getCommandCompletions().registerAsyncCompletion("items", c ->
                Arrays.stream(Material.values())
                        .filter(m -> !m.isLegacy() && m.isItem())
                        .map(m -> m.name().toLowerCase())
                        .collect(Collectors.toList())
        );
        commandManager.registerCommand(new DuelCommand(this));
        commandManager.registerCommand(new DueladminCommand(this));
    }

    private void registerListeners() {
        duelListener = new DuelListener(this);
        getServer().getPluginManager().registerEvents(duelListener, this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
    }

    public static Duels getInstance() {
        return instance;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public DuelListener getDuelListener() {
        return duelListener;
    }
}
