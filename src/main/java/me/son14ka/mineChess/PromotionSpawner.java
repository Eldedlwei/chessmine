package me.son14ka.mineChess;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.util.List;
import java.util.UUID;

public final class PromotionSpawner {
    private static final int[] WHITE_PROMOTION_CMDS = {5, 4, 3, 2};

    private PromotionSpawner() {
    }

    public static void spawnPromotionChoices(MineChess plugin, ChessGame game, int row, int col, boolean isWhite) {
        Location baseLoc = BoardGeometry.promotionBase(game.getOrigin(), row, col);
        MineChessKeys keys = plugin.getKeys();

        int[] types = WHITE_PROMOTION_CMDS.clone();
        if (!isWhite) { for (int i = 0; i < 4; i++) types[i] += 6; }

        for (int i = 0; i < 4; i++) {
            Location choiceLoc = BoardGeometry.promotionChoice(baseLoc, i);
            int cmd = types[i];

            choiceLoc.getWorld().spawn(choiceLoc, ItemDisplay.class, display -> {
                ItemStack item = new ItemStack(Material.TORCH);
                var meta = item.getItemMeta();
                if (meta == null) {
                    return;
                }
                var modelData = meta.getCustomModelDataComponent();
                modelData.setFloats(List.of((float) cmd));
                meta.setCustomModelDataComponent(modelData);
                item.setItemMeta(meta);
                display.setItemStack(item);

                Transformation trafo = display.getTransformation();
                trafo.getScale().set(0.25f, 0.25f, 0.25f);
                display.setTransformation(trafo);

                display.getPersistentDataContainer().set(keys.promotionItem(), PersistentDataType.BYTE, (byte) 1);
                display.getPersistentDataContainer().set(keys.gameId(), PersistentDataType.STRING, game.getGameId().toString());
            });

            choiceLoc.getWorld().spawn(choiceLoc.clone().add(0, BoardGeometry.PROMOTION_INTERACTION_Y_OFFSET, 0), Interaction.class, inter -> {
                inter.setInteractionWidth(BoardGeometry.PROMOTION_INTERACTION_WIDTH);
                inter.setInteractionHeight(BoardGeometry.PROMOTION_INTERACTION_HEIGHT);

                var pdc = inter.getPersistentDataContainer();
                pdc.set(keys.promotionCmd(), PersistentDataType.INTEGER, cmd);
                pdc.set(keys.promotionRow(), PersistentDataType.INTEGER, row);
                pdc.set(keys.promotionCol(), PersistentDataType.INTEGER, col);
                pdc.set(keys.gameId(), PersistentDataType.STRING, game.getGameId().toString());
            });
        }
    }

    public static void cleanupPromotionEntities(MineChess plugin, World world, UUID gameId) {
        MineChessKeys keys = plugin.getKeys();
        String idString = gameId.toString();

        for (Entity entity : world.getEntities()) {
            var pdc = entity.getPersistentDataContainer();
            String storedId = pdc.get(keys.gameId(), PersistentDataType.STRING);
            if (!idString.equals(storedId)) {
                continue;
            }
            if (pdc.has(keys.promotionItem(), PersistentDataType.BYTE)
                    || pdc.has(keys.promotionCmd(), PersistentDataType.INTEGER)) {
                entity.remove();
            }
        }
    }
}
