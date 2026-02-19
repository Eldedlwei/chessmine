package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public final class GameEconomy {
    private GameEconomy() {
    }

    public static boolean lockBets(MineChess plugin, ChessGame game) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return true;

        Player white = plugin.getServer().getPlayer(game.getWhitePlayer());
        Player black = plugin.getServer().getPlayer(game.getBlackPlayer());
        if (white == null || black == null) return false;

        double wBet = game.getWhiteBet();
        double bBet = game.getBlackBet();
        if (wBet < 0 || bBet < 0) return false;

        if (!economy.has(white, wBet) || !economy.has(black, bBet)) {
            return false;
        }

        if (wBet > 0 && !economy.withdrawPlayer(white, wBet).transactionSuccess()) {
            return false;
        }
        if (bBet > 0 && !economy.withdrawPlayer(black, bBet).transactionSuccess()) {
            if (wBet > 0) economy.depositPlayer(white, wBet);
            return false;
        }
        game.setBetLocked(true);
        return true;
    }

    public static void payoutWinner(MineChess plugin, ChessGame game, Side winnerSide) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return;

        Player winner = plugin.getServer().getPlayer(game.getPlayer(winnerSide));
        if (winner == null) return;

        double pot = game.getWhiteBet() + game.getBlackBet();
        if (pot > 0) {
            economy.depositPlayer(winner, pot);
        }
    }

    public static void refundBets(MineChess plugin, ChessGame game) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return;

        Player white = plugin.getServer().getPlayer(game.getWhitePlayer());
        Player black = plugin.getServer().getPlayer(game.getBlackPlayer());
        if (white != null && game.getWhiteBet() > 0) {
            economy.depositPlayer(white, game.getWhiteBet());
        }
        if (black != null && game.getBlackBet() > 0) {
            economy.depositPlayer(black, game.getBlackBet());
        }
    }
}
