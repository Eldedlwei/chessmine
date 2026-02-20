package me.son14ka.mineChess;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class BoardSpawner {
    private BoardSpawner() {
    }

    public static void spawnInteractionCells(MineChess plugin, Location baseLoc, UUID gameId) {
        MineChessKeys keys = plugin.getKeys();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final int cellRow = row;
                final int cellCol = col;
                Location cellLoc = BoardGeometry.cellCenter(baseLoc, cellRow, cellCol, BoardGeometry.INTERACTION_Y);
                baseLoc.getWorld().spawn(cellLoc, Interaction.class, interaction -> {
                    interaction.setInteractionWidth(BoardGeometry.INTERACTION_WIDTH);
                    interaction.setInteractionHeight(BoardGeometry.INTERACTION_HEIGHT);

                    var pdc = interaction.getPersistentDataContainer();
                    pdc.set(keys.chessRow(), PersistentDataType.INTEGER, cellRow);
                    pdc.set(keys.chessCol(), PersistentDataType.INTEGER, cellCol);
                    pdc.set(keys.gameId(), PersistentDataType.STRING, gameId.toString());
                });
            }
        }
    }
}
