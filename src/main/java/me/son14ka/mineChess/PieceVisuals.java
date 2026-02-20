package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;

public final class PieceVisuals {
    private PieceVisuals() {
    }

    public static Visual resolve(MineChess plugin, Piece piece) {
        String sideKey = piece.getPieceSide() == Side.WHITE ? "white" : "black";
        String pieceKey = pieceTypeKey(piece.getPieceType());

        double scale = plugin.getConfig().getDouble(
                "piece." + pieceKey + "." + sideKey,
                plugin.getConfig().getDouble("piece.default." + sideKey, 0.25)
        );
        int quarterTurns = plugin.getConfig().getInt(
                "rotation." + pieceKey + "." + sideKey,
                plugin.getConfig().getInt("rotation.default." + sideKey, 0)
        );
        double yOffset = plugin.getConfig().getDouble(
                "offset." + pieceKey + "." + sideKey,
                plugin.getConfig().getDouble("offset.default." + sideKey, 0.0)
        );

        return new Visual((float) scale, quarterTurns * 90.0f, yOffset);
    }

    private static String pieceTypeKey(PieceType pieceType) {
        return switch (pieceType) {
            case KING -> "king";
            case QUEEN -> "queen";
            case ROOK -> "rook";
            case BISHOP -> "bishop";
            case KNIGHT -> "knight";
            case PAWN -> "pawn";
            default -> "default";
        };
    }

    public record Visual(float scale, float yawDegrees, double yOffset) {
    }
}
