package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.retrooper.packetevents.util.Vector3f;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RenderViewManager {
    private static final double VIEW_RADIUS = 3.0;

    private final MineChess plugin;
    private final GameManager gameManager;
    private final Map<UUID, GameViews> viewsByGame = new HashMap<>();

    public RenderViewManager(MineChess plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void stop() {
        for (GameViews views : viewsByGame.values()) {
            views.clear();
        }
        viewsByGame.clear();
    }

    public void removeGame(UUID gameId) {
        GameViews views = viewsByGame.remove(gameId);
        if (views != null) {
            views.clear();
        }
    }

    public void refreshGame(ChessGame game) {
        GameViews views = viewsByGame.get(game.getGameId());
        if (views == null) return;
        views.refresh(game);
    }

    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        for (GameViews views : viewsByGame.values()) {
            views.removePlayer(playerId);
        }
    }

    public void updateViewsNow(ChessGame game) {
        updateViewsForGame(game);
    }

    public void tick() {
        for (ChessGame game : gameManager.getActiveGames()) {
            updateViewsForGame(game);
        }
    }

    private void updateViewsForGame(ChessGame game) {
        Location center = game.getOrigin();
        Set<UUID> nearby = new HashSet<>();

        for (Player player : center.getWorld().getNearbyPlayers(center, VIEW_RADIUS)) {
            nearby.add(player.getUniqueId());
        }

        if (nearby.isEmpty() && !viewsByGame.containsKey(game.getGameId())) {
            return;
        }

        GameViews views = viewsByGame.computeIfAbsent(game.getGameId(), id -> new GameViews(plugin));
        views.syncPlayers(game, nearby);
    }

    private static final class GameViews {
        private final MineChess plugin;
        private final Map<UUID, PlayerView> playerViews = new HashMap<>();

        private GameViews(MineChess plugin) {
            this.plugin = plugin;
        }

        private void syncPlayers(ChessGame game, Set<UUID> nearby) {
            Iterator<Map.Entry<UUID, PlayerView>> it = playerViews.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, PlayerView> entry = it.next();
                if (!nearby.contains(entry.getKey())) {
                    entry.getValue().close();
                    it.remove();
                }
            }

            for (UUID playerId : nearby) {
                if (!playerViews.containsKey(playerId)) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        PlayerView view = new PlayerView(plugin, player);
                        view.spawnBoard(game);
                        view.refreshPieces(game);
                        view.announce();
                        playerViews.put(playerId, view);
                    }
                }
            }
        }

        private void refresh(ChessGame game) {
            for (PlayerView view : playerViews.values()) {
                view.refreshPieces(game);
                view.announce();
            }
        }

        private void removePlayer(UUID playerId) {
            PlayerView view = playerViews.remove(playerId);
            if (view != null) {
                view.close();
            }
        }

        private void clear() {
            for (PlayerView view : playerViews.values()) {
                view.close();
            }
            playerViews.clear();
        }
    }

    private static final class PlayerView {
        private final ViewSpace space;
        private final List<Integer> boardDisplays = new ArrayList<>();
        private final List<Integer> pieceDisplays = new ArrayList<>();
        private String lastFen;
        private String lastPendingKey;
        private static final Vector3f BOARD_SCALE = new Vector3f(0.25f, 0.05f, 0.25f);
        private static final Vector3f PIECE_SCALE = new Vector3f(0.25f, 0.25f, 0.25f);

        private PlayerView(MineChess plugin, Player player) {
            this.space = new ViewSpace(plugin, player);
        }

        private void spawnBoard(ChessGame game) {
            Location baseLoc = game.getOrigin();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Location cellLoc = baseLoc.clone().add(col / 4.0, 0, row / 4.0);
                    Material material = (row + col) % 2 == 0 ? Material.BIRCH_PLANKS : Material.DARK_OAK_PLANKS;
                    int displayId = space.spawnBlockDisplay(cellLoc, material, BOARD_SCALE);
                    boardDisplays.add(displayId);
                }
            }
        }

        private void refreshPieces(ChessGame game) {
            String fen = game.getBoard().getFen();
            String pendingKey = buildPendingKey(game.getPendingPromotion());
            if (fen.equals(lastFen) && Objects.equals(pendingKey, lastPendingKey)) {
                return;
            }
            lastFen = fen;
            lastPendingKey = pendingKey;

            for (int displayId : pieceDisplays) {
                space.destroy(displayId);
            }
            pieceDisplays.clear();

            ChessGame.PendingPromotion pending = game.getPendingPromotion();

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (pending != null) {
                        int[] from = ChessMapping.toCoords(pending.from());
                        int[] to = ChessMapping.toCoords(pending.to());
                        if (row == from[0] && col == from[1]) {
                            continue;
                        }
                        if (row == to[0] && col == to[1]) {
                            Piece pawn = pending.side() == Side.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
                            spawnPiece(game, row, col, pawn);
                            continue;
                        }
                    }

                    Piece piece = game.getBoard().getPiece(ChessMapping.toSquare(row, col));
                    if (piece == Piece.NONE) continue;

                    spawnPiece(game, row, col, piece);
                }
            }
        }

        private String buildPendingKey(ChessGame.PendingPromotion pending) {
            if (pending == null) return "";
            return pending.from().name() + "-" + pending.to().name() + "-" + pending.side().name();
        }

        private void spawnPiece(ChessGame game, int row, int col, Piece piece) {
            int cmd = ChessMapping.toModelData(piece);
            Location loc = game.getOrigin().clone().add(col / 4.0 + 0.125, 0.137, row / 4.0 + 0.125);

            if (cmd == 2) loc.setYaw(-90f);
            else if (cmd == 8) loc.setYaw(90f);
            else loc.setYaw(piece.getPieceSide().isWhite() ? 0f : 180f);

            ItemStack item = new ItemStack(Material.TORCH);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(cmd);
                item.setItemMeta(meta);
            }
            int displayId = space.spawnItemDisplay(loc, item, PIECE_SCALE);
            pieceDisplays.add(displayId);
        }

        private void announce() {
            space.announce();
        }

        private void close() {
            for (int displayId : pieceDisplays) {
                space.destroy(displayId);
            }
            pieceDisplays.clear();
            for (int displayId : boardDisplays) {
                space.destroy(displayId);
            }
            boardDisplays.clear();
            space.announce();
            space.close();
        }
    }
}
