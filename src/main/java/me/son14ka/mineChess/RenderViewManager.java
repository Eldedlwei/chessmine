package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.retrooper.packetevents.util.Vector3f;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RenderViewManager {
    private static final double VIEW_RADIUS = 50.0;

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
        private final MineChess plugin;
        private final ViewSpace space;
        private final List<Integer> boardDisplays = new ArrayList<>();
        private final Map<Integer, Integer> pieceDisplays = new HashMap<>();
        private final Map<Integer, PieceRender> pieceRenders = new HashMap<>();
        private String lastFen;
        private String lastPendingKey;
        private static final Vector3f BOARD_SCALE = new Vector3f((float) BoardGeometry.CELL_SIZE, 0.1f, (float) BoardGeometry.CELL_SIZE);

        private PlayerView(MineChess plugin, Player player) {
            this.plugin = plugin;
            this.space = new ViewSpace(plugin, player);
        }

        private void spawnBoard(ChessGame game) {
            Location baseLoc = game.getOrigin();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Location cellLoc = BoardGeometry.cellCorner(baseLoc, row, col);
                    Material material = (row + col) % 2 == 0 ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE;
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

            Map<Integer, PieceRender> target = buildTargetRenders(game);
            Set<Integer> cells = new HashSet<>();
            cells.addAll(pieceDisplays.keySet());
            cells.addAll(target.keySet());

            for (Integer cell : cells) {
                PieceRender currentRender = pieceRenders.get(cell);
                PieceRender targetRender = target.get(cell);
                Integer currentDisplay = pieceDisplays.get(cell);

                if (targetRender == null) {
                    if (currentDisplay != null) {
                        space.destroy(currentDisplay);
                    }
                    pieceDisplays.remove(cell);
                    pieceRenders.remove(cell);
                    continue;
                }

                if (targetRender.equals(currentRender) && currentDisplay != null) {
                    continue;
                }

                if (currentDisplay != null) {
                    space.destroy(currentDisplay);
                }

                int row = cell / 8;
                int col = cell % 8;
                int displayId = spawnPiece(game, row, col, targetRender);
                pieceDisplays.put(cell, displayId);
                pieceRenders.put(cell, targetRender);
            }
        }

        private String buildPendingKey(ChessGame.PendingPromotion pending) {
            if (pending == null) return "";
            return pending.from().name() + "-" + pending.to().name() + "-" + pending.side().name();
        }

        private Map<Integer, PieceRender> buildTargetRenders(ChessGame game) {
            Map<Integer, PieceRender> target = new HashMap<>();
            ChessGame.PendingPromotion pending = game.getPendingPromotion();
            int pendingFromRow = -1;
            int pendingFromCol = -1;
            int pendingToRow = -1;
            int pendingToCol = -1;
            Side pendingSide = null;
            if (pending != null) {
                int[] pendingFrom = ChessMapping.toCoords(pending.from());
                int[] pendingTo = ChessMapping.toCoords(pending.to());
                pendingFromRow = pendingFrom[0];
                pendingFromCol = pendingFrom[1];
                pendingToRow = pendingTo[0];
                pendingToCol = pendingTo[1];
                pendingSide = pending.side();
            }

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece piece = game.getBoard().getPiece(ChessMapping.toSquare(row, col));

                    if (pendingSide != null) {
                        if (row == pendingFromRow && col == pendingFromCol) {
                            piece = Piece.NONE;
                        } else if (row == pendingToRow && col == pendingToCol) {
                            piece = pendingSide == Side.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
                        }
                    }

                    if (piece == Piece.NONE) {
                        continue;
                    }

                    int cell = row * 8 + col;
                    target.put(cell, toRender(piece));
                }
            }
            return target;
        }

        private PieceRender toRender(Piece piece) {
            int cmd = ChessMapping.toModelData(piece);
            PieceVisuals.Visual visual = PieceVisuals.resolve(plugin, piece);
            return new PieceRender(piece, cmd, visual.yawDegrees(), visual.yOffset(), visual.scale());
        }

        private int spawnPiece(ChessGame game, int row, int col, PieceRender render) {
            Location loc = BoardGeometry.cellCenter(game.getOrigin(), row, col, BoardGeometry.PIECE_BASE_Y + render.yOffset());
            loc.setYaw(render.yaw());

            ItemStack item = plugin.getCraftEngineItems().createPieceItem(render.piece());
            if (item == null) {
                item = new ItemStack(Material.TORCH);
                var meta = item.getItemMeta();
                if (meta != null) {
                    var modelData = meta.getCustomModelDataComponent();
                    modelData.setFloats(List.of((float) render.cmd()));
                    meta.setCustomModelDataComponent(modelData);
                    item.setItemMeta(meta);
                }
            }
            Vector3f scale = new Vector3f(render.scale(), render.scale(), render.scale());
            return space.spawnItemDisplay(loc, item, scale);
        }

        private void announce() {
            space.announce();
        }

        private void close() {
            for (int displayId : pieceDisplays.values()) {
                space.destroy(displayId);
            }
            pieceDisplays.clear();
            pieceRenders.clear();
            for (int displayId : boardDisplays) {
                space.destroy(displayId);
            }
            boardDisplays.clear();
            space.announce();
            space.close();
        }

        private record PieceRender(Piece piece, int cmd, float yaw, double yOffset, float scale) {
        }
    }
}
