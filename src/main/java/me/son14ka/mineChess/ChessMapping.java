package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

public final class ChessMapping {
    private ChessMapping() {
    }

    public static Square toSquare(int row, int col) {
        char file = (char) ('A' + col);
        int rank = row + 1;
        return Square.valueOf("" + file + rank);
    }

    public static int[] toCoords(Square square) {
        String name = square.name();
        int col = name.charAt(0) - 'A';
        int row = name.charAt(1) - '1';
        return new int[]{row, col};
    }

    public static String sideToColor(Side side) {
        return side == Side.WHITE ? "white" : "black";
    }

    public static int toModelData(Piece piece) {
        if (piece == Piece.NONE) return -1;

        int base = switch (piece.getPieceType()) {
            case PAWN -> 1;
            case KNIGHT -> 2;
            case BISHOP -> 3;
            case ROOK -> 4;
            case QUEEN -> 5;
            case KING -> 6;
            default -> throw new IllegalArgumentException("Unsupported piece type: " + piece.getPieceType());
        };

        if (piece.getPieceSide() == Side.BLACK) base += 6;
        return base;
    }

    public static Piece toPromotionPiece(int cmd, Side side) {
        int type = side == Side.WHITE ? cmd : cmd - 6;
        return switch (type) {
            case 2 -> side == Side.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
            case 3 -> side == Side.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
            case 4 -> side == Side.WHITE ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
            default -> side == Side.WHITE ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
        };
    }

    public static PieceType pieceTypeFromModel(int cmd, Side side) {
        int type = side == Side.WHITE ? cmd : cmd - 6;
        return switch (type) {
            case 1 -> PieceType.PAWN;
            case 2 -> PieceType.KNIGHT;
            case 3 -> PieceType.BISHOP;
            case 4 -> PieceType.ROOK;
            case 5 -> PieceType.QUEEN;
            case 6 -> PieceType.KING;
            default -> null;
        };
    }
}
