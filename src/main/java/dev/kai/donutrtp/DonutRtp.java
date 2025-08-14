package dev.kai.donutrtp;

import dev.kai.donutrtp.rtp.RtpCommand;
import dev.kai.donutrtp.rtp.RtpListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutRtp extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RtpListener rtpListener = new RtpListener(this);
        getCommand("rtp").setExecutor(new RtpCommand(rtpListener));
        getServer().getPluginManager().registerEvents(rtpListener, this);

    }

    @Override
    public void onDisable() {
        getLogger().info("DonutRtp has been disabled");
    }
}
