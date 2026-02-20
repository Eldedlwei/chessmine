package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.ChessGame;
import me.son14ka.mineChess.GameManager;
import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.RenderViewManager;
import me.son14ka.mineChess.BoardSpawner;
import me.son14ka.mineChess.items.ChessBoardItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BoardPlaceListener implements Listener {

    private final MineChess plugin;
    private final GameManager gameManager;
    private final RenderViewManager renderViewManager;

    public BoardPlaceListener(MineChess plugin, GameManager gameManager, RenderViewManager renderViewManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.renderViewManager = renderViewManager;
    }

    @EventHandler
    public void onPlaceFrame(HangingPlaceEvent event) {
        ItemStack item = event.getItemStack();
        if (item == null || item.getType() != Material.ITEM_FRAME) return;
        if (!event.getPlayer().hasPermission("minechess.create")) {
            event.getPlayer().sendMessage(plugin.getMessageService().msg(event.getPlayer(), "no_permission"));
            return;
        }

        if (ChessBoardItem.isBoardItem(plugin, item)) {
            event.getEntity().remove();

            Location startLoc = event.getBlock().getLocation();
            ChessGame game = gameManager.createGame(startLoc);

            buildChessBoard(startLoc, game);
            if (plugin.getGameStorage() != null) {
                plugin.getGameStorage().saveGame(game);
            }
        }
    }

    private void buildChessBoard(Location baseLoc, ChessGame game) {
        BoardSpawner.spawnInteractionCells(plugin, baseLoc, game.getGameId());

        renderViewManager.updateViewsNow(game);
        renderViewManager.refreshGame(game);
    }

}
