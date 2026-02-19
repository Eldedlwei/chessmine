package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.MineChess;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;

public class AvoidInVanillaCraftsListener implements Listener {
    @EventHandler
    public void preventCustomItemInVanillaCrafts(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (item.getPersistentDataContainer().has(MineChess.BOARD_ITEM_KEY, PersistentDataType.BYTE)) {

                Recipe recipe = event.getRecipe();
                if (recipe instanceof Keyed keyed) {
                    if (!keyed.getKey().getKey().equals("chess_tutorial_recipe")) {
                        event.getInventory().setResult(null);
                        return;
                    }
                }
            }

            if (item.getPersistentDataContainer().has(MineChess.BOOK_ITEM_KEY, PersistentDataType.BYTE)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}

