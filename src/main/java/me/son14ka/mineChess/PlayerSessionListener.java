package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSessionListener implements Listener {
    private final MineChess plugin;
    private final GameManager gameManager;
    private final Map<UUID, BukkitTask> quitTasks = new HashMap<>();

    public PlayerSessionListener(MineChess plugin, GameManager gameManager, GameStorage storage) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ChessGame game = gameManager.getGameByPlayer(player.getUniqueId());
        if (game == null || game.isGameOver()) return;

        long delaySeconds = plugin.getConfig().getLong("disconnect.loss-seconds", 30L);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ChessGame stillGame = gameManager.getGameByPlayer(player.getUniqueId());
            if (stillGame == null || stillGame.isGameOver()) return;

            stillGame.setGameOver(true);
            Side loserSide = stillGame.getPlayerSide(player.getUniqueId());
            Side winnerSide = loserSide == Side.WHITE ? Side.BLACK : Side.WHITE;

            Component msg = plugin.getMessageService().msg(player, "disconnect_loss", Placeholder.unparsed("player", player.getName()));
            GameManager.broadcastToGame(stillGame, msg);
            GameManager.broadcastToGame(stillGame, plugin.getMessageService().msg(player, "board_reset"));
            GameEconomy.payoutWinner(plugin, stillGame, winnerSide);
            gameManager.resetGame(stillGame);
        }, delaySeconds * 20L);

        quitTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitTask task = quitTasks.remove(playerId);
        if (task != null) task.cancel();
    }
}
