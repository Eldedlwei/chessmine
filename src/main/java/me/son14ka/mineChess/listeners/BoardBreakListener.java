package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.MineChessKeys;
import me.son14ka.mineChess.GameManager;
import me.son14ka.mineChess.RenderViewManager;
import me.son14ka.mineChess.items.ChessBoardItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BoardBreakListener implements Listener {

    private final MineChess plugin;
    private final MineChessKeys keys;
    private final GameManager gameManager;
    private final RenderViewManager renderViewManager;

    public BoardBreakListener(MineChess plugin, GameManager gameManager, RenderViewManager renderViewManager) {
        this.plugin = plugin;
        this.keys = plugin.getKeys();
        this.gameManager = gameManager;
        this.renderViewManager = renderViewManager;
    }

    @EventHandler
    public void onBoardDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction interaction)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        var pdc = interaction.getPersistentDataContainer();
        NamespacedKey gameIdKey = keys.gameId();

        if (pdc.has(gameIdKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            if (player.isSneaking()) {
                if (!player.hasPermission("minechess.break")) {
                    player.sendMessage(plugin.getMessageService().msg(player, "no_permission"));
                    return;
                }
                String gameIdStr = pdc.get(gameIdKey, PersistentDataType.STRING);
                if (gameIdStr != null) {
                    UUID gameId = UUID.fromString(gameIdStr);

                    gameManager.cleanupGame(gameId, interaction.getLocation());
                    renderViewManager.removeGame(gameId);
                    if (plugin.getGameStorage() != null) {
                        plugin.getGameStorage().deleteGame(gameId);
                    }

                    player.getWorld().dropItemNaturally(interaction.getLocation(), ChessBoardItem.createTemplate(keys));
                }
            }
        }
    }

    @EventHandler
    public void onBoardItemPop(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;

        ItemStack containedItem = frame.getItem();
        if (containedItem.getType() == Material.AIR) return;

        if (containedItem.getPersistentDataContainer().has(keys.boardItem(), PersistentDataType.BYTE)) {

            event.setCancelled(true);

            ItemStack drop = ChessBoardItem.createTemplate(keys);

            frame.setItem(null);

            frame.getWorld().dropItemNaturally(frame.getLocation(), drop);

            frame.getWorld().playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
        }
    }
}
