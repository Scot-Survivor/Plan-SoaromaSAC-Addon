package com.shimincraft.plansac.soaromasacplanaddon;

import me.korbsti.soaromaac.Main;
import me.korbsti.soaromaac.api.SoaromaAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoaromaSACPlanAddon extends JavaPlugin {

    private SoaromaAPI soaromaAPI;
    private SoaromaSACStorage storage;
    private Plugin plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        soaromaAPI = new SoaromaAPI((Main) (Bukkit.getPluginManager().getPlugin("SoaromaSAC")));
        storage = new SoaromaSACStorage();
        plugin = Bukkit.getPluginManager().getPlugin("SoaromaSAC-Plan-Addon");
        try {
            new PlanHook(soaromaAPI, storage).hookIntoPlan();
            getLogger().info("PlanSoaromaSAC is alive");
        } catch (NoClassDefFoundError planIsNotInstalled) {
            // Plan is not installed
        }
        this.getServer().getPluginManager().registerEvents(new SoaromaListener(storage, plugin), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
