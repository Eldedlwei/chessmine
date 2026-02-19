package me.son14ka.mineChess;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ViewSpace {
    private final JavaPlugin plugin;
    private final Player player;
    private final List<Entity> entities = new ArrayList<>();

    public ViewSpace(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public <T extends Entity> T spawn(Location location, Class<T> entityClass) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Cannot spawn client entity without world");
        }

        T entity = world.spawn(location, entityClass, spawned -> {
            spawned.setPersistent(false);
            spawned.setVisibleByDefault(false);
            spawned.setSilent(true);
        });
        player.showEntity(plugin, entity);
        entities.add(entity);
        return entity;
    }

    public void announce() {
        if (!player.isOnline()) {
            return;
        }
        for (Entity entity : entities) {
            if (entity != null && entity.isValid()) {
                player.showEntity(plugin, entity);
            }
        }
    }

    public void close() {
        for (Entity entity : entities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        entities.clear();
    }
}
