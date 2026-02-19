package me.son14ka.mineChess.items;

import java.util.List;

public abstract class Piece {
    public final Boolean isWhite;
    public Boolean hasMoved = false;

    public Piece(Boolean isWhite) {
        this.isWhite = isWhite;
    }

    public abstract List<int[]> getAvailableCells(Piece[][] board, int row, int col);

    public void setHasMoved(boolean hasMoved){
        this.hasMoved = hasMoved;
    }

    protected static boolean isOutOfBounds(int r, int c) {
        return r < 0 || r >= 8 || c < 0 || c >= 8;
    }

    public static boolean isSquareAttacked(Piece[][] board, int targetR, int targetC, boolean isWhite) {
        int[][] rookDirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        if (checkRay(board, targetR, targetC, rookDirs, isWhite, Rook.class, Queen.class)) return true;

        int[][] bishopDirs = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        if (checkRay(board, targetR, targetC, bishopDirs, isWhite, Bishop.class, Queen.class)) return true;

        int[][] knightMoves = {{2,1}, {1,2}, {-1,2}, {-2,1}, {-2,-1}, {-1,-2}, {1,-2}, {2,-1}};
        for (int[] m : knightMoves) {
            int r = targetR + m[0], c = targetC + m[1];
            if (!isOutOfBounds(r, c)) {
                Piece p = board[r][c];
                if (p instanceof Knight && p.isWhite != isWhite) return true;
            }
        }

        int pDir = isWhite ? 1 : -1;
        int[][] pawnAttacks = {{pDir, 1}, {pDir, -1}};
        for (int[] a : pawnAttacks) {
            int r = targetR + a[0], c = targetC + a[1];
            if (!isOutOfBounds(r, c)) {
                Piece p = board[r][c];
                if (p instanceof Pawn && p.isWhite != isWhite) return true;
            }
        }

        return false;
    }

    private static boolean checkRay(Piece[][] board, int r, int c, int[][] dirs, boolean isWhite, Class<?>... types) {
        for (int[] d : dirs) {
            for (int i = 1; i < 8; i++) {
                int nextR = r + d[0] * i;
                int nextC = c + d[1] * i;
                if (isOutOfBounds(nextR, nextC)) break;

                Piece p = board[nextR][nextC];
                if (p != null) {
                    if (p.isWhite != isWhite) {
                        for (Class<?> type : types) {
                            if (type.isInstance(p)) return true;
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }
}