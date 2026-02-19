package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public class GameClockManager {
    private final MineChess plugin;
    private final GameManager gameManager;
    private final GameStorage storage;

    public GameClockManager(MineChess plugin, GameManager gameManager, GameStorage storage) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.storage = storage;
    }

    public void tick() {
        long startWaitMs = plugin.getConfig().getLong("start.wait-seconds", 60L) * 1000L;
        long now = System.currentTimeMillis();
        for (ChessGame game : gameManager.getActiveGames()) {
            if (game.isGameOver()) continue;

            if (!game.isStarted()) {
                long waitingSince = game.getWaitingStartSinceMs();
                if (waitingSince > 0L && now - waitingSince >= startWaitMs) {
                    game.setGameOver(true);
                    var white = plugin.getServer().getPlayer(game.getWhitePlayer());
                    if (white != null) {
                        white.sendMessage(plugin.getMessageService().msg(white, "start_timeout"));
                        white.sendMessage(plugin.getMessageService().msg(white, "board_reset"));
                    }
                    var black = plugin.getServer().getPlayer(game.getBlackPlayer());
                    if (black != null) {
                        black.sendMessage(plugin.getMessageService().msg(black, "start_timeout"));
                        black.sendMessage(plugin.getMessageService().msg(black, "board_reset"));
                    }
                    if (game.isBetLocked()) {
                        GameEconomy.refundBets(plugin, game);
                    }
                    gameManager.resetGame(game);
                }
                continue;
            }

            Side side = game.getCurrentTurnSide();
            game.decrementTime(side, 1000L);

            long remaining = side == Side.WHITE ? game.getWhiteTimeMs() : game.getBlackTimeMs();
            if (remaining <= 0) {
                handleTimeout(game, side);
            }
            if (storage != null) storage.saveGame(game);
        }
    }

    private void handleTimeout(ChessGame game, Side sideOut) {
        game.setGameOver(true);

        Side winnerSide = sideOut == Side.WHITE ? Side.BLACK : Side.WHITE;
        Player winner = plugin.getServer().getPlayer(game.getPlayer(winnerSide));
        if (winner != null) {
            String winnerName = winner.getName();
            Component msg = plugin.getMessageService().msg(winner, "timeout_broadcast", Placeholder.unparsed("winner", winnerName));
            GameManager.broadcastToGame(game, msg);
            GameManager.broadcastToGame(game, plugin.getMessageService().msg(winner, "board_reset"));
        }
        GameEconomy.payoutWinner(plugin, game, winnerSide);
        gameManager.resetGame(game);
    }
}
