package fr.lye.biomentry;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Biomentry extends JavaPlugin {

    private static final String VERSION = "1.2";
    public static boolean DEBUG_MODE = false; // Désactivé par défaut pour les performances
    public static boolean WORLD_GUARD_ENABLED = false;
    
    private ConfigManager configManager;
    private BiomeListener biomeListener;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        getLogger().info("Starting Biomentry initialization...");

        // Initialisation de la configuration
        initializeConfig();
        
        // Initialisation du gestionnaire de langue
        languageManager = new LanguageManager(this);
        getLogger().info("Language manager initialized.");
        
        // Initialisation du gestionnaire de configuration
        configManager = new ConfigManager(this, languageManager);
        getLogger().info("Configuration loaded and cached.");

        // Initialisation des événements
        biomeListener = new BiomeListener(this, configManager);
        Bukkit.getPluginManager().registerEvents(biomeListener, this);
        getLogger().info("Event listeners registered.");

        // Initialisation des commandes
        initializeCommands();

        // Vérification de WorldGuard
        checkWorldGuardIntegration();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info(String.format("Biomentry v%s initialized successfully in %dms.", VERSION, loadTime));
    }

    @Override
    public void onDisable() {
        getLogger().info(String.format("Biomentry v%s shutting down.", VERSION));
        
        // Nettoyage des ressources si nécessaire
        if (biomeListener != null) {
            // Nettoyer le PriorityManager via le BiomeListener
            biomeListener.cleanup();
            // Le listener sera automatiquement désenregistré
            biomeListener = null;
        }
        
        if (configManager != null) {
            configManager = null;
        }
    }

    private void initializeConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
            getLogger().info("Default configuration created.");
        } else {
            getLogger().info("Configuration file found.");
        }
    }

    private void initializeCommands() {
        try {
            ParentCommand parentCommand = new ParentCommand(this);
            getCommand("biomentry").setExecutor(parentCommand);
            getCommand("biomentry").setTabCompleter(parentCommand);
            getLogger().info("Commands initialized.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize commands", e);
        }
    }

    private void checkWorldGuardIntegration() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            new WGRegionManager(); // Test de création
            WORLD_GUARD_ENABLED = true;
            getLogger().info("WorldGuard integration enabled.");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            WORLD_GUARD_ENABLED = false;
            getLogger().info("WorldGuard not found - region features disabled.");
        } catch (Exception e) {
            WORLD_GUARD_ENABLED = false;
            getLogger().warning("WorldGuard integration failed: " + e.getMessage());
        }
    }

    // Méthode pour recharger la configuration
    public void reloadPluginConfig() {
        reloadConfig();
        if (languageManager != null) {
            languageManager.reloadLanguages();
        }
        if (configManager != null) {
            configManager.reloadConfiguration();
        }
        getLogger().info("Configuration reloaded.");
    }

    // Getters pour accéder aux composants
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public static String getPluginVersion() {
        return VERSION;
    }
}
