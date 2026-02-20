package me.son14ka.mineChess;

import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class CraftEngineReloadListener implements Listener {
    private final MineChess plugin;

    public CraftEngineReloadListener(MineChess plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        plugin.getCraftEngineItems().reload();
        for (ChessGame game : plugin.getGameManager().getActiveGames()) {
            plugin.getRenderViewManager().refreshGame(game);
        }
    }
}
