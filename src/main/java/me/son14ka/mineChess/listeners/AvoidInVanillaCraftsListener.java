package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.MineChessKeys;
import me.son14ka.mineChess.items.ChessBoardItem;
import me.son14ka.mineChess.items.ChessBookItem;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class AvoidInVanillaCraftsListener implements Listener {
    private final MineChess plugin;
    private final MineChessKeys keys;

    public AvoidInVanillaCraftsListener(MineChess plugin) {
        this.plugin = plugin;
        this.keys = plugin.getKeys();
    }

    @EventHandler
    public void preventCustomItemInVanillaCrafts(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (ChessBoardItem.isBoardItem(plugin, item)) {

                Recipe recipe = event.getRecipe();
                if (recipe instanceof Keyed keyed) {
                    if (!keyed.getKey().equals(keys.chessTutorialRecipe())) {
                        event.getInventory().setResult(null);
                        return;
                    }
                }
            }

            if (ChessBookItem.isBookItem(plugin, item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
