package me.son14ka.mineChess;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class BoardSpawner {
    private BoardSpawner() {
    }

    public static void spawnInteractionCells(MineChess plugin, Location baseLoc, UUID gameId) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Location cellLoc = baseLoc.clone().add(col / 4.0 + 0.125, 0.05, row / 4.0 + 0.125);
                baseLoc.getWorld().spawn(cellLoc, Interaction.class, interaction -> {
                    interaction.setInteractionWidth(0.25f);
                    interaction.setInteractionHeight(0.05f);

                    var pdc = interaction.getPersistentDataContainer();
                    pdc.set(new NamespacedKey(plugin, "chess_row"), PersistentDataType.INTEGER, row);
                    pdc.set(new NamespacedKey(plugin, "chess_col"), PersistentDataType.INTEGER, col);
                    pdc.set(new NamespacedKey(plugin, "game_id"), PersistentDataType.STRING, gameId.toString());
                });
            }
        }
    }
}
