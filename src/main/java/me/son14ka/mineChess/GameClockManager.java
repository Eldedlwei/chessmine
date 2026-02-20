package me.son14ka.mineChess;

import com.github.bhlangonijr.chesslib.Side;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameClockManager {
    private final MineChess plugin;
    private final GameManager gameManager;
    private final GameStorage storage;
    private final Map<UUID, BossBar> clockBarsByPlayer = new HashMap<>();

    public GameClockManager(MineChess plugin, GameManager gameManager, GameStorage storage) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.storage = storage;
    }

    public void tick() {
        long startWaitMs = plugin.getConfig().getLong("start.wait-seconds", 60L) * 1000L;
        long now = System.currentTimeMillis();
        boolean bossBarEnabled = plugin.getConfig().getBoolean("clock.bossbar-enabled", true);
        Set<UUID> activeClockBarPlayers = bossBarEnabled ? new HashSet<>() : null;

        for (ChessGame game : gameManager.getActiveGames()) {
            if (game.isGameOver()) continue;

            if (!game.isStarted()) {
                long waitingSince = game.getWaitingStartSinceMs();
                if (waitingSince > 0L && now - waitingSince >= startWaitMs) {
                    game.setGameOver(true);
                    var white = getPlayer(game.getWhitePlayer());
                    if (white != null) {
                        white.sendMessage(plugin.getMessageService().msg(white, "start_timeout"));
                        white.sendMessage(plugin.getMessageService().msg(white, "board_reset"));
                    }
                    var black = getPlayer(game.getBlackPlayer());
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
            } else if (bossBarEnabled) {
                updateBarsForGame(game, activeClockBarPlayers);
            }
            if (storage != null) storage.saveGame(game);
        }

        if (bossBarEnabled) {
            cleanupInactiveBars(activeClockBarPlayers);
        } else {
            clearAllBars();
        }
    }

    private void handleTimeout(ChessGame game, Side sideOut) {
        game.setGameOver(true);

        Side winnerSide = sideOut == Side.WHITE ? Side.BLACK : Side.WHITE;
        Player winner = getPlayer(game.getPlayer(winnerSide));
        if (winner != null) {
            String winnerName = winner.getName();
            Component msg = plugin.getMessageService().msg(winner, "timeout_broadcast", Placeholder.unparsed("winner", winnerName));
            GameManager.broadcastToGame(game, msg);
            GameManager.broadcastToGame(game, plugin.getMessageService().msg(winner, "board_reset"));
        }
        GameEconomy.payoutWinner(plugin, game, winnerSide);
        gameManager.resetGame(game);
    }

    public void stop() {
        clearAllBars();
    }

    private void updateBarsForGame(ChessGame game, Set<UUID> activeClockBarPlayers) {
        UUID whiteId = game.getWhitePlayer();
        if (whiteId != null && updateBarForPlayer(game, Side.WHITE, whiteId)) {
            activeClockBarPlayers.add(whiteId);
        }

        UUID blackId = game.getBlackPlayer();
        if (blackId != null && updateBarForPlayer(game, Side.BLACK, blackId)) {
            activeClockBarPlayers.add(blackId);
        }
    }

    private boolean updateBarForPlayer(ChessGame game, Side perspective, UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            removeBar(playerId);
            return false;
        }

        BossBar bar = clockBarsByPlayer.computeIfAbsent(playerId, id ->
                BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS));

        float progress = computeProgress(game, perspective);
        bar.progress(progress);
        bar.color(resolveBarColor(progress));
        bar.name(buildBarTitle(player, game));
        player.showBossBar(bar);
        return true;
    }

    private Component buildBarTitle(Player viewer, ChessGame game) {
        Component whiteLabel = plugin.getMessageService().msg(viewer, "white");
        Component blackLabel = plugin.getMessageService().msg(viewer, "black");
        String whiteTime = formatTime(game.getWhiteTimeMs());
        String blackTime = formatTime(game.getBlackTimeMs());
        boolean whiteTurn = game.getCurrentTurnSide() == Side.WHITE;
        boolean blackTurn = !whiteTurn;

        return Component.empty()
                .append(whiteTurn ? Component.text(">> ", NamedTextColor.GOLD) : Component.text("   "))
                .append(whiteLabel)
                .append(Component.text(" " + whiteTime + " ", NamedTextColor.WHITE))
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(blackTurn ? Component.text(">> ", NamedTextColor.GOLD) : Component.text("   "))
                .append(blackLabel)
                .append(Component.text(" " + blackTime, NamedTextColor.WHITE));
    }

    private float computeProgress(ChessGame game, Side perspective) {
        long baseTimeMs = Math.max(1000L, plugin.getConfig().getLong("clock.initial-seconds", 300L) * 1000L);
        long remaining = perspective == Side.WHITE ? game.getWhiteTimeMs() : game.getBlackTimeMs();
        float ratio = (float) remaining / (float) baseTimeMs;
        if (ratio < 0f) return 0f;
        if (ratio > 1f) return 1f;
        return ratio;
    }

    private BossBar.Color resolveBarColor(float progress) {
        if (progress <= 0.2f) {
            return BossBar.Color.RED;
        }
        if (progress <= 0.5f) {
            return BossBar.Color.YELLOW;
        }
        return BossBar.Color.GREEN;
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void cleanupInactiveBars(Set<UUID> activeClockBarPlayers) {
        Iterator<Map.Entry<UUID, BossBar>> it = clockBarsByPlayer.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BossBar> entry = it.next();
            if (!activeClockBarPlayers.contains(entry.getKey())) {
                Player player = getPlayer(entry.getKey());
                if (player != null) {
                    player.hideBossBar(entry.getValue());
                }
                it.remove();
            }
        }
    }

    private void removeBar(UUID playerId) {
        BossBar bar = clockBarsByPlayer.remove(playerId);
        if (bar == null) {
            return;
        }
        Player player = getPlayer(playerId);
        if (player != null) {
            player.hideBossBar(bar);
        }
    }

    private void clearAllBars() {
        for (Map.Entry<UUID, BossBar> entry : clockBarsByPlayer.entrySet()) {
            Player player = getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        clockBarsByPlayer.clear();
    }

    private Player getPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return plugin.getServer().getPlayer(playerId);
    }
}
