package me.son14ka.mineChess.items;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    public Pawn(boolean isWhite) {
        super(isWhite);
    }

    @Override
    public List<int[]> getAvailableCells(Piece[][] board, int row, int col) {
        List<int[]> moves = new ArrayList<>();
        int dir = isWhite ? 1 : -1;

        int nextRow = row + dir;
        if (!isOutOfBounds(nextRow, col) && board[nextRow][col] == null) {
            moves.add(new int[]{nextRow, col});

            int doubleRow = row + (2 * dir);
            if (!hasMoved && !isOutOfBounds(doubleRow, col) && board[doubleRow][col] == null) {
                moves.add(new int[]{doubleRow, col});
            }
        }

        int[] diagCols = {col - 1, col + 1};
        for (int diagCol : diagCols) {
            if (!isOutOfBounds(nextRow, diagCol)) {
                Piece target = board[nextRow][diagCol];
                if (target != null && target.isWhite != this.isWhite) {
                    moves.add(new int[]{nextRow, diagCol});
                }
            }
        }

        return moves;
    }
}
