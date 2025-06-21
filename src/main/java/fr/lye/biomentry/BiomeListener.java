package fr.lye.biomentry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.lye.biomentry.Animations.TitleAnimation;
import fr.lye.biomentry.Managers.PriorityManager;
import fr.lye.biomentry.Models.BiomeTitleInfo;
import fr.lye.biomentry.Models.TitleInfo;

public class BiomeListener implements Listener {

    private final JavaPlugin _plugin;
    private final ConfigManager _config;
    private final Map<String, Biome> _lastBiomes;
    private final Map<String, Integer> _lastYLevels;
    private final Map<String, String> _lastEffectiveBiomes;
    private final Map<String, Long> _lastNotificationTimes;
    private static final long NOTIFICATION_COOLDOWN = 2000;
    private final TitleInfo _titleInfo;
    private final WGRegionManager _regionManager;
    private final PriorityManager _priorityManager;

    public BiomeListener(JavaPlugin plugin, ConfigManager config) {
        _plugin = plugin;
        _config = config;
        _lastBiomes = new ConcurrentHashMap<>();
        _lastYLevels = new ConcurrentHashMap<>();
        _lastEffectiveBiomes = new ConcurrentHashMap<>();
        _lastNotificationTimes = new ConcurrentHashMap<>();
        _titleInfo = config.GetTitleInfo();
        _regionManager = Biomentry.WORLD_GUARD_ENABLED ? new WGRegionManager() : null;
        _priorityManager = new PriorityManager(plugin, config);
    }

    /**
     * Détermine si le joueur est dans une grotte en analysant plusieurs paramètres
     * @param location La position du joueur
     * @return true si le joueur est considéré comme étant dans une grotte
     */
    private boolean isInCave(Location location) {
        int yLevel = location.getBlockY();
        
        // RÈGLE ABSOLUE : Jamais de grotte en surface (Y > 55)
        if (yLevel > 55) {
            return false;
        }
        
        // Vérifications anti-faux positifs pour les structures en surface
        if (yLevel > 40) {
            // Entre Y=40 et Y=55, vérifications très strictes
            if (hasArtificialStructure(location)) {
                return false; // Structure artificielle détectée
            }
            if (!hasDeepUndergroundEnvironment(location)) {
                return false; // Pas assez profond/naturel
            }
        }
        
        // Critères multiples pour une détection plus précise
        boolean hasLowLight = hasLowLightLevel(location);
        boolean hasEnclosedEnvironment = hasEnclosedEnvironment(location);
        boolean hasNaturalCaveStructure = hasNaturalCaveStructure(location);
        boolean isUndergroundLevel = yLevel <= 50;
        boolean hasDirectSky = hasDirectSkyAccess(location);
        boolean isDeepUnderground = yLevel <= 30; // Vraiment souterrain
        
        // Système de score pour déterminer si c'est une grotte
        int caveScore = 0;
        
        // Luminosité faible = +2 points
        if (hasLowLight) caveScore += 2;
        
        // Environnement fermé = +3 points
        if (hasEnclosedEnvironment) caveScore += 3;
        
        // Structure naturelle de grotte = +3 points (augmenté)
        if (hasNaturalCaveStructure) caveScore += 3;
        
        // Niveau souterrain = +1 point
        if (isUndergroundLevel) caveScore += 1;
        
        // Pas d'accès direct au ciel = +3 points (augmenté)
        if (!hasDirectSky) caveScore += 3;
        
        // Bonus pour les niveaux vraiment profonds
        if (isDeepUnderground) caveScore += 3; // Bonus important pour Y <= 30
        if (yLevel <= 20) caveScore += 2;
        if (yLevel <= 0) caveScore += 2;
        
        // Malus pour les niveaux trop hauts
        if (yLevel > 45) caveScore -= 3;
        if (yLevel > 50) caveScore -= 5;
        
        // Debug
        if (Biomentry.DEBUG_MODE) {
            _plugin.getLogger().info(String.format(
                "Cave detection - Y:%d, Light:%s, Enclosed:%s, Structure:%s, Underground:%s, NoSky:%s, Deep:%s, Score:%d", 
                yLevel, hasLowLight, hasEnclosedEnvironment, hasNaturalCaveStructure, isUndergroundLevel, !hasDirectSky, isDeepUnderground, caveScore));
        }
        
        // Score >= 8 = grotte détectée (seuil plus élevé)
        return caveScore >= 8;
    }

    /**
     * Vérifie le niveau de luminosité autour du joueur
     * @param location La position du joueur
     * @return true si la luminosité est faible (caractéristique des grottes)
     */
    private boolean hasLowLightLevel(Location location) {
        int lightLevel = location.getBlock().getLightLevel();
        int skyLight = location.getBlock().getLightFromSky();
        int blockLight = location.getBlock().getLightFromBlocks();
        
        // Grotte si luminosité totale faible ET peu de lumière du ciel
        return lightLevel <= 7 && skyLight <= 3;
    }

    /**
     * Vérifie si le joueur a un accès direct au ciel (version améliorée)
     * @param location La position du joueur
     * @return true si le joueur peut voir le ciel directement
     */
    private boolean hasDirectSkyAccess(Location location) {
        int maxY = location.getWorld().getMaxHeight();
        int playerY = location.getBlockY();
        int solidBlocksFound = 0;
        
        // Vérifier plusieurs colonnes autour du joueur pour plus de précision
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                int x = location.getBlockX() + xOffset;
                int z = location.getBlockZ() + zOffset;
                
                // Vérifier jusqu'à 15 blocs au-dessus
                for (int y = playerY + 2; y <= Math.min(playerY + 15, maxY); y++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    if (block.getType().isSolid()) {
                        solidBlocksFound++;
                        break; // Arrêter pour cette colonne
                    }
                }
            }
        }
        
        // Si la majorité des colonnes ont des blocs solides, pas d'accès au ciel
        return solidBlocksFound <= 3; // Sur 9 colonnes, max 3 peuvent avoir des blocs
    }

    /**
     * Analyse l'environnement fermé autour du joueur
     * @param location La position du joueur
     * @return true si l'environnement est fermé comme une grotte
     */
    private boolean hasEnclosedEnvironment(Location location) {
        int solidBlocks = 0;
        int totalBlocks = 0;
        int radius = 4; // Rayon plus large pour plus de précision
        
        int playerX = location.getBlockX();
        int playerY = location.getBlockY();
        int playerZ = location.getBlockZ();
        
        // Analyser un volume plus large autour du joueur
        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int y = playerY - 2; y <= playerY + 4; y++) {
                for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                    // Ignorer l'espace immédiat du joueur
                    if (Math.abs(x - playerX) <= 1 && Math.abs(y - playerY) <= 1 && Math.abs(z - playerZ) <= 1) {
                        continue;
                    }
                    
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    totalBlocks++;
                    
                    if (block.getType().isSolid()) {
                        solidBlocks++;
                    }
                }
            }
        }
        
        // Seuil plus bas pour détecter des grottes plus petites
        double solidRatio = (double) solidBlocks / totalBlocks;
        return solidRatio > 0.45; // Réduit de 0.6 à 0.45
    }

    /**
     * Vérifie si la structure autour ressemble à une grotte naturelle
     * @param location La position du joueur
     * @return true si la structure ressemble à une grotte naturelle
     */
    private boolean hasNaturalCaveStructure(Location location) {
        int playerX = location.getBlockX();
        int playerY = location.getBlockY();
        int playerZ = location.getBlockZ();
        
        // Vérifier la présence de matériaux typiques des grottes
        int stoneBlocks = 0;
        int airBlocks = 0;
        int totalChecked = 0;
        
        for (int x = playerX - 3; x <= playerX + 3; x++) {
            for (int y = playerY - 2; y <= playerY + 3; y++) {
                for (int z = playerZ - 3; z <= playerZ + 3; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    Material type = block.getType();
                    totalChecked++;
                    
                    // Compter les blocs de pierre/terre (typiques des grottes)
                    if (type == Material.STONE || type == Material.DEEPSLATE || 
                        type == Material.GRANITE || type == Material.DIORITE || 
                        type == Material.ANDESITE || type == Material.DIRT ||
                        type == Material.GRAVEL || type == Material.COBBLESTONE) {
                        stoneBlocks++;
                    }
                    
                    // Compter les blocs d'air
                    if (type == Material.AIR || type == Material.CAVE_AIR) {
                        airBlocks++;
                    }
                }
            }
        }
        
        // Structure de grotte = beaucoup de pierre ET espaces d'air
        double stoneRatio = (double) stoneBlocks / totalChecked;
        double airRatio = (double) airBlocks / totalChecked;
        
        return stoneRatio > 0.3 && airRatio > 0.2;
    }

    /**
     * Vérifie si le biome donné est un biome de grotte spécifique
     * @param biomeName Le nom du biome
     * @return true si c'est un biome de grotte spécifique
     */
    private boolean isSpecificCaveBiome(String biomeName) {
        return biomeName.equals("dripstone_caves") || 
               biomeName.equals("lush_caves") || 
               biomeName.equals("deep_dark");
    }

    /**
     * Détecte la présence de structures artificielles (villages, bâtiments, etc.)
     * @param location La position du joueur
     * @return true si une structure artificielle est détectée
     */
    private boolean hasArtificialStructure(Location location) {
        int playerX = location.getBlockX();
        int playerY = location.getBlockY();
        int playerZ = location.getBlockZ();
        int radius = 3;
        
        // Matériaux typiques des structures artificielles
        int artificialBlocks = 0;
        int totalBlocks = 0;
        
        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int y = playerY - 1; y <= playerY + 3; y++) {
                for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    Material type = block.getType();
                    totalBlocks++;
                    
                    // Matériaux typiques des constructions
                    if (type == Material.OAK_PLANKS || type == Material.GLASS ||
                        type == Material.BRICKS || type == Material.SMOOTH_STONE ||
                        type == Material.STONE_STAIRS || type == Material.OAK_DOOR ||
                        type == Material.CRAFTING_TABLE || type == Material.FURNACE ||
                        type == Material.CHEST || type == Material.BOOKSHELF ||
                        type == Material.WALL_TORCH || type == Material.LADDER ||
                        type == Material.OAK_STAIRS || type == Material.OAK_FENCE ||
                        type == Material.STONE_BRICKS || type == Material.STONE_SLAB ||
                        // Ajout d'autres variantes de bois et matériaux de construction
                        type == Material.SPRUCE_PLANKS || type == Material.BIRCH_PLANKS ||
                        type == Material.SPRUCE_DOOR || type == Material.BIRCH_DOOR ||
                        type == Material.SPRUCE_STAIRS || type == Material.BIRCH_STAIRS ||
                        type == Material.SPRUCE_FENCE || type == Material.BIRCH_FENCE ||
                        // Matériaux de village
                        type == Material.COBBLESTONE || type == Material.COBBLESTONE_STAIRS ||
                        type == Material.STONE_BRICK_STAIRS || type == Material.GLASS_PANE ||
                        type == Material.ACACIA_DOOR || type == Material.DARK_OAK_DOOR ||
                        type == Material.JUNGLE_DOOR || type == Material.IRON_DOOR) {
                        artificialBlocks++;
                    }
                }
            }
        }
        
        // Si plus de 15% des blocs sont artificiels, c'est probablement une structure
        double artificialRatio = (double) artificialBlocks / totalBlocks;
        return artificialRatio > 0.15;
    }

    /**
     * Vérifie si l'environnement ressemble à une grotte profonde naturelle
     * @param location La position du joueur
     * @return true si l'environnement est typique d'une grotte profonde
     */
    private boolean hasDeepUndergroundEnvironment(Location location) {
        int playerX = location.getBlockX();
        int playerY = location.getBlockY();
        int playerZ = location.getBlockZ();
        int radius = 4;
        
        int naturalBlocks = 0;
        int totalBlocks = 0;
        boolean hasWaterOrLava = false;
        boolean hasOres = false;
        
        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int y = playerY - 2; y <= playerY + 3; y++) {
                for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    Material type = block.getType();
                    totalBlocks++;
                    
                    // Matériaux naturels des grottes profondes
                    if (type == Material.STONE || type == Material.DEEPSLATE ||
                        type == Material.GRANITE || type == Material.DIORITE ||
                        type == Material.ANDESITE || type == Material.TUFF ||
                        type == Material.CALCITE || type == Material.DRIPSTONE_BLOCK) {
                        naturalBlocks++;
                    }
                    
                    // Présence d'eau ou de lave (typique des grottes naturelles)
                    if (type == Material.WATER || type == Material.LAVA) {
                        hasWaterOrLava = true;
                    }
                    
                    // Présence de minerais (typique des grottes naturelles)
                    if (type.name().endsWith("_ORE")) {
                        hasOres = true;
                    }
                }
            }
        }
        
        double naturalRatio = (double) naturalBlocks / totalBlocks;
        
        // Critères multiples pour une grotte naturelle profonde :
        // - Au moins 40% de blocs naturels
        // - Présence d'eau/lave OU de minerais
        return naturalRatio > 0.4 && (hasWaterOrLava || hasOres);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && 
                          from.getBlockZ() == to.getBlockZ() && 
                          from.getBlockY() == to.getBlockY())) {
            return;
        }

        if (!fr.lye.biomentry.Models.PlayerPreferences.areNotificationsEnabled(e.getPlayer().getUniqueId())) {
            return;
        }

        String playerName = e.getPlayer().getName();
        
        long currentTime = System.currentTimeMillis();
        Long lastNotification = _lastNotificationTimes.get(playerName);
        if (lastNotification != null && currentTime - lastNotification < NOTIFICATION_COOLDOWN) {
            return;
        }

        String biomeName = to.getBlock().getBiome().toString().toLowerCase();
        int yLevel = to.getBlockY();
        
        // Détection dynamique des grottes
        boolean playerInCave = isInCave(to);
        boolean isSpecificCave = isSpecificCaveBiome(biomeName);
        
        // Déterminer le biome effectif avec la nouvelle logique
        String effectiveBiomeName;
        
        if (playerInCave) {
            // Le joueur est dans une grotte (détection dynamique)
            if (isSpecificCave) {
                // Biome de grotte spécifique (dripstone_caves, lush_caves, deep_dark)
                effectiveBiomeName = biomeName;
            } else {
                // Grotte générique
                effectiveBiomeName = "cave";
            }
        } else {
            // Le joueur n'est pas dans une grotte
            if (isSpecificCave || biomeName.equals("cave")) {
                // Ignorer les biomes de grotte si le joueur n'est pas détecté comme étant en grotte
                return;
            }
            effectiveBiomeName = biomeName;
        }

        // Vérifier si le biome effectif a changé
        String lastEffectiveBiome = _lastEffectiveBiomes.get(playerName);
        boolean changedEffectiveBiome = lastEffectiveBiome == null || !effectiveBiomeName.equals(lastEffectiveBiome);

        // Si pas de changement de biome effectif, on sort
        if (!changedEffectiveBiome) {
            return;
        }

        // Mise à jour des caches
        _lastYLevels.put(playerName, yLevel);
        _lastEffectiveBiomes.put(playerName, effectiveBiomeName);
        _lastBiomes.put(playerName, to.getBlock().getBiome());

        // Vérifier les groupes de biomes
        if (lastEffectiveBiome != null && 
            _config.GetBiomeGroups(lastEffectiveBiome).stream()
                .anyMatch(x -> x.equalsIgnoreCase(effectiveBiomeName))) {
            return;
        }

        // Debug mode
        if (Biomentry.DEBUG_MODE) {
            _plugin.getLogger().info(String.format("Player '%s' entering %s at Y=%d (in cave: %s, specific cave: %s)", 
                playerName, effectiveBiomeName, yLevel, playerInCave, isSpecificCave));
        }

        BiomeTitleInfo info = _config.GetBiomeInfo(effectiveBiomeName);
        if (info == null) {
            return;
        }

        // Vérification WorldGuard
        if (_regionManager != null) {
            for (String region : _config.GetDisabledRegions()) {
                if (_regionManager.IsInRegion(to, region)) {
                    if (Biomentry.DEBUG_MODE) {
                        _plugin.getLogger().info(String.format("Player '%s' is in disabled region '%s'.", playerName, region));
                    }
                    return;
                }
            }
        }

        // Debug mode
        if (Biomentry.DEBUG_MODE) {
            _plugin.getLogger().info(String.format("Found details for '%s': '%s', '%s', display: %s", 
                effectiveBiomeName, info.title, info.subtitle, info.display));
        }

        // Vérifier si l'animation peut être démarrée selon le système de priorité
        if (!_priorityManager.canDisplayBiomeMessage(e.getPlayer())) {
            return;
        }

        // Mise à jour du temps de dernière notification
        _lastNotificationTimes.put(playerName, currentTime);

        // Animation du titre avec gestion des animations précédentes
        TitleAnimation.startFor(_plugin, e.getPlayer(), info, _titleInfo, _priorityManager);

        // Jouer le son configuré si disponible
        if (info.sound != null && !info.sound.isEmpty()) {
            try {
                var soundEnum = org.bukkit.Sound.valueOf(info.sound);
                var player = e.getPlayer();
                player.playSound(player.getLocation(), soundEnum, org.bukkit.SoundCategory.MASTER, 5.0f, 1.0f);
                if (Biomentry.DEBUG_MODE) {
                    _plugin.getLogger().info(String.format(
                        "Playing sound: %s for biome: %s (volume=5.0, category=MASTER)",
                        info.sound, effectiveBiomeName
                    ));
                }
            } catch (IllegalArgumentException ex) {
                _plugin.getLogger().warning("Invalid sound name '" + info.sound + "' for biome '" + effectiveBiomeName + "'");
            }
        }
    }

    /**
     * Nettoie les ressources du BiomeListener
     */
    public void cleanup() {
        if (_priorityManager != null) {
            _priorityManager.cleanup();
        }
    }
}
