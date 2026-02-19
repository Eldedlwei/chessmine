package me.son14ka.mineChess.items;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    public King(boolean isWhite) {
        super(isWhite);
    }

    @Override
    public List<int[]> getAvailableCells(Piece[][] board, int row, int col) {
        List<int[]> moves = new ArrayList<>();
        int[][] directions = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}};

        for (int[] d : directions) {
            int r = row + d[0], c = col + d[1];
            if (isOutOfBounds(r, c)) continue;

            // Перевіряємо: чи клітинка вільна/ворожа ТА чи вона не під атакою
            if ((board[r][c] == null || board[r][c].isWhite != this.isWhite)) {
                if (!isSquareAttacked(board, r, c, this.isWhite)) {
                    moves.add(new int[]{r, c});
                }
            }
        }
        return moves;
    }


}
