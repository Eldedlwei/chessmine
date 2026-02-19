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
        for (ChessGame game : gameManager.getActiveGames()) {
            if (!game.isStarted() || game.isGameOver()) continue;
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
        }
        GameEconomy.payoutWinner(plugin, game, winnerSide);
    }
}
