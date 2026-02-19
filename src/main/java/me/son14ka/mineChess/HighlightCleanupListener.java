package me.son14ka.mineChess;

import me.son14ka.mineChess.listeners.BoardClickListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class HighlightCleanupListener implements Listener {
    private final BoardClickListener boardClickListener;

    public HighlightCleanupListener(BoardClickListener boardClickListener) {
        this.boardClickListener = boardClickListener;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boardClickListener.closeHighlights(event.getPlayer().getUniqueId());
    }
}
