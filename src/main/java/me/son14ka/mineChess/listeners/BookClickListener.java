package me.son14ka.mineChess.listeners;

import me.son14ka.mineChess.MineChess;
import me.son14ka.mineChess.items.ChessBookItem;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BookClickListener implements Listener {
    private final MineChess plugin;

    public BookClickListener(MineChess plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick()) return;
        if (e.getItem() == null) return;

        if (!ChessBookItem.isBookItem(plugin, e.getItem()))
            return;

        e.setCancelled(true);
        openTutorialBook(e.getPlayer());
    }

    private void openTutorialBook(@NotNull Player player) {
        Book book = Book.book(
                Component.translatable("item.minechess.chess_tutorial"),
                Component.text("MineChess"),
                List.of(
                        Component.translatable("book.minechess.tutorial.page1"),
                        Component.translatable("book.minechess.tutorial.page2"),
                        Component.translatable("book.minechess.tutorial.page3"),
                        Component.translatable("book.minechess.tutorial.page4"),
                        Component.translatable("book.minechess.tutorial.page5"),
                        Component.translatable("book.minechess.tutorial.page6"),
                        Component.translatable("book.minechess.tutorial.page7"),
                        Component.translatable("book.minechess.tutorial.page8"),
                        Component.translatable("book.minechess.tutorial.page9"),
                        Component.translatable("book.minechess.tutorial.page10"),
                        Component.translatable("book.minechess.tutorial.page11"),
                        Component.translatable("book.minechess.tutorial.page12"),
                        Component.translatable("book.minechess.tutorial.page13"),
                        Component.translatable("book.minechess.tutorial.page14"),
                        Component.translatable("book.minechess.tutorial.page15"),
                        Component.translatable("book.minechess.tutorial.page16"),
                        Component.translatable("book.minechess.tutorial.page17"),
                        Component.translatable("book.minechess.tutorial.page18")
                )
        );

        player.openBook(book);
    }
}
