package me.son14ka.mineChess;

import org.bukkit.Location;

public final class BoardGeometry {
    public static final double CELL_SIZE = 0.5;
    public static final double HALF_CELL = CELL_SIZE / 2.0;

    public static final double INTERACTION_Y = 0.1;
    public static final float INTERACTION_WIDTH = (float) CELL_SIZE;
    public static final float INTERACTION_HEIGHT = 0.2f;

    public static final double PIECE_BASE_Y = 0.137;
    public static final double HIGHLIGHT_Y = 0.1;

    public static final double PROMOTION_BASE_Y = 2.5;
    public static final double PROMOTION_SPACING_X = 0.8;
    public static final double PROMOTION_INTERACTION_Y_OFFSET = -0.4;
    public static final float PROMOTION_INTERACTION_WIDTH = 0.8f;
    public static final float PROMOTION_INTERACTION_HEIGHT = 0.3f;

    private BoardGeometry() {
    }

    public static Location cellCorner(Location origin, int row, int col) {
        return origin.clone().add(col * CELL_SIZE, 0.0, row * CELL_SIZE);
    }

    public static Location cellCenter(Location origin, int row, int col, double yOffset) {
        return origin.clone().add(col * CELL_SIZE + HALF_CELL, yOffset, row * CELL_SIZE + HALF_CELL);
    }

    public static Location promotionBase(Location origin, int row, int col) {
        return cellCenter(origin, row, col, PROMOTION_BASE_Y);
    }

    public static Location promotionChoice(Location base, int index) {
        return base.clone().add((index - 1.5) * PROMOTION_SPACING_X, 0.0, 0.0);
    }
}
