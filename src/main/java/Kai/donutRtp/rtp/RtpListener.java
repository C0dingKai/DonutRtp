package Kai.donutRtp.rtp;

import Kai.donutRtp.DonutRtp;
import Kai.donutRtp.util.ColorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RtpListener implements Listener {

    private static DonutRtp plugin;
    private static final Random random = new Random();
    private static final LinkedList<UUID> rtpQueue = new LinkedList<>();
    private static final Map<UUID, String> playerDimension = new HashMap<>();
    private static final Set<UUID> teleportingPlayers = new HashSet<>();
    private static boolean processing = false;
    private static final Map<UUID, Long> rtpCooldowns = new HashMap<>();
    private static long getCooldownMillis() {
        return plugin.getConfig().getLong("cooldown", 15) * 1000L;
    }

    private static int getCountdownSeconds() {
        return plugin.getConfig().getInt("countdown", 5);
    }


    public RtpListener(DonutRtp pluginInstance) {
        plugin = pluginInstance;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
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

    private static World getWorldByDimension(String dim) {
        return switch (dim) {
            case "nether" -> Bukkit.getWorld("world_nether");
            case "end" -> Bukkit.getWorld("world_the_end");
            default -> Bukkit.getWorld("world");
        };
    }

    public static void startRtp(Player player, String dim, World world) {
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
            long timeLeft = lastTeleport +getCooldownMillis() - currentTime;
            if (timeLeft > 0) {
                player.closeInventory();
                player.sendMessage(ColorUtil.color("&cYou rtp for another " + (timeLeft / 1000) + "s."));
                player.sendActionBar(ColorUtil.color("&cYou can't rtp for another " + (timeLeft / 1000) + "s."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

        teleportingPlayers.add(uuid);

        player.closeInventory();
        Location startLoc = player.getLocation();

        new BukkitRunnable() {
            int seconds = getCountdownSeconds();

            @Override
            public void run() {
                if (!player.getLocation().getWorld().equals(startLoc.getWorld()) ||
                        player.getLocation().distanceSquared(startLoc) > 0.01) {
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
                    if (rtpQueue.isEmpty()) {
                        Location safeLocation = findSafeLocation(world, dim);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.teleport(safeLocation);
                                player.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                                player.sendActionBar(ColorUtil.color("&#b6cbfaYou teleported to a random location"));
                                teleportingPlayers.remove(uuid);
                                rtpCooldowns.put(uuid, System.currentTimeMillis());
                            }
                        });
                    } else {
                        rtpQueue.add(uuid);
                        playerDimension.put(uuid, dim);
                        rtpCooldowns.put(uuid, System.currentTimeMillis());
                        notifyQueuePosition(player);
                        processQueue();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static void notifyQueuePosition(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!rtpQueue.contains(player.getUniqueId())) {
                    cancel();
                    return;
                }
                int position = rtpQueue.indexOf(player.getUniqueId()) + 1;
                player.sendActionBar(ColorUtil.color("&#b6cbfaYou are #" + position + " in the queue."));
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private static void processQueue() {
        if (processing) return;
        processing = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rtpQueue.isEmpty()) {
                    processing = false;
                    cancel();
                    return;
                }

                UUID uuid = rtpQueue.peek();
                if (uuid == null) return;

                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    rtpQueue.remove(uuid);
                    playerDimension.remove(uuid);
                    teleportingPlayers.remove(uuid);
                    return;
                }

                String dim = playerDimension.get(uuid);
                World world = getWorldByDimension(dim);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Location safeLocation = findSafeLocation(world, dim);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleport(safeLocation);
                            player.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                            player.sendActionBar(ColorUtil.color("&7You teleported to a random location"));
                        }

                        rtpQueue.remove(uuid);
                        playerDimension.remove(uuid);
                        teleportingPlayers.remove(uuid);
                    });
                });
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private static Location findSafeLocation(World world, String dim) {
        int minX = plugin.getConfig().getInt(dim + ".min-x", -1000);
        int maxX = plugin.getConfig().getInt(dim + ".max-x", 1000);
        int minZ = plugin.getConfig().getInt(dim + ".min-z", -1000);
        int maxZ = plugin.getConfig().getInt(dim + ".max-z", 1000);

        SafeCheck check = switch (dim) {
            case "nether" -> RtpListener::isSafeNetherLocation;
            case "end" -> RtpListener::isSafeEndLocation;
            default -> RtpListener::isSafeLocation;
        };

        return getSafeLocation(world, minX, maxX, minZ, maxZ, check);
    }


    private static Location getSafeLocation(World world, int minX, int maxX, int minZ, int maxZ, SafeCheck check) {
        for (int i = 0; i < 1000; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSafeY(world, x, z);
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (check.test(loc)) return loc;
        }
        return new Location(world, 0.5, getSafeY(world, 0, 0), 0.5);
    }



    private static int getSafeY(World world, int x, int z) {
        if (world.getEnvironment() == World.Environment.NETHER) {

            int maxRoofY = 108;
            for (int y = maxRoofY; y > 1; y--) {
                Material block = world.getBlockAt(x, y, z).getType();
                if (block.isSolid() && block != Material.LAVA && block != Material.WATER && block != Material.BEDROCK) {
                    return y + 1;
                }
            }
            return 50;
        } else {
            int y = world.getHighestBlockYAt(x, z);
            while (y > 1) {
                Material block = world.getBlockAt(x, y, z).getType();
                if (block.isSolid() && block != Material.WATER && block != Material.LAVA) break;
                y--;
            }
            return Math.max(y + 1, world.getHighestBlockYAt(x, z));
        }
    }

    private static boolean isSafeLocation(Location loc) {
        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block current = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();

        return below.getType().isSolid() && !isTransparent(below)
                && current.getType() == Material.AIR && above.getType() == Material.AIR;
    }


    private static boolean isSafeNetherLocation(Location loc) {
        if (loc.getY() >= 108) return false;

        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block current = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();

        return below.getType().isSolid() && below.getType() != Material.BEDROCK && !isUnsafeNetherBlock(below.getType())
                && current.getType() == Material.AIR && above.getType() == Material.AIR;
    }

    private static boolean isSafeEndLocation(Location loc) {
        Block below = loc.clone().add(0, -1, 0).getBlock();
        Block current = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();

        return below.getType().isSolid() && current.getType() == Material.AIR && above.getType() == Material.AIR;
    }

    private static boolean isTransparent(Block block) {
        return switch (block.getType()) {
            case AIR, CAVE_AIR, VOID_AIR, FERN, DEAD_BUSH, DANDELION, POPPY,
                 BLUE_ORCHID, ALLIUM, AZURE_BLUET, RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
                 OXEYE_DAISY, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE, SUNFLOWER, LILAC,
                 ROSE_BUSH, PEONY, SWEET_BERRY_BUSH, GRAVEL -> true;
            default -> false;
        };
    }

    private static boolean isUnsafeNetherBlock(Material mat) {
        return switch (mat) {
            case LAVA, MAGMA_BLOCK, FIRE, CAMPFIRE -> true;
            default -> false;
        };
    }

    @FunctionalInterface
    private interface SafeCheck {
        boolean test(Location location);
    }
}