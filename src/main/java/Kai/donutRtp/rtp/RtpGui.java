package EmberLabs.server.rtp;

import EmberLabs.server.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RtpGui {

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.color("ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ"));


        ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta overworldMeta = overworld.getItemMeta();
        overworldMeta.setDisplayName(ColorUtil.color("&#00F986ᴏᴠᴇʀᴡᴏʀʟᴅ"));
        overworldMeta.setLore(Arrays.asList(
                ColorUtil.color("&fClick to randomly teleport"),
                "",
                ColorUtil.color("&7Players (&#0ea4e5" + Bukkit.getWorld("world").getPlayers().size() + "&7)")
        ));
        overworld.setItemMeta(overworldMeta);
        gui.setItem(11, overworld);


        ItemStack nether = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = nether.getItemMeta();
        netherMeta.setDisplayName(ColorUtil.color("&#00F986ɴᴇᴛʜᴇʀ"));
        netherMeta.setLore(Arrays.asList(
                ColorUtil.color("&fClick to randomly teleport"),
                "",
                ColorUtil.color("§7Players (&#0ea4e5" + Bukkit.getWorld("world_nether").getPlayers().size() + "§7)")
        ));
        nether.setItemMeta(netherMeta);
        gui.setItem(13, nether);


        ItemStack end = new ItemStack(Material.END_STONE);
        ItemMeta endMeta = end.getItemMeta();
        endMeta.setDisplayName(ColorUtil.color("&#00F986ᴛʜᴇ ᴇɴᴅ"));
        endMeta.setLore(Arrays.asList(
                ColorUtil.color("&fClick to randomly teleport"),
                "§7",
                ColorUtil.color("§7Players (&#0ea4e5" + Bukkit.getWorld("world_the_end").getPlayers().size() + "§7)")
        ));
        end.setItemMeta(endMeta);
        gui.setItem(15, end);

        player.openInventory(gui);
    }

    private static int getPing(Player player) {
        try {
            return player.getPing();
        } catch (NoSuchMethodError e) {
            return -1;
        }
    }
}
