package me.son14ka.mineChess.items;

import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.MineChessKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ChessBoardItem {

    public static ItemStack createTemplate(MineChess plugin) {
        MineChessKeys keys = plugin.getKeys();
        ItemStack ceItem = plugin.getCraftEngineItems().createBoardItem();
        if (ceItem != null) {
            markBoardItem(ceItem, keys);
            return ceItem;
        }

        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of(1f));
            meta.setCustomModelDataComponent(modelData);

            meta.itemName(Component.translatable("item.minechess.board")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.YELLOW));

            markBoardItemMeta(meta, keys);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isBoardItem(MineChess plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (plugin.getCraftEngineItems().isBoardItem(item)) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(plugin.getKeys().boardItem(), PersistentDataType.BYTE);
    }

    private static void markBoardItem(ItemStack item, MineChessKeys keys) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        markBoardItemMeta(meta, keys);
        item.setItemMeta(meta);
    }

    private static void markBoardItemMeta(ItemMeta meta, MineChessKeys keys) {
        meta.getPersistentDataContainer().set(keys.boardItem(), PersistentDataType.BYTE, (byte) 1);
    }
}
