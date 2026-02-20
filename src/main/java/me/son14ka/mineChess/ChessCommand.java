package me.son14ka.mineChess;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChessCommand implements CommandExecutor, TabCompleter {
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
            sendHelp(player);
            return true;
        }

        String sub = normalizeSubcommand(args[0]);
        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

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
            GameManager.broadcastToGame(game, plugin.getMessageService().msg(player, "board_reset"));
            GameEconomy.payoutWinner(plugin, game, winnerSide);
            gameManager.resetGame(game);
            return true;
        }

        if (sub.equals("reload")) {
            if (!player.hasPermission("minechess.reload")) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getMessageService().reload();
            player.sendMessage(plugin.getMessageService().msg(player, "reload_done"));
            return true;
        }

        if (sub.equals("bet")) {
            if (!player.hasPermission("minechess.bet")) {
                player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                return true;
            }
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.getMessageService().msg(player, "bet_requires_vault"));
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
            double minAmount = plugin.getConfig().getDouble("bet.min-amount", 100.0);
            double maxAmount = plugin.getConfig().getDouble("bet.max-amount", 1000.0);
            if (amount < minAmount || amount > maxAmount) {
                player.sendMessage(plugin.getMessageService().msg(
                        player,
                        "bet_range",
                        Placeholder.unparsed("min", String.valueOf(minAmount)),
                        Placeholder.unparsed("max", String.valueOf(maxAmount))
                ));
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
            if (!GameEconomy.placeOrUpdateBet(plugin, game, side, amount)) {
                player.sendMessage(plugin.getMessageService().msg(player, "bet_insufficient"));
                return true;
            }
            storage.saveGame(game);
            player.sendMessage(plugin.getMessageService().msg(player, "bet_set", Placeholder.unparsed("amount", String.valueOf(amount))));
            return true;
        }

        player.sendMessage(plugin.getMessageService().msg(player, "command_unknown"));
        sendHelp(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        boolean zhLocale = player.locale().toLanguageTag().toLowerCase(Locale.ROOT).startsWith("zh");
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            addSubcommandsByPermission(player, zhLocale, subs);
            return filterByPrefix(subs, args[0]);
        }

        String sub = normalizeSubcommand(args[0]);
        if (args.length == 2 && sub.equals("bet") && player.hasPermission("minechess.bet")) {
            String min = String.valueOf(plugin.getConfig().getDouble("bet.min-amount", 100.0));
            String max = String.valueOf(plugin.getConfig().getDouble("bet.max-amount", 1000.0));
            return filterByPrefix(List.of(min, "100", "500", max), args[1]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessageService().msg(player, "help_header"));
        if (player.hasPermission("minechess.bet")) {
            player.sendMessage(plugin.getMessageService().msg(player, "help_bet"));
        }
        if (player.hasPermission("minechess.resign")) {
            player.sendMessage(plugin.getMessageService().msg(player, "help_resign"));
        }
        if (player.hasPermission("minechess.reload")) {
            player.sendMessage(plugin.getMessageService().msg(player, "help_reload"));
        }
        player.sendMessage(plugin.getMessageService().msg(player, "help_help"));
    }

    private void addSubcommandsByPermission(Player player, boolean zhLocale, List<String> subs) {
        if (player.hasPermission("minechess.bet")) {
            subs.add("bet");
            if (zhLocale) subs.add("下注");
        }
        if (player.hasPermission("minechess.resign")) {
            subs.add("resign");
            if (zhLocale) subs.add("认输");
        }
        if (player.hasPermission("minechess.reload")) {
            subs.add("reload");
            if (zhLocale) subs.add("重载");
        }
        subs.add("help");
        subs.add("?");
        if (zhLocale) subs.add("帮助");
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return result;
    }

    private String normalizeSubcommand(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "bet", "下注" -> "bet";
            case "resign", "认输" -> "resign";
            case "reload", "重载" -> "reload";
            case "help", "?", "帮助" -> "help";
            default -> lower;
        };
    }
}
