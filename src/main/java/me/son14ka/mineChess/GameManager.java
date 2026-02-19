package me.son14ka.mineChess;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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

        loc.getWorld().getNearbyEntities(loc, 3, 2, 3).forEach(entity -> {
            var pdc = entity.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "game_id");

            if (pdc.has(key, PersistentDataType.STRING)) {
                String idStr = pdc.get(key, PersistentDataType.STRING);
                if (idStr != null && idStr.equals(gameId.toString())) {
                    entity.remove();
                }
            }
        });
    }

    public static void broadcastToGame(ChessGame game, Component message) {
        double radius = 3.0;
        Location center = game.getOrigin();

        center.getWorld().getNearbyPlayers(center, radius).forEach(player -> {
            player.sendMessage(message);
        });
    }
}
