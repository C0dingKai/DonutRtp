package Kai.donutRtp;

import Kai.donutRtp.rtp.RtpCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutRtp extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new Kai.donutRtp.rtp.RtpListener(this), this);
        getCommand("rtp").setExecutor(new RtpCommand());

    }

    @Override
    public void onDisable() {
        System.out.println("[DonutRtp] Plugin has been disabled.");
    }
}
