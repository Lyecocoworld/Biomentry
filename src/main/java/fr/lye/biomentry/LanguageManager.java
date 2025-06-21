package fr.lye.biomentry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gestionnaire des langues pour Biomentry
 * Gère le chargement et l'accès aux fichiers de langue
 */
public class LanguageManager {
    
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languages;
    private String currentLanguage;
    private FileConfiguration currentConfig;
    
    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        
        // Charger la langue depuis la configuration
        String configLanguage = plugin.getConfig().getString("general.language", "en");
        this.currentLanguage = configLanguage;
        
        loadLanguages();
        setLanguage(currentLanguage);
    }
    
    /**
     * Charge tous les fichiers de langue disponibles
     */
    private void loadLanguages() {
        // Langues supportées
        String[] supportedLanguages = {"fr", "en"};
        
        for (String lang : supportedLanguages) {
            loadLanguage(lang);
        }
    }
    
    /**
     * Charge un fichier de langue spécifique
     * @param language Code de la langue (fr, en, etc.)
     */
    private void loadLanguage(String language) {
        try {
            File languageFile = new File(plugin.getDataFolder(), "languages/" + language + ".yml");
            FileConfiguration config;
            
            // Si le fichier n'existe pas, le créer depuis les ressources
            if (!languageFile.exists()) {
                plugin.saveResource("languages/" + language + ".yml", false);
            }
            
            // Charger le fichier
            config = YamlConfiguration.loadConfiguration(languageFile);
            
            // Charger les valeurs par défaut depuis les ressources
            InputStream defConfigStream = plugin.getResource("languages/" + language + ".yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defConfig);
            }
            
            languages.put(language, config);
            plugin.getLogger().info("Langue chargée: " + language);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement de la langue " + language, e);
        }
    }
    
    /**
     * Définit la langue actuelle
     * @param language Code de la langue
     */
    public void setLanguage(String language) {
        if (languages.containsKey(language)) {
            this.currentLanguage = language;
            this.currentConfig = languages.get(language);
            plugin.getLogger().info("Langue définie sur: " + language);
        } else {
            plugin.getLogger().warning("Langue non supportée: " + language + ". Utilisation de 'en' par défaut.");
            this.currentLanguage = "en";
            this.currentConfig = languages.get("en");
        }
    }
    
    /**
     * Obtient la langue actuelle
     * @return Code de la langue actuelle
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Obtient le titre d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Titre formaté ou null si non trouvé
     */
    public String getBiomeTitle(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".title");
    }
    
    /**
     * Obtient le sous-titre d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Sous-titre formaté ou null si non trouvé
     */
    public String getBiomeSubtitle(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".subtitle");
    }

    /**
     * Obtient le paramètre display d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Valeur display ou null si non trouvé
     */
    public String getBiomeDisplay(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".display");
    }

    /**
     * Obtient le paramètre sound d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Valeur sound ou null si non trouvé
     */
    public String getBiomeSound(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".sound");
    }

    /**
     * Obtient le paramètre particle d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Valeur particle ou null si non trouvé
     */
    public String getBiomeParticle(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".particle");
    }

    /**
     * Obtient le séparateur d'un biome dans la langue actuelle
     * @param biome Nom du biome
     * @return Séparateur ou null si non trouvé
     */
    public String getBiomeSeparator(String biome) {
        if (currentConfig == null) return null;
        return currentConfig.getString("biomes." + biome + ".separator");
    }
    
    /**
     * Vérifie si un biome existe dans la configuration de langue
     * @param biome Nom du biome
     * @return true si le biome existe
     */
    public boolean hasBiome(String biome) {
        if (currentConfig == null) return false;
        return currentConfig.contains("biomes." + biome);
    }
    
    /**
     * Obtient la liste de tous les noms de biomes disponibles
     * @return Liste des noms de biomes
     */
    public List<String> getAllBiomeNames() {
        List<String> biomeNames = new ArrayList<>();
        if (currentConfig == null) return biomeNames;
        
        var biomesSection = currentConfig.getConfigurationSection("biomes");
        if (biomesSection != null) {
            biomeNames.addAll(biomesSection.getKeys(false));
        }
        
        return biomeNames;
    }
    
    /**
     * Recharge tous les fichiers de langue
     */
    public void reloadLanguages() {
        languages.clear();
        
        // Relire la langue depuis la configuration mise à jour
        String configLanguage = plugin.getConfig().getString("general.language", "en");
        this.currentLanguage = configLanguage;
        
        loadLanguages();
        setLanguage(currentLanguage);
    }
    
    /**
     * Obtient la liste des langues disponibles
     * @return Array des codes de langue disponibles
     */
    public String[] getAvailableLanguages() {
        return languages.keySet().toArray(new String[0]);
    }
    
    /**
     * Sauvegarde la configuration de langue actuelle
     */
    public void saveCurrentLanguage() {
        if (currentConfig == null) return;
        
        try {
            File languageFile = new File(plugin.getDataFolder(), "languages/" + currentLanguage + ".yml");
            currentConfig.save(languageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde de la langue " + currentLanguage, e);
        }
    }
}
