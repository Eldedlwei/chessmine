package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.RenderViewManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerViewListener implements Listener {
    private final RenderViewManager renderViewManager;

    public PlayerViewListener(RenderViewManager renderViewManager) {
        this.renderViewManager = renderViewManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        renderViewManager.onPlayerQuit(event.getPlayer());
    }
}
