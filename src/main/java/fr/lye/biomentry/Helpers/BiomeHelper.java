package fr.lye.biomentry.Helpers;

import org.bukkit.Location;
import org.bukkit.block.Biome;

public class BiomeHelper {
    
    /**
     * Détermine le biome effectif à afficher en fonction de la position Y
     * @param location La position du joueur
     * @param actualBiome Le biome réel détecté par Minecraft
     * @return Le nom du biome à utiliser pour l'affichage
     */
    public static String getEffectiveBiome(Location location, Biome actualBiome) {
        String biomeName = actualBiome.toString().toLowerCase();
        int yLevel = location.getBlockY();
        
        if (yLevel <= 30) {
            // En dessous de Y=30, logique des grottes
            if (isCaveBiome(biomeName)) {
                // Si c'est un biome de grotte spécial, on le garde
                return biomeName;
            } else {
                // Sinon, on utilise le biome "cave" générique
                return "cave";
            }
        } else {
            // Au-dessus de Y=30, logique de surface
            if (isCaveBiome(biomeName)) {
                // On ignore les biomes de grotte en surface
                return null;
            } else {
                // Biome de surface normal
                return biomeName;
            }
        }
    }
    
    /**
     * Vérifie si un biome est un biome de grotte
     * @param biomeName Le nom du biome
     * @return true si c'est un biome de grotte
     */
    public static boolean isCaveBiome(String biomeName) {
        return biomeName.equals("dripstone_caves") || 
               biomeName.equals("lush_caves") || 
               biomeName.equals("deep_dark") || 
               biomeName.equals("cave");
    }
    
    /**
     * Génère une clé unique pour le cache qui inclut la position Y
     * @param playerName Le nom du joueur
     * @param location La position
     * @return Une clé unique pour le cache
     */
    public static String getCacheKey(String playerName, Location location) {
        boolean isUnderground = location.getBlockY() <= 30;
        return playerName + "_" + (isUnderground ? "underground" : "surface");
    }
}
