package me.son14ka.mineChess.items;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    public Knight(boolean isWhite) {
        super(isWhite);
    }

    @Override
    public List<int[]> getAvailableCells(Piece[][] board, int row, int col) {

        List<int[]> moves = new ArrayList<>();
        int[][] directions = {{2,1}, {1,2}, {-1,2}, {-2,1}, {-2,-1}, {-1,-2}, {1,-2}, {2,-1}};

        for (int[] d : directions){
            int r = row + d[0];
            int c = col + d[1];

            if (isOutOfBounds(r, c)) continue;

            if (board[r][c] == null)
                moves.add(new int[]{r, c});
            else if (board[r][c].isWhite != this.isWhite)
                moves.add(new int[]{r, c});
        }
        return moves;
    }
}
