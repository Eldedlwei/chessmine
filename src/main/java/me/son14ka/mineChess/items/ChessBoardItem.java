package me.son14ka.mineChess.items;

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

    public static ItemStack createTemplate(MineChessKeys keys) {
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of(1f));
            meta.setCustomModelDataComponent(modelData);

            meta.itemName(Component.translatable("item.minechess.board")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.YELLOW));

            meta.getPersistentDataContainer().set(keys.boardItem(), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
