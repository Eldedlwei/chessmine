package me.son14ka.mineChess;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private final MineChess plugin;
    private final Map<UUID, ChessGame> activeGames = new HashMap<>();

    public GameManager(MineChess plugin) {
        this.plugin = plugin;
    }

    public ChessGame createGame(Location origin) {
        UUID gameId = UUID.randomUUID();
        ChessGame game = new ChessGame(gameId, origin);
        activeGames.put(gameId, game);
        return game;
    }

    public ChessGame getGame(UUID gameId) {
        return activeGames.get(gameId);
    }

    public Collection<ChessGame> getActiveGames() {
        return activeGames.values();
    }

    public void addGame(ChessGame game) {
        activeGames.put(game.getGameId(), game);
    }

    public ChessGame getNearestGame(Location location, double radius) {
        ChessGame best = null;
        double bestDist = radius * radius;
        for (ChessGame game : activeGames.values()) {
            if (!game.getOrigin().getWorld().equals(location.getWorld())) continue;
            double dist = game.getOrigin().distanceSquared(location);
            if (dist <= bestDist) {
                bestDist = dist;
                best = game;
            }
        }
        return best;
    }

    public ChessGame getGameByPlayer(UUID playerId) {
        for (ChessGame game : activeGames.values()) {
            if (game.isPlayer(playerId)) {
                return game;
            }
        }
        return null;
    }

    public void cleanupGame(UUID gameId, Location loc) {
        activeGames.remove(gameId);
        String targetId = gameId.toString();
        NamespacedKey key = plugin.getKeys().gameId();
        for (Entity entity : loc.getWorld().getEntities()) {
            var pdc = entity.getPersistentDataContainer();
            String idStr = pdc.get(key, PersistentDataType.STRING);
            if (targetId.equals(idStr)) {
                entity.remove();
            }
        }
    }

    public void resetGame(ChessGame game) {
        PromotionSpawner.cleanupPromotionEntities(plugin, game.getOrigin().getWorld(), game.getGameId());
        game.resetForNextMatch();

        if (plugin.getRenderViewManager() != null) {
            plugin.getRenderViewManager().refreshGame(game);
        }
        if (plugin.getGameStorage() != null) {
            plugin.getGameStorage().saveGame(game);
        }
    }

    public static void broadcastToGame(ChessGame game, Component message) {
        double radius = 50.0;
        Location center = game.getOrigin();

        center.getWorld().getNearbyPlayers(center, radius).forEach(player -> {
            player.sendMessage(message);
        });
    }
}
