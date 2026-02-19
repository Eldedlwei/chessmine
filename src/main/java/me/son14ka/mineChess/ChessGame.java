package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class ChessGame {
    private final UUID gameId;
    private final Location origin;
    private final Board board = new Board();
    private PendingPromotion pendingPromotion;
    private boolean isWaitingForPromotion = false;
    private boolean isGameOver = false;
    private UUID whitePlayer;
    private UUID blackPlayer;
    private long whiteTimeMs;
    private long blackTimeMs;
    private long incrementMs;
    private double whiteBet;
    private double blackBet;
    private boolean whiteBetConfirmed;
    private boolean blackBetConfirmed;
    private boolean betLocked;
    private boolean started;
    private final MoveCache moveCache = new MoveCache();

    public ChessGame(UUID gameId, Location origin) {
        this.gameId = gameId;
        this.origin = origin;
    }

    public void setWaitingForPromotion(boolean waiting) { isWaitingForPromotion = waiting; }
    public boolean isGameOver() { return isGameOver; }
    public void setGameOver(boolean gameOver) { isGameOver = gameOver; }
    public UUID getGameId() { return gameId; }
    public Location getOrigin() { return origin; }
    public Board getBoard() { return board; }
    public boolean isWaitingForPromotion() { return isWaitingForPromotion; }
    public PendingPromotion getPendingPromotion() { return pendingPromotion; }
    public void setPendingPromotion(PendingPromotion pendingPromotion) { this.pendingPromotion = pendingPromotion; }
    public List<Move> getLegalMovesFrom(Square from) { return moveCache.getMoves(board, from); }
    public UUID getWhitePlayer() { return whitePlayer; }
    public UUID getBlackPlayer() { return blackPlayer; }
    public void setWhitePlayer(UUID whitePlayer) { this.whitePlayer = whitePlayer; }
    public void setBlackPlayer(UUID blackPlayer) { this.blackPlayer = blackPlayer; }
    public boolean hasBothPlayers() { return whitePlayer != null && blackPlayer != null; }
    public UUID getPlayer(Side side) { return side == Side.WHITE ? whitePlayer : blackPlayer; }
    public Side getPlayerSide(UUID playerId) {
        if (playerId == null) return null;
        if (playerId.equals(whitePlayer)) return Side.WHITE;
        if (playerId.equals(blackPlayer)) return Side.BLACK;
        return null;
    }
    public boolean isPlayer(UUID playerId) { return getPlayerSide(playerId) != null; }
    public long getWhiteTimeMs() { return whiteTimeMs; }
    public long getBlackTimeMs() { return blackTimeMs; }
    public void setWhiteTimeMs(long whiteTimeMs) { this.whiteTimeMs = whiteTimeMs; }
    public void setBlackTimeMs(long blackTimeMs) { this.blackTimeMs = blackTimeMs; }
    public long getIncrementMs() { return incrementMs; }
    public void setIncrementMs(long incrementMs) { this.incrementMs = incrementMs; }
    public double getWhiteBet() { return whiteBet; }
    public double getBlackBet() { return blackBet; }
    public void setWhiteBet(double whiteBet) { this.whiteBet = whiteBet; }
    public void setBlackBet(double blackBet) { this.blackBet = blackBet; }
    public boolean isWhiteBetConfirmed() { return whiteBetConfirmed; }
    public boolean isBlackBetConfirmed() { return blackBetConfirmed; }
    public void setWhiteBetConfirmed(boolean whiteBetConfirmed) { this.whiteBetConfirmed = whiteBetConfirmed; }
    public void setBlackBetConfirmed(boolean blackBetConfirmed) { this.blackBetConfirmed = blackBetConfirmed; }
    public boolean isBetLocked() { return betLocked; }
    public void setBetLocked(boolean betLocked) { this.betLocked = betLocked; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public boolean areBetsConfirmed() { return whiteBetConfirmed && blackBetConfirmed; }
    public void addIncrement(Side side) {
        if (side == Side.WHITE) whiteTimeMs += incrementMs;
        else blackTimeMs += incrementMs;
    }
    public void decrementTime(Side side, long deltaMs) {
        if (side == Side.WHITE) whiteTimeMs = Math.max(0, whiteTimeMs - deltaMs);
        else blackTimeMs = Math.max(0, blackTimeMs - deltaMs);
    }

    public String getCurrentTurn() {
        return ChessMapping.sideToColor(board.getSideToMove());
    }

    public Side getCurrentTurnSide() {
        return board.getSideToMove();
    }

    public record PendingPromotion(Square from, Square to, Side side) {
    }

    private static final class MoveCache {
        private String fen;
        private Map<Square, List<Move>> byFrom = new EnumMap<>(Square.class);

        private List<Move> getMoves(Board board, Square from) {
            String currentFen = board.getFen();
            if (!currentFen.equals(fen)) {
                rebuild(board);
            }
            List<Move> moves = byFrom.get(from);
            return moves != null ? moves : List.of();
        }

        private void rebuild(Board board) {
            fen = board.getFen();
            byFrom = new EnumMap<>(Square.class);
            for (Move move : board.legalMoves()) {
                byFrom.computeIfAbsent(move.getFrom(), k -> new ArrayList<>()).add(move);
            }
        }
    }
}
