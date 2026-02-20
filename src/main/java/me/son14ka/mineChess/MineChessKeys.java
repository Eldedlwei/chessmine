package me.son14ka.mineChess;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class MineChessKeys {
    private final NamespacedKey boardItem;
    private final NamespacedKey bookItem;
    private final NamespacedKey gameId;
    private final NamespacedKey chessRow;
    private final NamespacedKey chessCol;
    private final NamespacedKey promotionCmd;
    private final NamespacedKey promotionRow;
    private final NamespacedKey promotionCol;
    private final NamespacedKey promotionItem;
    private final NamespacedKey chessBoardRecipe;
    private final NamespacedKey chessTutorialRecipe;

    public MineChessKeys(Plugin plugin) {
        this.boardItem = new NamespacedKey(plugin, "chess_board_item");
        this.bookItem = new NamespacedKey(plugin, "chess_book_item");
        this.gameId = new NamespacedKey(plugin, "game_id");
        this.chessRow = new NamespacedKey(plugin, "chess_row");
        this.chessCol = new NamespacedKey(plugin, "chess_col");
        this.promotionCmd = new NamespacedKey(plugin, "promotion_cmd");
        this.promotionRow = new NamespacedKey(plugin, "promotion_row");
        this.promotionCol = new NamespacedKey(plugin, "promotion_col");
        this.promotionItem = new NamespacedKey(plugin, "is_promotion_item");
        this.chessBoardRecipe = new NamespacedKey(plugin, "chess_board");
        this.chessTutorialRecipe = new NamespacedKey(plugin, "chess_tutorial_recipe");
    }

    public NamespacedKey boardItem() {
        return boardItem;
    }

    public NamespacedKey bookItem() {
        return bookItem;
    }

    public NamespacedKey gameId() {
        return gameId;
    }

    public NamespacedKey chessRow() {
        return chessRow;
    }

    public NamespacedKey chessCol() {
        return chessCol;
    }

    public NamespacedKey promotionCmd() {
        return promotionCmd;
    }

    public NamespacedKey promotionRow() {
        return promotionRow;
    }

    public NamespacedKey promotionCol() {
        return promotionCol;
    }

    public NamespacedKey promotionItem() {
        return promotionItem;
    }

    public NamespacedKey chessBoardRecipe() {
        return chessBoardRecipe;
    }

    public NamespacedKey chessTutorialRecipe() {
        return chessTutorialRecipe;
    }
}
