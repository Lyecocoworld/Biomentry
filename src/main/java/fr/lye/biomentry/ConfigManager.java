    package fr.lye.biomentry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

import fr.lye.biomentry.Helpers.ColorHelper;
import fr.lye.biomentry.Models.BiomeTitleInfo;
import fr.lye.biomentry.Models.TitleInfo;

public class ConfigManager {
    private final JavaPlugin _plugin;
    private final LanguageManager _languageManager;
    
    // Cache pour optimiser les performances
    private final Map<String, BiomeTitleInfo> _biomeInfoCache;
    private final Map<String, List<String>> _biomeGroupsCache;
    private final List<String> _disabledRegions;
    private final TitleInfo _titleInfo;
    
    // Pattern pour détecter les dégradés au format <gradient:#color1:#color2>text</gradient>
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}(?::#[0-9A-Fa-f]{6})*?)>(.*?)</gradient>");
    
    public ConfigManager(JavaPlugin plugin, LanguageManager languageManager) {
        _plugin = plugin;
        _languageManager = languageManager;
        _biomeInfoCache = new ConcurrentHashMap<>();
        _biomeGroupsCache = new ConcurrentHashMap<>();
        
        // Charger d'abord les infos de titre et les régions désactivées
        _disabledRegions = loadDisabledRegions();
        _titleInfo = loadTitleInfo();
        
        // Puis initialiser le cache des biomes
        loadConfiguration();
    }

    private void loadConfiguration() {
        // Chargement de tous les biomes depuis LanguageManager
        List<String> biomeNames = _languageManager.getAllBiomeNames();
        
        for (String biomeName : biomeNames) {
            var info = new BiomeTitleInfo();
            
            // Charger toutes les informations depuis LanguageManager
            info.title = _languageManager.getBiomeTitle(biomeName);
            if (info.title == null) info.title = "";
            
            info.subtitle = _languageManager.getBiomeSubtitle(biomeName);
            if (info.subtitle == null) info.subtitle = "";
            
            info.display = _languageManager.getBiomeDisplay(biomeName);
            if (info.display == null) {
                info.display = "title"; // valeur par défaut
            }
            
            info.sound = _languageManager.getBiomeSound(biomeName);
            info.particle = _languageManager.getBiomeParticle(biomeName);
            
            // Charger le séparateur spécifique au biome, sinon utiliser le global
            info.separator = _languageManager.getBiomeSeparator(biomeName);
            if (info.separator == null) {
                info.separator = _titleInfo.Separator; // Utiliser le séparateur global par défaut
            }
            
            // Valeurs par défaut pour les autres propriétés
            info.commands = new ArrayList<>();
            info.timeDisplay = "always";
            info.weatherDisplay = "always";
            info.useGradient = false;
            
            _biomeInfoCache.put(biomeName.toLowerCase(), info);
        }
        
        // Chargement des groupes de biomes en cache depuis config.yml
        var conf = _plugin.getConfig();
        if (conf.contains("biomeGroups")) {
            var groups = conf.getList("biomeGroups");
            if (groups != null) {
                for (Object biomes : groups) {
                    if (biomes instanceof List) {
                        List<String> biomeList = (List<String>) biomes;
                        for (String biomeName : biomeList) {
                            _biomeGroupsCache.put(biomeName.toLowerCase(), 
                                biomeList.stream().map(String::toLowerCase).toList());
                        }
                    }
                }
            }
        }
    }

    public BiomeTitleInfo GetBiomeInfo(String biomeName) {
        // Normalisation du nom
        if (biomeName.toLowerCase().startsWith("minecraft:")) {
            biomeName = biomeName.substring(10);
        }
        
        BiomeTitleInfo info = _biomeInfoCache.get(biomeName.toLowerCase());
        if (info != null) {
            // Créer une copie pour éviter de modifier le cache
            BiomeTitleInfo processedInfo = new BiomeTitleInfo();
            processedInfo.title = processColors(info.title);
            processedInfo.subtitle = processColors(info.subtitle);
            processedInfo.display = info.display;
            processedInfo.sound = info.sound;
            processedInfo.particle = info.particle;
            processedInfo.separator = info.separator;
            processedInfo.commands = info.commands;
            processedInfo.timeDisplay = info.timeDisplay;
            processedInfo.weatherDisplay = info.weatherDisplay;
            processedInfo.useGradient = info.useGradient;
            processedInfo.gradientStartColor = info.gradientStartColor;
            processedInfo.gradientEndColor = info.gradientEndColor;
            
            return processedInfo;
        }
        
        return null;
    }

    private String processColors(String text) {
        return ColorHelper.processAllColors(text);
    }

    public TitleInfo GetTitleInfo() {
        return _titleInfo;
    }

    private TitleInfo loadTitleInfo() {
        var conf = _plugin.getConfig();
        var info = new TitleInfo();
        info.FadeIn = conf.getInt("titleInfo.fadeIn", 10);
        info.Stay = conf.getInt("titleInfo.stay", 70);
        info.FadeOut = conf.getInt("titleInfo.fadeOut", 20);
        info.AnimationType = conf.getString("titleInfo.animationType", "fade");
        info.TypewriterSpeed = conf.getInt("titleInfo.typewriterSpeed", 2);
        info.Separator = conf.getString("titleInfo.separator", "§r§f| ");
        return info;
    }

    public List<String> GetBiomeGroups(String biomeName) {
        List<String> groups = _biomeGroupsCache.get(biomeName.toLowerCase());
        if (groups == null) {
            return new ArrayList<>();
        }
        
        // Retourner une copie sans le biome actuel
        List<String> result = new ArrayList<>(groups);
        result.removeIf(x -> x.equalsIgnoreCase(biomeName));
        
        if (Biomentry.DEBUG_MODE) {
            _plugin.getLogger().info(String.format("Found %s biomes that match group of %s", result.size(), biomeName));
        }
        
        return result;
    }

    public List<String> GetDisabledRegions() {
        return _disabledRegions;
    }

    private List<String> loadDisabledRegions() {
        var conf = _plugin.getConfig();
        if (!conf.contains("disabledRegions")) {
            return new ArrayList<>();
        }
        return new ArrayList<>(conf.getStringList("disabledRegions"));
    }

    // Méthode pour recharger la configuration
    public void reloadConfiguration() {
        _biomeInfoCache.clear();
        _biomeGroupsCache.clear();
        loadConfiguration();
    }
}
