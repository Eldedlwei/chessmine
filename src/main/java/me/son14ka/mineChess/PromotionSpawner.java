package me.son14ka.mineChess;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.util.List;

public final class PromotionSpawner {
    private PromotionSpawner() {
    }

    public static void spawnPromotionChoices(MineChess plugin, ChessGame game, int row, int col, boolean isWhite) {
        Location baseLoc = BoardGeometry.promotionBase(game.getOrigin(), row, col);

        int[] types = {5, 4, 3, 2};
        if (!isWhite) { for (int i = 0; i < 4; i++) types[i] += 6; }

        for (int i = 0; i < 4; i++) {
            Location choiceLoc = BoardGeometry.promotionChoice(baseLoc, i);
            int cmd = types[i];

            choiceLoc.getWorld().spawn(choiceLoc, ItemDisplay.class, display -> {
                ItemStack item = new ItemStack(Material.TORCH);
                var meta = item.getItemMeta();
                var modelData = meta.getCustomModelDataComponent();
                modelData.setFloats(List.of((float) cmd));
                meta.setCustomModelDataComponent(modelData);
                item.setItemMeta(meta);
                display.setItemStack(item);

                Transformation trafo = display.getTransformation();
                trafo.getScale().set(0.25f, 0.25f, 0.25f);
                display.setTransformation(trafo);

                display.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_promotion_item"), PersistentDataType.BYTE, (byte) 1);
                display.getPersistentDataContainer().set(new NamespacedKey(plugin, "game_id"), PersistentDataType.STRING, game.getGameId().toString());
            });

            choiceLoc.getWorld().spawn(choiceLoc.clone().add(0, BoardGeometry.PROMOTION_INTERACTION_Y_OFFSET, 0), Interaction.class, inter -> {
                inter.setInteractionWidth(BoardGeometry.PROMOTION_INTERACTION_WIDTH);
                inter.setInteractionHeight(BoardGeometry.PROMOTION_INTERACTION_HEIGHT);

                var pdc = inter.getPersistentDataContainer();
                pdc.set(new NamespacedKey(plugin, "promotion_cmd"), PersistentDataType.INTEGER, cmd);
                pdc.set(new NamespacedKey(plugin, "promotion_row"), PersistentDataType.INTEGER, row);
                pdc.set(new NamespacedKey(plugin, "promotion_col"), PersistentDataType.INTEGER, col);
                pdc.set(new NamespacedKey(plugin, "game_id"), PersistentDataType.STRING, game.getGameId().toString());
            });
        }
    }
}
