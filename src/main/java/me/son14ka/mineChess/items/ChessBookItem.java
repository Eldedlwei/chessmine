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

public class ChessBookItem {

    public static ItemStack create(MineChess plugin) {
        MineChessKeys keys = plugin.getKeys();
        ItemStack ceItem = plugin.getCraftEngineItems().createTutorialBookItem();
        if (ceItem != null) {
            markBookItem(ceItem, keys);
            return ceItem;
        }

        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            var modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of(2f));
            meta.setCustomModelDataComponent(modelData);

            meta.itemName(Component.translatable("item.minechess.chess_tutorial")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.YELLOW));

            markBookItemMeta(meta, keys);
            book.setItemMeta(meta);
        }
        return book;
    }

    public static boolean isBookItem(MineChess plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (plugin.getCraftEngineItems().isTutorialBookItem(item)) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(plugin.getKeys().bookItem(), PersistentDataType.BYTE);
    }

    private static void markBookItem(ItemStack item, MineChessKeys keys) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        markBookItemMeta(meta, keys);
        item.setItemMeta(meta);
    }

    private static void markBookItemMeta(ItemMeta meta, MineChessKeys keys) {
        meta.getPersistentDataContainer().set(keys.bookItem(), PersistentDataType.BYTE, (byte) 1);
    }
}
