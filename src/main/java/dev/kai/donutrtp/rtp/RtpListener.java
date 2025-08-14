package dev.kai.donutrtp.rtp;


import dev.kai.donutrtp.DonutRtp;
import dev.kai.donutrtp.util.ColorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RtpListener implements Listener {

    private final DonutRtp plugin;
    private final Random random = new Random();
    private final LinkedList<UUID> rtpQueue = new LinkedList<>();
    private final Map<UUID, String> playerDimension = new HashMap<>();
    private final Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<UUID, Long> rtpCooldowns = new HashMap<>();
    private boolean processingQueue = false;

    public RtpListener(DonutRtp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ColorUtil.stripColor(e.getView().getTitle()).equalsIgnoreCase("ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ")) return;
        e.setCancelled(true);
        if (teleportingPlayers.contains(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(ColorUtil.color("&cYou are already teleporting."));
            player.sendActionBar(ColorUtil.color("&cYou are already teleporting."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String dim = switch (e.getSlot()) {
            case 11 -> "overworld";
            case 13 -> "nether";
            case 15 -> "end";
            default -> null;
        };
        if (dim == null) return;
        World world = getWorldByDimension(dim);
        if (world == null) return;
        startRtp(player, dim, world);
    }

    private World getWorldByDimension(String dim) {
        return switch (dim) {
            case "nether" -> Bukkit.getWorld("world_nether");
            case "end" -> Bukkit.getWorld("world_the_end");
            default -> Bukkit.getWorld("world");
        };
    }

    private long getCooldownMillis() {
        return plugin.getConfig().getLong("cooldown", 15) * 1000L;
    }

    private int getCountdownSeconds() {
        return plugin.getConfig().getInt("countdown", 5);
    }

    public void startRtp(CommandSender sender, String dim, World world) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
            return;
        }
        startRtp(player, dim, world);
    }

    private void startRtp(Player player, String dim, World world) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (rtpQueue.contains(uuid) || teleportingPlayers.contains(uuid)) {
            player.closeInventory();
            player.sendMessage(ColorUtil.color("&cYou are already teleporting."));
            player.sendActionBar(ColorUtil.color("&cYou are already teleporting."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (rtpCooldowns.containsKey(uuid)) {
            long lastTeleport = rtpCooldowns.get(uuid);
            long timeLeft = lastTeleport + getCooldownMillis() - currentTime;
            if (timeLeft > 0) {
                player.closeInventory();
                player.sendMessage(ColorUtil.color("&cYou can't rtp for another " + (timeLeft / 1000) + "s."));
                player.sendActionBar(ColorUtil.color("&cYou can't rtp for another " + (timeLeft / 1000) + "s."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }
        teleportingPlayers.add(uuid);
        player.closeInventory();
        Location startLoc = player.getLocation();
        int countdown = getCountdownSeconds();

        new BukkitRunnable() {
            int seconds = countdown;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getLocation().getWorld().equals(startLoc.getWorld())
                        || player.getLocation().distanceSquared(startLoc) > 0.01) {
                    player.sendActionBar(ColorUtil.color("&cTeleportation cancelled because you moved."));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    teleportingPlayers.remove(uuid);
                    cancel();
                    return;
                }
                if (seconds > 0) {
                    player.sendActionBar(ColorUtil.color("&#b6cbfaTeleporting in " + seconds + " seconds. Do not move"));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    seconds--;
                } else {
                    cancel();
                    rtpQueue.add(uuid);
                    playerDimension.put(uuid, dim);
                    processQueue();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void processQueue() {
        if (processingQueue) return;
        processingQueue = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rtpQueue.isEmpty()) {
                    processingQueue = false;
                    cancel();
                    return;
                }

                UUID uuid = rtpQueue.peek();
                if (uuid == null) {
                    rtpQueue.poll();
                    return;
                }

                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    rtpQueue.poll();
                    playerDimension.remove(uuid);
                    teleportingPlayers.remove(uuid);
                    return;
                }

                String dim = playerDimension.get(uuid);
                World world = getWorldByDimension(dim);

                CompletableFuture.supplyAsync(() -> findSafeLocation(world, dim))
                        .thenAcceptAsync(loc -> {
                            if (player.isOnline()) {
                                player.teleport(loc);
                                player.sendActionBar(ColorUtil.color("&#b6cbfaYou teleported to a random location"));
                                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                            }
                            teleportingPlayers.remove(uuid);
                            rtpCooldowns.put(uuid, System.currentTimeMillis());
                            rtpQueue.poll();
                            playerDimension.remove(uuid);
                        }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private Location findSafeLocation(World world, String dim) {
        int minX = plugin.getConfig().getInt(dim + ".min-x", -1000);
        int maxX = plugin.getConfig().getInt(dim + ".max-x", 1000);
        int minZ = plugin.getConfig().getInt(dim + ".min-z", -1000);
        int maxZ = plugin.getConfig().getInt(dim + ".max-z", 1000);
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSafeY(world, x, z);
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isLocationSafe(loc, dim)) return loc;
        }
        return new Location(world, 0.5, getSafeY(world, 0, 0), 0.5);
    }

    private int getSafeY(World world, int x, int z) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            for (int y = 108; y > 1; y--) {
                Material block = world.getBlockAt(x, y, z).getType();
                if (block.isSolid() && block != Material.LAVA && block != Material.WATER && block != Material.BEDROCK) return y + 1;
            }
            return 50;
        } else {
            int y = world.getHighestBlockYAt(x, z);
            while (y > 1) {
                Material block = world.getBlockAt(x, y, z).getType();
                if (block.isSolid() && block != Material.LAVA && block != Material.WATER) break;
                y--;
            }
            return Math.max(y + 1, world.getHighestBlockYAt(x, z));
        }
    }

    private boolean isLocationSafe(Location loc, String dim) {
        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block current = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();
        switch (dim) {
            case "nether":
                if (loc.getY() >= 108) return false;
                return below.getType().isSolid() && below.getType() != Material.BEDROCK && !isUnsafeNetherBlock(below.getType())
                        && current.getType() == Material.AIR && above.getType() == Material.AIR;
            case "end":
                return below.getType().isSolid() && current.getType() == Material.AIR && above.getType() == Material.AIR;
            default:
                return below.getType().isSolid() && !isTransparent(below) && current.getType() == Material.AIR && above.getType() == Material.AIR;
        }
    }

    private boolean isTransparent(Block block) {
        return switch (block.getType()) {
            case AIR, CAVE_AIR, VOID_AIR, FERN, DEAD_BUSH, DANDELION, POPPY,
                 BLUE_ORCHID, ALLIUM, AZURE_BLUET, RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
                 OXEYE_DAISY, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE, SUNFLOWER, LILAC,
                 ROSE_BUSH, PEONY, SWEET_BERRY_BUSH, GRAVEL -> true;
            default -> false;
        };
    }

    private boolean isUnsafeNetherBlock(Material mat) {
        return switch (mat) {
            case LAVA, MAGMA_BLOCK, FIRE, CAMPFIRE -> true;
            default -> false;
        };
    }
}
