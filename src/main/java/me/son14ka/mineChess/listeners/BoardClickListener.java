package me.son14ka.mineChess.listeners;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.retrooper.packetevents.util.Vector3f;
import me.son14ka.mineChess.ChessGame;
import me.son14ka.mineChess.ChessMapping;
import me.son14ka.mineChess.GameManager;
import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.RenderViewManager;
import me.son14ka.mineChess.GameEconomy;
import me.son14ka.mineChess.GameStorage;
import me.son14ka.mineChess.PromotionSpawner;
import me.son14ka.mineChess.MessageService;
import me.son14ka.mineChess.ViewSpace;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BoardClickListener implements Listener {
    private static final Vector3f HIGHLIGHT_SCALE = new Vector3f(0.25f, 0.25f, 0.25f);

    private final MineChess plugin;
    private final GameManager gameManager;
    private final RenderViewManager renderViewManager;
    private final GameStorage storage;
    private final MessageService messages;

    private final Map<UUID, int[]> selectedCells = new HashMap<>();
    private final Map<UUID, HighlightSpace> activeHighlights = new HashMap<>();
    private final Map<UUID, HighlightKey> highlightKeys = new HashMap<>();

    public BoardClickListener(MineChess plugin, GameManager gameManager, RenderViewManager renderViewManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.renderViewManager = renderViewManager;
        this.storage = plugin.getGameStorage();
        this.messages = plugin.getMessageService();
    }

    @EventHandler
    public void onBoardClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        if (!event.getPlayer().hasPermission("minechess.play")) {
            event.getPlayer().sendMessage(messages.msg(event.getPlayer(), "no_permission"));
            return;
        }

        var pdc = interaction.getPersistentDataContainer();
        Player player = event.getPlayer();
        NamespacedKey promotionKey = new NamespacedKey(plugin, "promotion_cmd");

        if (interaction.getPersistentDataContainer().has(promotionKey, PersistentDataType.INTEGER)) {
            int cmd = pdc.get(promotionKey, PersistentDataType.INTEGER);
            int row = pdc.get(new NamespacedKey(plugin, "promotion_row"), PersistentDataType.INTEGER);
            int col = pdc.get(new NamespacedKey(plugin, "promotion_col"), PersistentDataType.INTEGER);
            UUID gameId = UUID.fromString(pdc.get(new NamespacedKey(plugin, "game_id"), PersistentDataType.STRING));

            ChessGame game = gameManager.getGame(gameId);
            if (game == null) return;

            completePromotion(game, row, col, cmd);

            cleanupPromotionEntities(interaction.getWorld(), gameId);

            game.setWaitingForPromotion(false);
            game.setPendingPromotion(null);

            handlePostMoveState(game, player);
            renderViewManager.refreshGame(game);
            if (storage != null) storage.saveGame(game);
        } else {
            NamespacedKey gameKey = new NamespacedKey(plugin, "game_id");
            if (!pdc.has(gameKey, PersistentDataType.STRING)) return;

            UUID gameId = UUID.fromString(pdc.get(gameKey, PersistentDataType.STRING));
            ChessGame game = gameManager.getGame(gameId);
            if (game == null) return;

            if (game.isGameOver()) {
                event.getPlayer().sendMessage(messages.msg(player, "game_over"));
                return;
            }
            if (game.isWaitingForPromotion()) {
                event.getPlayer().sendMessage(messages.msg(player, "promotion_choice"));
                return;
            }

            int row = pdc.get(new NamespacedKey(plugin, "chess_row"), PersistentDataType.INTEGER);
            int col = pdc.get(new NamespacedKey(plugin, "chess_col"), PersistentDataType.INTEGER);

            handleInteraction(player, game, row, col);
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
                Component playerColor = game.getCurrentTurn().equals("white") ? messages.msg(player, "white") : messages.msg(player, "black");
                player.sendMessage(messages.msg(player, "turn_info", Placeholder.component("color", playerColor)));
                return;
            }
            Side playerSide = game.getPlayerSide(player.getUniqueId());
            if (playerSide != null && playerSide != board.getSideToMove()) {
                Component playerColor = game.getCurrentTurn().equals("white") ? messages.msg(player, "white") : messages.msg(player, "black");
                player.sendMessage(messages.msg(player, "turn_info", Placeholder.component("color", playerColor)));
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
            if (storage != null) storage.saveGame(game);
            return;
        }

        board.doMove(move, true);
        Side moverSide = board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE;
        game.addIncrement(moverSide);

        movePieceEntity(game, fromR, fromC, toR, toC, isCapture, isEnPassant ? fromR : toR, toC);

        if (isCastle) {
            moveRookForCastle(game, fromR, fromC, toC);
        }

        handlePostMoveState(game, player);
        renderViewManager.refreshGame(game);
        if (storage != null) storage.saveGame(game);
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
        Location newLoc = game.getOrigin().clone().add(toC / 4.0 + 0.125, 0.137, toR / 4.0 + 0.125);
        newLoc.getWorld().playSound(newLoc, Sound.BLOCK_WOOD_PLACE, 1.0f, 1.0f);

        if (isCapture) {
            Location captureLoc = game.getOrigin().clone().add(captureC / 4.0 + 0.125, 0.137, captureR / 4.0 + 0.125);
            captureLoc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, captureLoc.add(0, 0.1, 0), 10, 0.1, 0.1, 0.1, 0.05);
            captureLoc.getWorld().playSound(captureLoc, Sound.ENTITY_GENERIC_HURT, 1.0f, 1.0f);
        }
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

            Location loc = game.getOrigin().clone().add(c / 4.0 + 0.125, 0.051, r / 4.0 + 0.125);

            int displayId = playerSpace.spawnItemDisplay(loc, new ItemStack(Material.LIME_STAINED_GLASS_PANE), HIGHLIGHT_SCALE);
            highlights.add(displayId);
        }
        playerSpace.announce();
        highlightSpace.displays = highlights;
        if (newKey != null) {
            highlightKeys.put(player.getUniqueId(), newKey);
        }
    }

    private void clearHighlights(Player player) {
        HighlightSpace highlights = activeHighlights.remove(player.getUniqueId());
        if (highlights != null) {
            highlights.displays.forEach(highlights.space::destroy);
            highlights.space.announce();
            activeHighlights.put(player.getUniqueId(), highlights);
        }
        highlightKeys.remove(player.getUniqueId());
    }

    public void closeHighlights(UUID playerId) {
        HighlightSpace highlights = activeHighlights.remove(playerId);
        if (highlights != null) {
            highlights.displays.forEach(highlights.space::destroy);
            highlights.space.announce();
            highlights.space.close();
        }
        highlightKeys.remove(playerId);
    }

    private void cleanupPromotionEntities(@NotNull World world, UUID gameId) {
        String idString = gameId.toString();
        NamespacedKey gameKey = new NamespacedKey(plugin, "game_id");
        NamespacedKey promoItemKey = new NamespacedKey(plugin, "is_promotion_item");
        NamespacedKey promoCmdKey = new NamespacedKey(plugin, "promotion_cmd");

        for (Entity entity : world.getEntities()) {
            var pdc = entity.getPersistentDataContainer();

            if (pdc.has(gameKey, PersistentDataType.STRING)) {
                String storedId = pdc.get(gameKey, PersistentDataType.STRING);

                if (idString.equals(storedId)) {
                    if (pdc.has(promoItemKey, PersistentDataType.BYTE) ||
                            pdc.has(promoCmdKey, PersistentDataType.INTEGER)) {

                        entity.remove();
                    }
                }
            }
        }
    }

    private void completePromotion(ChessGame game, int row, int col, int cmd) {
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

        if (storage != null) storage.saveGame(game);
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
                Component winner = board.getSideToMove() == Side.WHITE ? messages.msg(player, "black") : messages.msg(player, "white");
                msg = messages.msg(player, "mate_broadcast", Placeholder.component("winner", winner));
                spawnVictoryFireworks(game.getOrigin());
                GameEconomy.payoutWinner(plugin, game, board.getSideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE);
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
            return;
        }

        if (inCheck) {
            Component colorName = board.getSideToMove() == Side.WHITE ? messages.msg(player, "white") : messages.msg(player, "black");
            Component msg = messages.msg(player, "check_notification", Placeholder.component("color", colorName));

            GameManager.broadcastToGame(game, msg);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_PLACE, 1.0f, 1.0f);
        }
    }

    private boolean ensurePlayerAssigned(ChessGame game, Player player) {
        if (game.getWhitePlayer() == null) {
            game.setWhitePlayer(player.getUniqueId());
            game.setWhiteTimeMs(getInitialTimeMs());
            game.setIncrementMs(getIncrementMs());
            if (storage != null) storage.saveGame(game);
            return true;
        }
        if (game.getBlackPlayer() == null && !player.getUniqueId().equals(game.getWhitePlayer())) {
            game.setBlackPlayer(player.getUniqueId());
            game.setBlackTimeMs(getInitialTimeMs());
            game.setIncrementMs(getIncrementMs());
            if (storage != null) storage.saveGame(game);
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
        if (storage != null) storage.saveGame(game);
        return true;
    }

    private long getInitialTimeMs() {
        return plugin.getConfig().getLong("clock.initial-seconds", 300L) * 1000L;
    }

    private long getIncrementMs() {
        return plugin.getConfig().getLong("clock.increment-seconds", 3L) * 1000L;
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

    public static Component getMsg(Player player, String path, MineChess plugin) {
        return plugin.getMessageService().msg(player, path);
    }
}
