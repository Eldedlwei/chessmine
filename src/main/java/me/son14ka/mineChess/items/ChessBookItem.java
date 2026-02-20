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

public class ChessBookItem {

    public static ItemStack create(MineChessKeys keys) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            var modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of(2f));
            meta.setCustomModelDataComponent(modelData);

            meta.itemName(Component.translatable("item.minechess.chess_tutorial")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.YELLOW));

            meta.getPersistentDataContainer().set(keys.bookItem(), PersistentDataType.BYTE, (byte) 1);
            book.setItemMeta(meta);
        }
        return book;
    }
}
