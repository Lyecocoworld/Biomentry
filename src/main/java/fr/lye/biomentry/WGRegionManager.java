package fr.lye.biomentry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WGRegionManager {
    // Cache des RegionManagers par monde pour éviter les lookups répétés
    private final Map<String, RegionManager> regionManagerCache;
    
    // Cache des résultats de vérification de région pour réduire les calculs
    private final Map<String, Boolean> regionCheckCache;
    
    // Taille maximale du cache pour éviter les fuites de mémoire
    private static final int MAX_CACHE_SIZE = 1000;

    public WGRegionManager() {
        this.regionManagerCache = new ConcurrentHashMap<>();
        this.regionCheckCache = new ConcurrentHashMap<>();
    }

    public boolean IsInRegion(Location location, String regionName) {
        if (location == null || regionName == null) {
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        // Création d'une clé unique pour le cache
        String cacheKey = String.format("%s:%d,%d,%d:%s",
            world.getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            regionName);

        // Vérification du cache
        Boolean cachedResult = regionCheckCache.get(cacheKey);
        if (cachedResult != null) {
            // Nettoyage périodique du cache si nécessaire
            if (regionCheckCache.size() > MAX_CACHE_SIZE) {
                regionCheckCache.clear();
            }
            return cachedResult;
        }

        try {
            // Obtention du RegionManager depuis le cache
            RegionManager regionManager = regionManagerCache.computeIfAbsent(
                world.getName(),
                w -> WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))
            );

            if (regionManager == null) {
                regionCheckCache.put(cacheKey, false);
                return false;
            }

            // Vérification de la région
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                regionCheckCache.put(cacheKey, false);
                return false;
            }

            // Vérification si la location est dans la région
            boolean result = region.contains(BukkitAdapter.asBlockVector(location));
            
            // Mise en cache du résultat
            regionCheckCache.put(cacheKey, result);
            
            return result;

        } catch (Exception e) {
            if (Biomentry.DEBUG_MODE) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // Méthode pour vider le cache si nécessaire
    public void clearCache() {
        regionManagerCache.clear();
        regionCheckCache.clear();
    }
}
