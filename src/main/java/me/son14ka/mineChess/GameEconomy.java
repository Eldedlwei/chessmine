package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class GameEconomy {
    private GameEconomy() {
    }

    public static boolean placeOrUpdateBet(MineChess plugin, ChessGame game, Side side, double newAmount) {
        Economy economy = plugin.getEconomy();
        if (economy == null || newAmount < 0.0) return false;

        UUID playerId = game.getPlayer(side);
        OfflinePlayer player = getOfflinePlayer(playerId);
        if (player == null) return false;

        double previousBet = side == Side.WHITE ? game.getWhiteBet() : game.getBlackBet();
        boolean previousConfirmed = side == Side.WHITE ? game.isWhiteBetConfirmed() : game.isBlackBetConfirmed();
        boolean wasLocked = game.isBetLocked();
        boolean otherConfirmed = side == Side.WHITE ? game.isBlackBetConfirmed() : game.isWhiteBetConfirmed();

        if (wasLocked) {
            double previousEscrow = getEscrowedAmount(game, side);
            double delta = newAmount - previousEscrow;
            if (delta > 0.0 && !withdraw(economy, player, delta)) {
                return false;
            }
            if (delta < 0.0 && !deposit(economy, player, -delta)) {
                return false;
            }
        } else if (!otherConfirmed && newAmount > 0.0 && !withdraw(economy, player, newAmount)) {
            return false;
        }

        if (side == Side.WHITE) {
            game.setWhiteBet(newAmount);
            game.setWhiteBetConfirmed(true);
        } else {
            game.setBlackBet(newAmount);
            game.setBlackBetConfirmed(true);
        }

        if (!wasLocked && game.areBetsConfirmed()) {
            game.setBetLocked(false);
            if (!lockBets(plugin, game)) {
                if (side == Side.WHITE) {
                    game.setWhiteBet(previousBet);
                    game.setWhiteBetConfirmed(previousConfirmed);
                } else {
                    game.setBlackBet(previousBet);
                    game.setBlackBetConfirmed(previousConfirmed);
                }
                return false;
            }
            return true;
        }

        game.setBetLocked(true);
        return true;
    }

    public static boolean lockBets(MineChess plugin, ChessGame game) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return false;
        if (game.isBetLocked()) return true;

        double wBet = game.isWhiteBetConfirmed() ? Math.max(0.0, game.getWhiteBet()) : 0.0;
        double bBet = game.isBlackBetConfirmed() ? Math.max(0.0, game.getBlackBet()) : 0.0;
        if (wBet < 0 || bBet < 0) return false;

        OfflinePlayer white = getOfflinePlayer(game.getWhitePlayer());
        OfflinePlayer black = getOfflinePlayer(game.getBlackPlayer());
        if (wBet > 0 && white == null) return false;
        if (bBet > 0 && black == null) return false;

        if (wBet > 0 && !withdraw(economy, white, wBet)) {
            return false;
        }
        if (bBet > 0 && !withdraw(economy, black, bBet)) {
            if (wBet > 0) {
                deposit(economy, white, wBet);
            }
            return false;
        }

        game.setBetLocked(true);
        return true;
    }

    public static void payoutWinner(MineChess plugin, ChessGame game, Side winnerSide) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return;

        OfflinePlayer winner = getOfflinePlayer(game.getPlayer(winnerSide));
        if (winner == null) return;

        double pot = getEscrowedAmount(game, Side.WHITE) + getEscrowedAmount(game, Side.BLACK);
        if (pot > 0) {
            deposit(economy, winner, pot);
        }
    }

    public static void refundBets(MineChess plugin, ChessGame game) {
        Economy economy = plugin.getEconomy();
        if (economy == null) return;

        double whiteStake = getEscrowedAmount(game, Side.WHITE);
        double blackStake = getEscrowedAmount(game, Side.BLACK);

        OfflinePlayer white = getOfflinePlayer(game.getWhitePlayer());
        OfflinePlayer black = getOfflinePlayer(game.getBlackPlayer());
        if (white != null && whiteStake > 0) {
            deposit(economy, white, whiteStake);
        }
        if (black != null && blackStake > 0) {
            deposit(economy, black, blackStake);
        }
    }

    private static double getEscrowedAmount(ChessGame game, Side side) {
        if (!game.isBetLocked()) {
            return 0.0;
        }
        if (side == Side.WHITE) {
            return game.isWhiteBetConfirmed() ? Math.max(0.0, game.getWhiteBet()) : 0.0;
        }
        return game.isBlackBetConfirmed() ? Math.max(0.0, game.getBlackBet()) : 0.0;
    }

    private static boolean withdraw(Economy economy, OfflinePlayer player, double amount) {
        if (player == null || amount <= 0.0) {
            return true;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    private static boolean deposit(Economy economy, OfflinePlayer player, double amount) {
        if (player == null || amount <= 0.0) {
            return true;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    private static OfflinePlayer getOfflinePlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(playerId);
    }
}
