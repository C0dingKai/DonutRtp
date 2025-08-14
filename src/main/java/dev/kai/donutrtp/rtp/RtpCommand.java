package dev.kai.donutrtp.rtp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RtpCommand implements CommandExecutor, TabCompleter {

    private final List<String> options = Arrays.asList("overworld", "nether", "end");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length == 0) {
            RtpGui.open(player);
            return true;
        }

        if (args.length == 1) {
            String dim = args[0].toLowerCase();
            World world = switch (dim) {
                case "overworld" -> Bukkit.getWorld("world");
                case "nether" -> Bukkit.getWorld("world_nether");
                case "end" -> Bukkit.getWorld("world_the_end");
                default -> null;
            };

            if (world == null) {
                RtpGui.open(player);
                return true;
            }

            RtpListener.startRtp(player, dim, world);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
            return completions;
        }
        return List.of();
    }
}
