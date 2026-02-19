package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameStorage {
    private final String jdbcUrl;

    public GameStorage(File dataFolder) {
        this.jdbcUrl = "jdbc:h2:file:" + new File(dataFolder, "minechess").getAbsolutePath();
    }

    public void init() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS games (
                    game_id VARCHAR(36) PRIMARY KEY,
                    world VARCHAR(128) NOT NULL,
                    origin_x DOUBLE NOT NULL,
                    origin_y DOUBLE NOT NULL,
                    origin_z DOUBLE NOT NULL,
                    fen VARCHAR(256) NOT NULL,
                    pending_from VARCHAR(8),
                    pending_to VARCHAR(8),
                    pending_side VARCHAR(8),
                    waiting_promotion BOOLEAN NOT NULL,
                    game_over BOOLEAN NOT NULL,
                    white_uuid VARCHAR(36),
                    black_uuid VARCHAR(36),
                    white_time BIGINT NOT NULL,
                    black_time BIGINT NOT NULL,
                    increment BIGINT NOT NULL,
                    white_bet DOUBLE NOT NULL,
                    black_bet DOUBLE NOT NULL,
                    white_bet_confirmed BOOLEAN NOT NULL,
                    black_bet_confirmed BOOLEAN NOT NULL,
                    bet_locked BOOLEAN NOT NULL,
                    started BOOLEAN NOT NULL
                )
            """);
        }
    }

    public void saveGame(ChessGame game) {
        String sql = """
            MERGE INTO games KEY(game_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, game.getGameId().toString());
            ps.setString(2, game.getOrigin().getWorld().getName());
            ps.setDouble(3, game.getOrigin().getX());
            ps.setDouble(4, game.getOrigin().getY());
            ps.setDouble(5, game.getOrigin().getZ());
            ps.setString(6, game.getBoard().getFen());

            ChessGame.PendingPromotion pending = game.getPendingPromotion();
            ps.setString(7, pending != null ? pending.from().name() : null);
            ps.setString(8, pending != null ? pending.to().name() : null);
            ps.setString(9, pending != null ? pending.side().name() : null);
            ps.setBoolean(10, game.isWaitingForPromotion());
            ps.setBoolean(11, game.isGameOver());
            ps.setString(12, game.getWhitePlayer() != null ? game.getWhitePlayer().toString() : null);
            ps.setString(13, game.getBlackPlayer() != null ? game.getBlackPlayer().toString() : null);
            ps.setLong(14, game.getWhiteTimeMs());
            ps.setLong(15, game.getBlackTimeMs());
            ps.setLong(16, game.getIncrementMs());
            ps.setDouble(17, game.getWhiteBet());
            ps.setDouble(18, game.getBlackBet());
            ps.setBoolean(19, game.isWhiteBetConfirmed());
            ps.setBoolean(20, game.isBlackBetConfirmed());
            ps.setBoolean(21, game.isBetLocked());
            ps.setBoolean(22, game.isStarted());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save game", e);
        }
    }

    public void deleteGame(UUID gameId) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM games WHERE game_id = ?")) {
            ps.setString(1, gameId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete game", e);
        }
    }

    public List<StoredGame> loadGames() {
        List<StoredGame> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM games");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StoredGame game = new StoredGame(
                        UUID.fromString(rs.getString("game_id")),
                        rs.getString("world"),
                        rs.getDouble("origin_x"),
                        rs.getDouble("origin_y"),
                        rs.getDouble("origin_z"),
                        rs.getString("fen"),
                        rs.getString("pending_from"),
                        rs.getString("pending_to"),
                        rs.getString("pending_side"),
                        rs.getBoolean("waiting_promotion"),
                        rs.getBoolean("game_over"),
                        rs.getString("white_uuid"),
                        rs.getString("black_uuid"),
                        rs.getLong("white_time"),
                        rs.getLong("black_time"),
                        rs.getLong("increment"),
                        rs.getDouble("white_bet"),
                        rs.getDouble("black_bet"),
                        rs.getBoolean("white_bet_confirmed"),
                        rs.getBoolean("black_bet_confirmed"),
                        rs.getBoolean("bet_locked"),
                        rs.getBoolean("started")
                );
                result.add(game);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load games", e);
        }
        return result;
    }

    public record StoredGame(
            UUID gameId,
            String world,
            double x,
            double y,
            double z,
            String fen,
            String pendingFrom,
            String pendingTo,
            String pendingSide,
            boolean waitingPromotion,
            boolean gameOver,
            String whiteUuid,
            String blackUuid,
            long whiteTime,
            long blackTime,
            long increment,
            double whiteBet,
            double blackBet,
            boolean whiteBetConfirmed,
            boolean blackBetConfirmed,
            boolean betLocked,
            boolean started
    ) {
        public Side getPendingSide() {
            if (pendingSide == null) return null;
            return Side.valueOf(pendingSide);
        }
    }
}
