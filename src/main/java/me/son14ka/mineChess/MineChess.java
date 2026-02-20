package me.son14ka.mineChess;

import me.son14ka.mineChess.items.ChessBoardItem;
import me.son14ka.mineChess.items.ChessBookItem;
import me.son14ka.mineChess.listeners.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.util.UUID;

public final class MineChess extends JavaPlugin {


    private GameManager gameManager;
    private RenderViewManager renderViewManager;
    private GameClockManager gameClockManager;
    private GameStorage gameStorage;
    private MessageService messageService;
    private CraftEngineItemService craftEngineItemService;
    private Economy economy;
    private BukkitTask viewTask;
    private BoardClickListener boardClickListener;
    private MineChessKeys keys;
    private boolean craftEngineReloadListenerRegistered;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfAbsent("messages_uk.yml");
        saveResourceIfAbsent("messages_en.yml");
        saveResourceIfAbsent("messages_zh.yml");
        keys = new MineChessKeys(this);

        gameManager = new GameManager(this);
        renderViewManager = new RenderViewManager(this, gameManager);
        messageService = new MessageService(this);
        craftEngineItemService = new CraftEngineItemService(this);
        craftEngineReloadListenerRegistered = false;
        setupEconomy();
        gameStorage = new GameStorage(getDataFolder());
        try {
            gameStorage.init();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize H2 storage: " + e.getMessage());
        }
        gameClockManager = new GameClockManager(this, gameManager, gameStorage);

        registerListeners();
        registerRecipes();
        viewTask = getServer().getScheduler().runTaskTimer(this, () -> {
            renderViewManager.tick();
            gameClockManager.tick();
        }, 0L, 20L);
        loadGames();
        getLogger().info("MineChess has loaded.");
    }

    void registerListeners(){
        getServer().getPluginManager().registerEvents(new BoardInCraftListener(this), this);
        getServer().getPluginManager().registerEvents(new BookClickListener(this), this);
        getServer().getPluginManager().registerEvents(new AvoidInVanillaCraftsListener(this), this);
        boardClickListener = new BoardClickListener(this, gameManager, renderViewManager);
        getServer().getPluginManager().registerEvents(boardClickListener, this);
        getServer().getPluginManager().registerEvents(new BoardBreakListener(this, gameManager, renderViewManager),this);
        getServer().getPluginManager().registerEvents(new BoardPlaceListener(this, gameManager, renderViewManager), this);
        getServer().getPluginManager().registerEvents(new PlayerViewListener(renderViewManager), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this, gameManager, gameStorage), this);
        getServer().getPluginManager().registerEvents(new HighlightCleanupListener(boardClickListener), this);
        registerCraftEngineReloadListenerIfNeeded();
        var cmd = getCommand("chess");
        if (cmd != null) {
            ChessCommand chessCommand = new ChessCommand(this, gameManager, gameStorage);
            cmd.setExecutor(chessCommand);
            cmd.setTabCompleter(chessCommand);
        }
    }

    void registerRecipes(){
        ItemStack boardIcon = ChessBoardItem.createTemplate(this);

        ShapedRecipe recipe = new ShapedRecipe(keys.chessBoardRecipe(), boardIcon);
        RecipeChoice choices = new RecipeChoice.MaterialChoice(
                Material.ACACIA_PLANKS, Material.OAK_PLANKS,
                Material.BAMBOO_PLANKS, Material.CHERRY_PLANKS,
                Material.BIRCH_PLANKS, Material.CRIMSON_PLANKS,
                Material.JUNGLE_PLANKS, Material.SPRUCE_PLANKS,
                Material.MANGROVE_PLANKS, Material.WARPED_PLANKS,
                Material.PALE_OAK_PLANKS, Material.DARK_OAK_PLANKS);

        recipe.shape(
                "SSS",
                "WWW",
                "SSS"
        );

        recipe.setIngredient('W', choices);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);

        ItemStack tutorialItem = ChessBookItem.create(this);

        ShapelessRecipe tutorialRecipe = new ShapelessRecipe(
                keys.chessTutorialRecipe(),
                tutorialItem
        );

        tutorialRecipe.addIngredient(
                Material.ITEM_FRAME);
        tutorialRecipe.addIngredient(
                Material.BOOK);

        Bukkit.addRecipe(tutorialRecipe);
    }

    @Override
    public void onDisable() {
        if (viewTask != null) {
            viewTask.cancel();
            viewTask = null;
        }
        if (gameClockManager != null) {
            gameClockManager.stop();
        }
        if (gameStorage != null) {
            for (ChessGame game : gameManager.getActiveGames()) {
                gameStorage.saveGame(game);
            }
        }
        if (renderViewManager != null) {
            renderViewManager.stop();
        }
        getLogger().info("MineChess has unloaded.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public GameStorage getGameStorage() {
        return gameStorage;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public CraftEngineItemService getCraftEngineItems() {
        return craftEngineItemService;
    }

    public RenderViewManager getRenderViewManager() {
        return renderViewManager;
    }

    public MineChessKeys getKeys() {
        return keys;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public void refreshCraftEngineIntegration() {
        if (craftEngineItemService == null) {
            return;
        }
        craftEngineItemService.reload();
        registerCraftEngineReloadListenerIfNeeded();
    }

    private void registerCraftEngineReloadListenerIfNeeded() {
        if (craftEngineReloadListenerRegistered || craftEngineItemService == null || !craftEngineItemService.isUsingCraftEngine()) {
            return;
        }
        getServer().getPluginManager().registerEvents(new CraftEngineReloadListener(this), this);
        craftEngineReloadListenerRegistered = true;
    }

    private void saveResourceIfAbsent(String resourcePath) {
        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private void loadGames() {
        if (gameStorage == null) return;
        for (GameStorage.StoredGame stored : gameStorage.loadGames()) {
            var world = getServer().getWorld(stored.world());
            if (world == null) continue;
            ChessGame game = new ChessGame(stored.gameId(), new org.bukkit.Location(world, stored.x(), stored.y(), stored.z()));
            game.getBoard().loadFromFen(stored.fen());
            game.setWaitingForPromotion(stored.waitingPromotion());
            game.setGameOver(stored.gameOver());
            if (stored.whiteUuid() != null) game.setWhitePlayer(UUID.fromString(stored.whiteUuid()));
            if (stored.blackUuid() != null) game.setBlackPlayer(UUID.fromString(stored.blackUuid()));
            game.setWhiteTimeMs(stored.whiteTime());
            game.setBlackTimeMs(stored.blackTime());
            game.setIncrementMs(stored.increment());
            game.setWhiteBet(stored.whiteBet());
            game.setBlackBet(stored.blackBet());
            game.setWhiteBetConfirmed(stored.whiteBetConfirmed());
            game.setBlackBetConfirmed(stored.blackBetConfirmed());
            game.setBetLocked(stored.betLocked());
            game.setStarted(stored.started());
            if (stored.pendingFrom() != null && stored.pendingTo() != null && stored.getPendingSide() != null) {
                game.setPendingPromotion(new ChessGame.PendingPromotion(
                        com.github.bhlangonijr.chesslib.Square.valueOf(stored.pendingFrom()),
                        com.github.bhlangonijr.chesslib.Square.valueOf(stored.pendingTo()),
                        stored.getPendingSide()
                ));
            }
            gameManager.addGame(game);
            BoardSpawner.spawnInteractionCells(this, game.getOrigin(), game.getGameId());
            renderViewManager.updateViewsNow(game);
            renderViewManager.refreshGame(game);
            if (game.isWaitingForPromotion() && game.getPendingPromotion() != null) {
                int[] coords = ChessMapping.toCoords(game.getPendingPromotion().to());
                PromotionSpawner.spawnPromotionChoices(this, game, coords[0], coords[1], game.getPendingPromotion().side() == com.github.bhlangonijr.chesslib.Side.WHITE);
            }
        }
    }
}
