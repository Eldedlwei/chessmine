package me.son14ka.mineChess;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChessCommand implements CommandExecutor {
    private final MineChess plugin;
    private final GameManager gameManager;
    private final GameStorage storage;

    public ChessCommand(MineChess plugin, GameManager gameManager, GameStorage storage) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageService().msg(player, "command_usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("resign")) {
            if (!player.hasPermission("minechess.resign")) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                return true;
            }
            ChessGame game = gameManager.getGameByPlayer(player.getUniqueId());
            if (game == null) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_game"));
                return true;
            }
            game.setGameOver(true);
            var side = game.getPlayerSide(player.getUniqueId());
            var winnerSide = side == com.github.bhlangonijr.chesslib.Side.WHITE ? com.github.bhlangonijr.chesslib.Side.BLACK : com.github.bhlangonijr.chesslib.Side.WHITE;
            Component msg = plugin.getMessageService().msg(player, "resign_broadcast", Placeholder.unparsed("player", player.getName()));
            GameManager.broadcastToGame(game, msg);
            GameEconomy.payoutWinner(plugin, game, winnerSide);
            storage.saveGame(game);
            return true;
        }

        if (sub.equals("reload")) {
            if (!player.hasPermission("minechess.reload")) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                return true;
            }
            plugin.getMessageService().reload();
            player.sendMessage(plugin.getMessageService().msg(player, "reload_done"));
            return true;
        }

        if (sub.equals("bet")) {
            if (!player.hasPermission("minechess.bet")) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageService().msg(player, "command_usage"));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageService().msg(player, "bet_invalid"));
                return true;
            }
            if (amount < plugin.getConfig().getDouble("bet.min-amount", 0.0)) {
                player.sendMessage(plugin.getMessageService().msg(player, "bet_invalid"));
                return true;
            }
            ChessGame game = gameManager.getNearestGame(player.getLocation(), 4.0);
            if (game == null) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_game"));
                return true;
            }
            var side = game.getPlayerSide(player.getUniqueId());
            if (side == null) {
                player.sendMessage(plugin.getMessageService().msg(player, "not_player"));
                return true;
            }
            if (game.isStarted()) {
                player.sendMessage(plugin.getMessageService().msg(player, "bet_locked"));
                return true;
            }
            if (side == com.github.bhlangonijr.chesslib.Side.WHITE) {
                game.setWhiteBet(amount);
                game.setWhiteBetConfirmed(true);
            } else {
                game.setBlackBet(amount);
                game.setBlackBetConfirmed(true);
            }
            storage.saveGame(game);
            player.sendMessage(plugin.getMessageService().msg(player, "bet_set", Placeholder.unparsed("amount", String.valueOf(amount))));
            return true;
        }

        player.sendMessage(plugin.getMessageService().msg(player, "command_usage"));
        return true;
    }
}
