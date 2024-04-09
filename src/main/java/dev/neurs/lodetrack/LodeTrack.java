package dev.neurs.lodetrack;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class LodeTrack extends JavaPlugin {

    @Override
    public void onEnable() {
        LodeTrackPlayerEvents lodeEvents = new LodeTrackPlayerEvents();
        getServer().getPluginManager().registerEvents(lodeEvents, this);
        Objects.requireNonNull(this.getCommand("lodetrack")).setExecutor(new LodeTrackCommands(lodeEvents, this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
