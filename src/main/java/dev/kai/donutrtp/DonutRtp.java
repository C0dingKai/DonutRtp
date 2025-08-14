package dev.kai.donutrtp;

import dev.kai.donutrtp.rtp.RtpCommand;
import dev.kai.donutrtp.rtp.RtpListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutRtp extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new RtpListener(this), this);
        getCommand("rtp").setExecutor(new RtpCommand());

    }

    @Override
    public void onDisable() {
        getLogger().info("DonutRtp has been disabled");
    }
}
