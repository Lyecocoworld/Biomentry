package fr.lye.biomentry.Managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.lye.biomentry.ConfigManager;

public class PriorityManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    
    // Stockage des dernières activités de titre/actionbar par joueur
    private final Map<String, Long> lastTitleActivity = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActionBarActivity = new ConcurrentHashMap<>();
    
    // Tâches de surveillance par joueur
    private final Map<String, BukkitTask> monitoringTasks = new ConcurrentHashMap<>();
    
    // Joueurs actuellement en pause
    private final Map<String, Boolean> pausedPlayers = new ConcurrentHashMap<>();
    
    // Stockage des animations externes détectées
    private final Map<String, Long> externalAnimationEndTimes = new ConcurrentHashMap<>();
    
    // Tâche de surveillance des animations externes
    private BukkitTask externalAnimationWatcher;
    
    public PriorityManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        
        // Démarrer la surveillance des animations externes si nécessaire
        if (shouldPauseOnOtherPlugins()) {
            startExternalAnimationWatcher();
        }
    }
    
    /**
     * Vérifie si le système de priorité est activé
     */
    public boolean isPrioritySystemEnabled() {
        return plugin.getConfig().getBoolean("priority.enabled", true);
    }
    
    /**
     * Obtient le niveau de priorité configuré
     */
    public String getPriorityLevel() {
        return plugin.getConfig().getString("priority.level", "medium").toLowerCase();
    }
    
    /**
     * Vérifie si les affichages doivent être mis en pause pour les autres plugins
     */
    public boolean shouldPauseOnOtherPlugins() {
        return plugin.getConfig().getBoolean("priority.pauseOnOtherPlugins", true);
    }
    
    /**
     * Obtient le délai de reprise après qu'un autre plugin ait fini
     */
    public int getResumeDelay() {
        return plugin.getConfig().getInt("priority.resumeDelay", 40);
    }
    
    /**
     * Obtient l'intervalle de vérification
     */
    public int getCheckInterval() {
        return plugin.getConfig().getInt("priority.checkInterval", 5);
    }
    
    /**
     * Vérifie si un joueur peut afficher un message de biome
     */
    public boolean canDisplayBiomeMessage(Player player) {
        if (!isPrioritySystemEnabled()) {
            return true;
        }
        
        // Si pauseOnOtherPlugins est activé et qu'il y a une animation externe, bloquer
        if (shouldPauseOnOtherPlugins() && hasExternalAnimation(player)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Démarre la surveillance d'un joueur pour détecter l'activité d'autres plugins
     */
    public void startMonitoring(Player player) {
        if (!isPrioritySystemEnabled() || !shouldPauseOnOtherPlugins()) {
            return;
        }
        
        String playerName = player.getName();
        
        // Arrêter la surveillance existante si elle existe
        stopMonitoring(player);
        
        BukkitTask task = new BukkitRunnable() {
            private String lastTitle = "";
            private String lastActionBar = "";
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Cette méthode est appelée périodiquement pour détecter les changements
                // Dans une implémentation réelle, on pourrait utiliser des événements ou des hooks
                // Pour détecter quand d'autres plugins affichent des titres/actionbars
                
                // Pour l'instant, on suppose qu'il n'y a pas d'activité externe
                // Cette logique pourrait être étendue avec des hooks vers d'autres plugins
            }
        }.runTaskTimer(plugin, 0L, getCheckInterval());
        
        monitoringTasks.put(playerName, task);
    }
    
    /**
     * Arrête la surveillance d'un joueur
     */
    public void stopMonitoring(Player player) {
        String playerName = player.getName();
        BukkitTask task = monitoringTasks.remove(playerName);
        if (task != null) {
            task.cancel();
        }
        pausedPlayers.remove(playerName);
    }
    
    /**
     * Enregistre une activité de titre externe
     */
    public void recordExternalTitleActivity(Player player) {
        if (!isPrioritySystemEnabled()) {
            return;
        }
        
        String playerName = player.getName();
        lastTitleActivity.put(playerName, System.currentTimeMillis());
        
        // Mettre en pause si nécessaire
        if (shouldPauseOnOtherPlugins() && !getPriorityLevel().equals("high")) {
            pausedPlayers.put(playerName, true);
            
            // Programmer la reprise
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                pausedPlayers.remove(playerName);
            }, getResumeDelay());
        }
    }
    
    /**
     * Enregistre une activité d'actionbar externe
     */
    public void recordExternalActionBarActivity(Player player) {
        if (!isPrioritySystemEnabled()) {
            return;
        }
        
        String playerName = player.getName();
        lastActionBarActivity.put(playerName, System.currentTimeMillis());
        
        // Mettre en pause si nécessaire
        if (shouldPauseOnOtherPlugins() && !getPriorityLevel().equals("high")) {
            pausedPlayers.put(playerName, true);
            
            // Programmer la reprise
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                pausedPlayers.remove(playerName);
            }, getResumeDelay());
        }
    }
    
    /**
     * Nettoie les données d'un joueur qui se déconnecte
     */
    public void cleanupPlayer(Player player) {
        String playerName = player.getName();
        lastTitleActivity.remove(playerName);
        lastActionBarActivity.remove(playerName);
        pausedPlayers.remove(playerName);
        stopMonitoring(player);
    }
    
    /**
     * Vérifie si un joueur est actuellement en pause
     */
    public boolean isPlayerPaused(Player player) {
        return pausedPlayers.getOrDefault(player.getName(), false);
    }
    
    /**
     * Démarre la surveillance des animations externes
     */
    private void startExternalAnimationWatcher() {
        if (externalAnimationWatcher != null && !externalAnimationWatcher.isCancelled()) {
            externalAnimationWatcher.cancel();
        }
        
        externalAnimationWatcher = new BukkitRunnable() {
            @Override
            public void run() {
                // Nettoyer les animations externes expirées
                long currentTime = System.currentTimeMillis();
                externalAnimationEndTimes.entrySet().removeIf(entry -> entry.getValue() < currentTime);
            }
        }.runTaskTimer(plugin, 0L, 5L); // Vérifier toutes les 5 ticks (250ms)
    }
    
    /**
     * Arrête la surveillance des animations externes
     */
    public void stopExternalAnimationWatcher() {
        if (externalAnimationWatcher != null && !externalAnimationWatcher.isCancelled()) {
            externalAnimationWatcher.cancel();
        }
    }
    
    /**
     * Détecte si un autre plugin affiche actuellement un titre
     * @param player Le joueur à vérifier
     * @return true si un autre plugin semble afficher un titre
     */
    private boolean hasExternalAnimation(Player player) {
        String playerName = player.getName();
        
        // Vérifier notre registre d'animations externes
        Long externalEndTime = externalAnimationEndTimes.get(playerName);
        if (externalEndTime != null && System.currentTimeMillis() < externalEndTime) {
            return true;
        }
        
        // Vérifier les métadonnées communes utilisées par d'autres plugins
        String[] commonMetadataKeys = {
            "title_plugin", "custom_title", "displaying_title", 
            "actionbar_plugin", "subtitle_active", "title_active",
            "biomentry_external_animation", "external_title_animation"
        };
        
        for (String key : commonMetadataKeys) {
            if (player.hasMetadata(key)) {
                // Vérifier si la métadonnée a une valeur de temps
                try {
                    org.bukkit.metadata.MetadataValue value = player.getMetadata(key).get(0);
                    if (value.value() instanceof Long) {
                        long endTime = value.asLong();
                        if (System.currentTimeMillis() < endTime) {
                            return true;
                        } else {
                            // Nettoyer la métadonnée expirée
                            player.removeMetadata(key, plugin);
                        }
                    } else {
                        // Métadonnée présente sans temps, supposer qu'elle est active
                        return true;
                    }
                } catch (Exception e) {
                    // En cas d'erreur, supposer que la métadonnée est active
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Enregistre qu'un plugin externe affiche une animation
     * @param player Le joueur
     * @param durationMs Durée de l'animation externe en millisecondes
     */
    public void registerExternalAnimation(Player player, long durationMs) {
        String playerName = player.getName();
        long endTime = System.currentTimeMillis() + durationMs;
        
        // Enregistrer dans notre système
        externalAnimationEndTimes.put(playerName, endTime);
        
        // Définir une métadonnée pour que d'autres plugins puissent aussi la détecter
        player.setMetadata("biomentry_external_animation", 
            new org.bukkit.metadata.FixedMetadataValue(plugin, endTime));
        
        // Programmer le nettoyage de la métadonnée
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("biomentry_external_animation", plugin);
                externalAnimationEndTimes.remove(playerName);
            }
        }.runTaskLater(plugin, (durationMs / 50) + 10); // +10 ticks de sécurité pour le fade
    }
    
    /**
     * Vérifie si le mode pause sur autres plugins est activé et s'il y a une animation externe
     * @param player Le joueur
     * @return true si Biomentry doit être en pause
     */
    public boolean shouldPauseForExternalPlugin(Player player) {
        return shouldPauseOnOtherPlugins() && hasExternalAnimation(player);
    }
    
    /**
     * Nettoie toutes les ressources du gestionnaire
     */
    public void cleanup() {
        stopExternalAnimationWatcher();
        
        // Annuler toutes les tâches de nettoyage
        for (BukkitTask task : monitoringTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        
        // Nettoyer les maps
        lastTitleActivity.clear();
        lastActionBarActivity.clear();
        pausedPlayers.clear();
        externalAnimationEndTimes.clear();
        monitoringTasks.clear();
    }
}
