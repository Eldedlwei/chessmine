package me.son14ka.mineChess.items;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {
    public Rook(Boolean isWhite){
        super(isWhite);
    }

    @Override
    public List<int[]> getAvailableCells(Piece[][] board, int row, int col) {
        List<int[]> moves = new ArrayList<>();
        int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}};

        for (int[] d : directions) {
            for (int i = 1; i < 8; i++) {
                int r = row + d[0] * i;
                int c = col + d[1] * i;

                if (isOutOfBounds(r, c)) break;

                if (board[r][c] == null) {
                    moves.add(new int[]{r, c});
                } else {
                    if (board[r][c].isWhite != this.isWhite) {
                        moves.add(new int[]{r, c});
                    }
                    break;
                }
            }
        }
        return moves;
    }
}