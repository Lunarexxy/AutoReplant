package io.github.lunarexxy.AutoReplant;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

// Things that could be nice that I haven't bothered doing:
//
// Pumpkins are broken and dropped as carved pumpkins when sheared by a player, for people who make lots of jack-o-lanterns.
// Replant cocoa beans - the only difference is they sit next to a few different jungle wood types instead of on top of farmland.
// Sugarcane? Just gotta leave the base block standing while breaking the sugarcane block above it.
// Enchantment support, maybe? Dunno how hard it would be.

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {return;} // Can't replant what doesn't exist.
        BlockState blockState = block.getState();
        BlockData blockData = blockState.getBlockData();
        ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        ItemMeta heldItemMeta = heldItem.getItemMeta();

        // Is this the main hand event? (cuz apparently there's one for the off-hand that fires simultaneously)
        if (event.getHand() != EquipmentSlot.HAND) {return;}

        // Is player right clicking?
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {return;}

        // Is player using a hoe?
        if (!isHoeMaterial(heldItem.getType())) {return;}

        // Is the crop fully grown?
        if (!(blockData instanceof Ageable)) {return;}
        else if (((Ageable) blockData).getAge() != ((Ageable) blockData).getMaximumAge()) {return;}

        switch (blockState.getType()) {
            case WHEAT:
            case BEETROOTS:
            case CARROTS:
            case POTATOES:
                // I think some mods might delete the block below, for whatever reason. Check it, just in case.
                if (block.getLocation().subtract(0d, 1d, 0d).getBlock().getType() != Material.FARMLAND) {return;}
                break;
            case NETHER_WART:
                if (block.getLocation().subtract(0d, 1d, 0d).getBlock().getType() != Material.SOUL_SAND) {return;}
                break;
            default:
                // Fail if crop is not supported.
                return;
        }
        block.breakNaturally(heldItem);  // Break the crop with the player's tool
        ((Ageable) blockData).setAge(0); // Zero the age of the crop to replant
        block.setBlockData(blockData);   // "Plant" the new crop
        
        // If the player is a god, leave their tool alone.
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE ||
            event.getPlayer().getGameMode() == GameMode.SPECTATOR) {return;}

        // Damage the player's tool, if allowed. (and if it exists; 'instanceof' also handles null checking)
        if (heldItemMeta instanceof Damageable && !heldItemMeta.isUnbreakable()) {
            int dmg = ((Damageable) heldItemMeta).getDamage() + 1;
            ((Damageable) heldItemMeta).setDamage(dmg);
            heldItem.setItemMeta(heldItemMeta);

            // Break if durability is exceeded.
            if (shouldHoeBreak(heldItem.getType(), dmg)) { heldItem.setAmount(0); }
        }

        
    }

    private boolean isHoeMaterial(Material material) {
        return (material == Material.WOODEN_HOE ||
                material == Material.STONE_HOE ||
                material == Material.IRON_HOE ||
                material == Material.GOLDEN_HOE ||
                material == Material.DIAMOND_HOE ||
                material == Material.NETHERITE_HOE);
    }

    // Returns true if given hoe/damage is at or beyond max durability. (apparently this core feature isn't exposed anywhere???)
    private boolean shouldHoeBreak(Material hoeMaterial, int hoeDamage) {
        if (hoeMaterial == Material.WOODEN_HOE && hoeDamage >= 59) {return true;}
        if (hoeMaterial == Material.STONE_HOE && hoeDamage >= 131) {return true;}
        if (hoeMaterial == Material.IRON_HOE && hoeDamage >= 250) {return true;}
        if (hoeMaterial == Material.GOLDEN_HOE && hoeDamage >= 32) {return true;}
        if (hoeMaterial == Material.DIAMOND_HOE && hoeDamage >= 1561) {return true;}
        if (hoeMaterial == Material.NETHERITE_HOE && hoeDamage >= 2031) {return true;}
        return false;
    }
}
