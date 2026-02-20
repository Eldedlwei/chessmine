package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

public final class CraftEngineItemService {
    private final MineChess plugin;
    private final Map<Piece, String> pieceItemIds = new EnumMap<>(Piece.class);
    private String boardItemId;
    private String tutorialBookItemId;
    private String highlightItemId;

    private boolean configEnabled;
    private boolean craftEngineReady;
    private Method keyOfMethod;
    private Method byIdMethod;
    private Method getCustomItemIdMethod;

    public CraftEngineItemService(MineChess plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        configEnabled = plugin.getConfig().getBoolean("craft-engine.enabled", false);
        boardItemId = readId("craft-engine.items.board", "minechess:board");
        tutorialBookItemId = readId("craft-engine.items.book", "minechess:tutorial_book");
        highlightItemId = readId("craft-engine.items.highlight", "minechess:highlight");

        pieceItemIds.clear();
        for (Piece piece : Piece.values()) {
            if (piece == Piece.NONE || piece.getPieceType() == null || piece.getPieceSide() == null) {
                continue;
            }
            String side = piece.getPieceSide() == Side.WHITE ? "white" : "black";
            String pieceType = pieceTypePath(piece.getPieceType());
            String configPath = "craft-engine.items.pieces." + side + "." + pieceType;
            String fallback = "minechess:" + side + "_" + pieceType;
            pieceItemIds.put(piece, readId(configPath, fallback));
        }

        craftEngineReady = false;
        keyOfMethod = null;
        byIdMethod = null;
        getCustomItemIdMethod = null;

        if (!configEnabled) {
            return;
        }

        Plugin craftEngine = plugin.getServer().getPluginManager().getPlugin("CraftEngine");
        if (craftEngine == null || !craftEngine.isEnabled()) {
            plugin.getLogger().warning("CraftEngine integration is enabled in config but CraftEngine is not loaded; using fallback items.");
            return;
        }

        try {
            Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
            Class<?> itemsApiClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            keyOfMethod = keyClass.getMethod("of", String.class);
            byIdMethod = itemsApiClass.getMethod("byId", keyClass);
            getCustomItemIdMethod = itemsApiClass.getMethod("getCustomItemId", ItemStack.class);
            craftEngineReady = true;
        } catch (Throwable error) {
            plugin.getLogger().warning("Failed to initialize CraftEngine API bridge; using fallback items. " + error.getMessage());
        }
    }

    public boolean isUsingCraftEngine() {
        return craftEngineReady;
    }

    public ItemStack createBoardItem() {
        return build(boardItemId);
    }

    public boolean isBoardItem(ItemStack stack) {
        return hasCustomId(stack, boardItemId);
    }

    public ItemStack createTutorialBookItem() {
        return build(tutorialBookItemId);
    }

    public boolean isTutorialBookItem(ItemStack stack) {
        return hasCustomId(stack, tutorialBookItemId);
    }

    public ItemStack createHighlightItem() {
        return build(highlightItemId);
    }

    public ItemStack createPieceItem(Piece piece) {
        if (piece == null || piece == Piece.NONE) {
            return null;
        }
        String pieceId = pieceItemIds.get(piece);
        if (pieceId == null) {
            return null;
        }
        return build(pieceId);
    }

    public ItemStack createPromotionChoiceItem(int modelCmd, Side side) {
        PieceType pieceType = ChessMapping.pieceTypeFromModel(modelCmd, side);
        if (pieceType == null) {
            return null;
        }
        return createPieceItem(ChessMapping.toPiece(side, pieceType));
    }

    private boolean hasCustomId(ItemStack stack, String expectedId) {
        if (!craftEngineReady || expectedId == null || stack == null || stack.getType().isAir()) {
            return false;
        }
        try {
            Object customId = getCustomItemIdMethod.invoke(null, stack);
            return customId != null && expectedId.equals(String.valueOf(customId));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ItemStack build(String id) {
        if (!craftEngineReady || id == null || id.isBlank()) {
            return null;
        }
        try {
            Object key = keyOfMethod.invoke(null, id);
            Object customItem = byIdMethod.invoke(null, key);
            if (customItem == null) {
                return null;
            }
            Object built = invokeBuild(customItem);
            if (built instanceof ItemStack stack) {
                return stack;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeBuild(Object customItem) throws Exception {
        try {
            Method buildMethod = customItem.getClass().getMethod("buildItemStack");
            return buildMethod.invoke(customItem);
        } catch (NoSuchMethodException ignored) {
            Method buildMethod = customItem.getClass().getMethod("buildItemStack", int.class);
            return buildMethod.invoke(customItem, 1);
        }
    }

    private String readId(String path, String fallback) {
        String raw = plugin.getConfig().getString(path, fallback);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw;
    }

    private String pieceTypePath(PieceType pieceType) {
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
}
