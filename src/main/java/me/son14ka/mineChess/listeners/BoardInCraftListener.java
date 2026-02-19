package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.items.ChessBoardItem;
import me.son14ka.mineChess.items.ChessBookItem;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;

public class BoardInCraftListener implements Listener {

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || event.getViewers().isEmpty()) return;

        if (recipe instanceof Keyed keyed) {
            String recipeKey = keyed.getKey().getKey();

            if (recipeKey.equals("chess_board")) {
                event.getInventory().setResult(ChessBoardItem.createTemplate());
            }

            else if (recipeKey.equals("chess_tutorial_recipe")) {
                if (isCustomBoardPresent(event.getInventory().getMatrix())) {
                    event.getInventory().setResult(ChessBookItem.create());
                } else {
                    event.getInventory().setResult(null);
                }
            }
        }
    }

    private boolean isCustomBoardPresent(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && item.getType() == Material.ITEM_FRAME) {
                if (item.getPersistentDataContainer().has(MineChess.BOARD_ITEM_KEY, PersistentDataType.BYTE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
