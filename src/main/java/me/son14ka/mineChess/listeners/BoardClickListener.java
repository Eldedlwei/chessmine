package me.son14ka.mineChess.listeners;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.retrooper.packetevents.util.Vector3f;
import me.son14ka.mineChess.BoardGeometry;
import me.son14ka.mineChess.ChessGame;
import me.son14ka.mineChess.ChessMapping;
import me.son14ka.mineChess.GameManager;
import me.son14ka.mineChess.MineChessKeys;
import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.RenderViewManager;
import me.son14ka.mineChess.GameEconomy;
import me.son14ka.mineChess.GameStorage;
import me.son14ka.mineChess.PromotionSpawner;
import me.son14ka.mineChess.MessageService;
import me.son14ka.mineChess.ViewSpace;
import org.bukkit.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BoardClickListener implements Listener {
    private static final Vector3f HIGHLIGHT_SCALE = new Vector3f((float) BoardGeometry.CELL_SIZE, (float) BoardGeometry.CELL_SIZE, (float) BoardGeometry.CELL_SIZE);

    private final MineChess plugin;
    private final GameManager gameManager;
    private final RenderViewManager renderViewManager;
    private final GameStorage storage;
    private final MessageService messages;
    private final MineChessKeys keys;

    private final Map<UUID, int[]> selectedCells = new HashMap<>();
    private final Map<UUID, HighlightSpace> activeHighlights = new HashMap<>();
    private final Map<UUID, HighlightKey> highlightKeys = new HashMap<>();

    public BoardClickListener(MineChess plugin, GameManager gameManager, RenderViewManager renderViewManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.renderViewManager = renderViewManager;
        this.storage = plugin.getGameStorage();
        this.messages = plugin.getMessageService();
        this.keys = plugin.getKeys();
    }

    @EventHandler
    public void onBoardClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("minechess.play")) {
            player.sendMessage(messages.msg(player, "no_permission"));
            return;
        }

        PromotionChoiceClick promotionClick = readPromotionChoice(interaction);
        if (promotionClick != null) {
            ChessGame game = gameManager.getGame(promotionClick.gameId());
            if (game == null) return;

            completePromotion(game, player, promotionClick.cmd());
            PromotionSpawner.cleanupPromotionEntities(plugin, interaction.getWorld(), promotionClick.gameId());

            game.setWaitingForPromotion(false);
            game.setPendingPromotion(null);

            handlePostMoveState(game, player);
            renderViewManager.refreshGame(game);
            saveGame(game);
            return;
        }

        BoardCellClick boardClick = readBoardCell(interaction);
        if (boardClick == null) return;

        ChessGame game = gameManager.getGame(boardClick.gameId());
        if (game == null) return;

        if (game.isGameOver()) {
            player.sendMessage(messages.msg(player, "game_over"));
            return;
        }
        if (game.isWaitingForPromotion()) {
            player.sendMessage(messages.msg(player, "promotion_choice"));
            return;
        }

        handleInteraction(player, game, boardClick.row(), boardClick.col());
    }

    private PromotionChoiceClick readPromotionChoice(Interaction interaction) {
        var pdc = interaction.getPersistentDataContainer();
        Integer cmd = pdc.get(keys.promotionCmd(), PersistentDataType.INTEGER);
        if (cmd == null) {
            return null;
        }

        if (!pdc.has(keys.promotionRow(), PersistentDataType.INTEGER)
                || !pdc.has(keys.promotionCol(), PersistentDataType.INTEGER)) {
            return null;
        }
        String gameId = pdc.get(keys.gameId(), PersistentDataType.STRING);
        if (gameId == null) {
            return null;
        }

        try {
            return new PromotionChoiceClick(UUID.fromString(gameId), cmd);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private BoardCellClick readBoardCell(Interaction interaction) {
        var pdc = interaction.getPersistentDataContainer();
        Integer row = pdc.get(keys.chessRow(), PersistentDataType.INTEGER);
        Integer col = pdc.get(keys.chessCol(), PersistentDataType.INTEGER);
        String gameId = pdc.get(keys.gameId(), PersistentDataType.STRING);
        if (row == null || col == null || gameId == null) {
            return null;
        }

        try {
            return new BoardCellClick(UUID.fromString(gameId), row, col);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void handleInteraction(Player player, ChessGame game, int row, int col) {
        UUID uuid = player.getUniqueId();
        Board board = game.getBoard();

        int[] selected = selectedCells.get(uuid);

        if (!selectedCells.containsKey(uuid)) {
            if (!ensurePlayerAssigned(game, player)) return;
            if (!ensureGameStarted(game, player)) return;
            Piece piece = board.getPiece(ChessMapping.toSquare(row, col));
            if (piece == Piece.NONE) return;

            if (piece.getPieceSide() != board.getSideToMove()) {
                debugTurn(game, player, "reject_select_wrong_side");
                player.sendMessage(messages.msg(player, "turn_info", Placeholder.component("color", currentTurnColor(player, board))));
                return;
            }
            Side playerSide = game.getPlayerSide(player.getUniqueId());
            if (playerSide != null && playerSide != board.getSideToMove()) {
                debugTurn(game, player, "reject_player_not_turn");
                player.sendMessage(messages.msg(player, "turn_info", Placeholder.component("color", currentTurnColor(player, board))));
                return;
            }

            selectedCells.put(uuid, new int[]{row, col});
            Square from = ChessMapping.toSquare(row, col);
            List<Move> moves = game.getLegalMovesFrom(from);
            highlightMoves(player, game, moves);
        } else {
            int selRow = selected[0];
            int selCol = selected[1];

            if (selRow == row && selCol == col) {
                selectedCells.remove(uuid);
                clearHighlights(player);
                return;
            }

            Square fromSquare = ChessMapping.toSquare(selRow, selCol);
            Square toSquare = ChessMapping.toSquare(row, col);
            Piece movingPiece = board.getPiece(fromSquare);
            Piece targetPiece = board.getPiece(toSquare);

            if (movingPiece == Piece.NONE) {
                selectedCells.remove(uuid);
                clearHighlights(player);
                return;
            }

            if (targetPiece != Piece.NONE && targetPiece.getPieceSide() == movingPiece.getPieceSide()) {
                clearHighlights(player);
                selectedCells.put(uuid, new int[]{row, col});
                List<Move> moves = game.getLegalMovesFrom(toSquare);
                highlightMoves(player, game, moves);
                return;
            }

            List<Move> availableMoves = game.getLegalMovesFrom(fromSquare);
            List<Move> targetMoves = findMovesToSquare(availableMoves, toSquare);
            if (!targetMoves.isEmpty()) {
                selectedCells.remove(uuid);
                clearHighlights(player);
                executeMove(game, targetMoves, player);
            } else {
                player.sendMessage(messages.msg(player, "wrong_cell"));
            }
        }
    }

    private List<Move> findMovesToSquare(List<Move> moves, Square to) {
        List<Move> result = new ArrayList<>();
        for (Move move : moves) {
            if (move.getTo() == to) result.add(move);
        }
        return result;
    }

    private void executeMove(ChessGame game, List<Move> moves, Player player) {
        Board board = game.getBoard();

        Move move = moves.get(0);

        Square from = move.getFrom();
        Square to = move.getTo();

        int[] fromCoords = ChessMapping.toCoords(from);
        int[] toCoords = ChessMapping.toCoords(to);
        int fromR = fromCoords[0];
        int fromC = fromCoords[1];
        int toR = toCoords[0];
        int toC = toCoords[1];

        Piece movingPiece = board.getPiece(from);
        Piece targetPiece = board.getPiece(to);

        boolean isPawn = movingPiece.getPieceType() == PieceType.PAWN;
        boolean isEnPassant = isPawn && fromC != toC && targetPiece == Piece.NONE;
        boolean isCapture = targetPiece != Piece.NONE || isEnPassant;
        boolean isCastle = movingPiece.getPieceType() == PieceType.KING && Math.abs(fromC - toC) == 2;
        boolean isPromotion = false;
        for (Move m : moves) {
            if (m.getPromotion() != null && m.getPromotion() != Piece.NONE) {
                isPromotion = true;
                break;
            }
        }

        if (isPromotion) {
            movePieceEntity(game, fromR, fromC, toR, toC, isCapture, isEnPassant ? fromR : toR, toC);
            game.setPendingPromotion(new ChessGame.PendingPromotion(from, to, board.getSideToMove()));
            game.setWaitingForPromotion(true);
            PromotionSpawner.spawnPromotionChoices(plugin, game, toR, toC, board.getSideToMove() == Side.WHITE);
            renderViewManager.refreshGame(game);
            saveGame(game);
            return;
        }

        if (!board.doMove(move, true)) {
            player.sendMessage(messages.msg(player, "wrong_cell"));
            return;
        }
        Side moverSide = board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE;
        game.addIncrement(moverSide);
        debugTurn(game, player, "move_applied");

        movePieceEntity(game, fromR, fromC, toR, toC, isCapture, isEnPassant ? fromR : toR, toC);

        if (isCastle) {
            moveRookForCastle(game, fromR, fromC, toC);
        }

        handlePostMoveState(game, player);
        renderViewManager.refreshGame(game);
        saveGame(game);
    }

    private void spawnVictoryFireworks(Location origin) {
        Color black = Color.BLACK;
        Color white = Color.WHITE;

        for (int i = 0; i < 5; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                double xOffset = Math.random() * 2.0;
                double zOffset = Math.random() * 2.0;
                Location fireworkLoc = origin.clone().add(xOffset, 0.5, zOffset);

                for (Player nearby : getNearbyPlayers(origin, 3.0)) {
                    nearby.spawnParticle(Particle.FIREWORK, fireworkLoc, 30, 0.2, 0.2, 0.2, 0.02);
                    nearby.spawnParticle(Particle.FLASH, fireworkLoc, 1, 0, 0, 0, 0);
                    nearby.spawnParticle(Particle.DUST, fireworkLoc, 20, 0.25, 0.25, 0.25, 0.0,
                            new Particle.DustOptions(black, 1.0f));
                    nearby.spawnParticle(Particle.DUST, fireworkLoc, 20, 0.25, 0.25, 0.25, 0.0,
                            new Particle.DustOptions(white, 1.0f));
                    nearby.playSound(fireworkLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.3f, 1.0f);
                }

            }, i * 10L);
        }
    }

    private void spawnAmbientSmoke(Location origin) {
        int pulses = 8;

        for (int i = 0; i < pulses; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                for (Player nearby : getNearbyPlayers(origin, 3.0)) {
                    nearby.spawnParticle(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            origin, 3, 0.1, 0.1, 0.1, 0.1);
                    nearby.playSound(origin, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.8f);
                }

            }, i * 4L);
        }
    }

    private Collection<Player> getNearbyPlayers(Location origin, double radius) {
        return origin.getWorld().getNearbyPlayers(origin, radius);
    }

    private void movePieceEntity(ChessGame game, int fromR, int fromC, int toR, int toC, boolean isCapture, int captureR, int captureC) {
        Location newLoc = BoardGeometry.cellCenter(game.getOrigin(), toR, toC, BoardGeometry.PIECE_BASE_Y);
        newLoc.getWorld().playSound(newLoc, Sound.BLOCK_WOOD_PLACE, 1.0f, 1.0f);
    }

    private void highlightMoves(Player player, ChessGame game, List<Move> moves) {
        HighlightKey newKey = HighlightKey.from(game, moves);
        HighlightKey lastKey = highlightKeys.get(player.getUniqueId());
        if (newKey != null && newKey.equals(lastKey) && activeHighlights.containsKey(player.getUniqueId())) {
            return;
        }

        HighlightSpace highlightSpace = activeHighlights.get(player.getUniqueId());
        if (highlightSpace == null) {
            highlightSpace = new HighlightSpace(new ViewSpace(plugin, player), new ArrayList<>());
            activeHighlights.put(player.getUniqueId(), highlightSpace);
        }
        clearHighlights(player);

        List<Integer> highlights = new ArrayList<>();
        ViewSpace playerSpace = highlightSpace.space;
        Set<Square> seen = new HashSet<>();

        for (Move move : moves) {
            if (!seen.add(move.getTo())) {
                continue;
            }
            int[] coords = ChessMapping.toCoords(move.getTo());
            int r = coords[0];
            int c = coords[1];

            Location loc = BoardGeometry.cellCenter(game.getOrigin(), r, c, BoardGeometry.HIGHLIGHT_Y);
            loc.setPitch(90f);
            ItemStack highlightItem = plugin.getCraftEngineItems().createHighlightItem();
            if (highlightItem == null) {
                highlightItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            }
            int displayId = playerSpace.spawnItemDisplay(loc, highlightItem, HIGHLIGHT_SCALE);
            highlights.add(displayId);
        }
        playerSpace.announce();
        highlightSpace.displays = highlights;
        if (newKey != null) {
            highlightKeys.put(player.getUniqueId(), newKey);
        }
    }

    private void clearHighlights(Player player) {
        HighlightSpace highlights = activeHighlights.get(player.getUniqueId());
        if (highlights != null) {
            for (Integer displayId : highlights.displays) {
                if (displayId != null) {
                    highlights.space.destroy(displayId);
                }
            }
            highlights.space.announce();
            highlights.displays.clear();
        }
        highlightKeys.remove(player.getUniqueId());
    }

    public void closeHighlights(UUID playerId) {
        HighlightSpace highlights = activeHighlights.remove(playerId);
        if (highlights != null) {
            for (Integer displayId : highlights.displays) {
                if (displayId != null) {
                    highlights.space.destroy(displayId);
                }
            }
            highlights.space.announce();
            highlights.space.close();
        }
        highlightKeys.remove(playerId);
    }

    private void completePromotion(ChessGame game, Player player, int cmd) {
        ChessGame.PendingPromotion pending = game.getPendingPromotion();
        if (pending == null) return;

        Side side = pending.side();
        Piece promotion = ChessMapping.toPromotionPiece(cmd, side);
        Move move = new Move(pending.from(), pending.to(), promotion);

        if (!game.getBoard().isMoveLegal(move, true)) {
            plugin.getLogger().warning("Promotion move illegal: " + move);
            return;
        }

        if (!game.getBoard().doMove(move, true)) {
            plugin.getLogger().warning("Promotion move rejected: " + move);
            return;
        }
        game.addIncrement(side);
        debugTurn(game, player, "promotion_applied");

        saveGame(game);
    }

    private void moveRookForCastle(ChessGame game, int row, int fromCol, int kingToCol) {
        boolean isKingSide = kingToCol > fromCol;
        int rookFromCol = isKingSide ? 7 : 0;
        int rookToCol = isKingSide ? 5 : 3;
        movePieceEntity(game, row, rookFromCol, row, rookToCol, false, rookToCol, rookToCol);
    }

    private void handlePostMoveState(ChessGame game, Player player) {
        Board board = game.getBoard();
        boolean isMated = board.isMated();
        boolean isStalemate = board.isStaleMate();
        boolean isInsufficient = board.isInsufficientMaterial();
        boolean inCheck = board.isKingAttacked();

        if (isMated || isStalemate || isInsufficient) {
            game.setGameOver(true);

            Component msg;
            if (isMated) {
                Side winnerSide = board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE;
                Player winnerPlayer = getOnlinePlayer(game.getPlayer(winnerSide));
                Component winner = winnerPlayer != null
                        ? Component.text(winnerPlayer.getName())
                        : (winnerSide == Side.WHITE ? messages.msg(player, "white") : messages.msg(player, "black"));
                msg = messages.msg(player, "mate_broadcast", Placeholder.component("winner", winner));
                spawnVictoryFireworks(game.getOrigin());
                GameEconomy.payoutWinner(plugin, game, winnerSide);
            } else if (isStalemate) {
                msg = messages.msg(player, "stalemate_broadcast");
                spawnAmbientSmoke(game.getOrigin());
                GameEconomy.refundBets(plugin, game);
            } else {
                msg = messages.msg(player, "insufficient_material");
                spawnAmbientSmoke(game.getOrigin());
                GameEconomy.refundBets(plugin, game);
            }

            GameManager.broadcastToGame(game, msg);
            GameManager.broadcastToGame(game, messages.msg(player, "board_reset"));
            gameManager.resetGame(game);
            return;
        }

        if (inCheck) {
            Component colorName = board.getSideToMove() == Side.WHITE ? messages.msg(player, "white") : messages.msg(player, "black");
            Component msg = messages.msg(player, "check_notification", Placeholder.component("color", colorName));

            GameManager.broadcastToGame(game, msg);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_PLACE, 1.0f, 1.0f);
        }
    }

    private Component currentTurnColor(Player player, Board board) {
        return board.getSideToMove() == Side.WHITE ? messages.msg(player, "white") : messages.msg(player, "black");
    }

    private void debugTurn(ChessGame game, Player player, String stage) {
        if (!plugin.getConfig().getBoolean("debug.turn-logging", false)) {
            return;
        }
        Side turn = game.getBoard().getSideToMove();
        Side playerSide = game.getPlayerSide(player.getUniqueId());
        plugin.getLogger().info("[MineChess][turn] stage=" + stage
                + " game=" + game.getGameId()
                + " player=" + player.getName()
                + " playerSide=" + (playerSide == null ? "NONE" : playerSide.name())
                + " sideToMove=" + turn.name());
    }

    private boolean ensurePlayerAssigned(ChessGame game, Player player) {
        if (game.getWhitePlayer() == null) {
            game.setWhitePlayer(player.getUniqueId());
            game.setWhiteTimeMs(getInitialTimeMs());
            game.setIncrementMs(getIncrementMs());
            if (game.getWaitingStartSinceMs() <= 0L) {
                game.setWaitingStartSinceMs(System.currentTimeMillis());
            }
            player.sendMessage(messages.msg(player, "joined_white"));
            saveGame(game);
            return true;
        }
        if (game.getBlackPlayer() == null && !player.getUniqueId().equals(game.getWhitePlayer())) {
            game.setBlackPlayer(player.getUniqueId());
            game.setBlackTimeMs(getInitialTimeMs());
            game.setIncrementMs(getIncrementMs());
            if (game.getWaitingStartSinceMs() <= 0L) {
                game.setWaitingStartSinceMs(System.currentTimeMillis());
            }
            player.sendMessage(messages.msg(player, "joined_black"));
            saveGame(game);
            return true;
        }
        if (!game.isPlayer(player.getUniqueId())) {
            player.sendMessage(messages.msg(player, "not_player"));
            return false;
        }
        return true;
    }

    private boolean ensureGameStarted(ChessGame game, Player player) {
        if (game.isStarted()) return true;
        if (!game.hasBothPlayers()) {
            player.sendMessage(messages.msg(player, "need_opponent"));
            return false;
        }
        if (plugin.getEconomy() == null) {
            game.setStarted(true);
            game.setWaitingStartSinceMs(0L);
            saveGame(game);
            return true;
        }
        if (!game.areBetsConfirmed()) {
            player.sendMessage(messages.msg(player, "need_bets"));
            return false;
        }
        if (!game.isBetLocked()) {
            if (!GameEconomy.lockBets(plugin, game)) {
                player.sendMessage(messages.msg(player, "bet_insufficient"));
                return false;
            }
        }
        game.setStarted(true);
        game.setWaitingStartSinceMs(0L);
        saveGame(game);
        return true;
    }

    private void saveGame(ChessGame game) {
        if (storage != null) {
            storage.saveGame(game);
        }
    }

    private long getInitialTimeMs() {
        return plugin.getConfig().getLong("clock.initial-seconds", 300L) * 1000L;
    }

    private long getIncrementMs() {
        return plugin.getConfig().getLong("clock.increment-seconds", 3L) * 1000L;
    }

    private Player getOnlinePlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return plugin.getServer().getPlayer(playerId);
    }

    private static final class HighlightSpace {
        private final ViewSpace space;
        private List<Integer> displays;

        private HighlightSpace(ViewSpace space, List<Integer> displays) {
            this.space = space;
            this.displays = displays;
        }
    }

    private record HighlightKey(String fen, Square from) {
        private static HighlightKey from(ChessGame game, List<Move> moves) {
            if (moves.isEmpty()) return null;
            Square from = moves.get(0).getFrom();
            return new HighlightKey(game.getBoard().getFen(), from);
        }
    }

    private record PromotionChoiceClick(UUID gameId, int cmd) {
    }

    private record BoardCellClick(UUID gameId, int row, int col) {
    }

    public static Component getMsg(Player player, String path, MineChess plugin) {
        return plugin.getMessageService().msg(player, path);
    }
}
