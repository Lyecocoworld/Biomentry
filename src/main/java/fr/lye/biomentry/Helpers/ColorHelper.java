package fr.lye.biomentry.Helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper pour gérer tous les formats de couleurs Minecraft
 * Supporte les codes &, les balises nominales et les dégradés
 */
public class ColorHelper {
    
    // Pattern pour les balises nominales <color>
    private static final Pattern NAMED_COLOR_PATTERN = Pattern.compile("<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|bold|italic|underlined|strikethrough|obfuscated|reset)>");
    
    // Pattern pour les dégradés <gradient:#color1:#color2>text</gradient>
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}(?::#[0-9A-Fa-f]{6})*?)>(.*?)</gradient>");
    
    // Mapping des couleurs nominales vers les codes &
    private static final String[][] COLOR_MAPPINGS = {
        {"black", "0"}, {"dark_blue", "1"}, {"dark_green", "2"}, {"dark_aqua", "3"},
        {"dark_red", "4"}, {"dark_purple", "5"}, {"gold", "6"}, {"gray", "7"},
        {"dark_gray", "8"}, {"blue", "9"}, {"green", "a"}, {"aqua", "b"},
        {"red", "c"}, {"light_purple", "d"}, {"yellow", "e"}, {"white", "f"},
        {"bold", "l"}, {"italic", "o"}, {"underlined", "n"}, {"strikethrough", "m"},
        {"obfuscated", "k"}, {"reset", "r"}
    };
    
    /**
     * Convertit tous les formats de couleurs en codes § Minecraft
     * @param text Texte avec différents formats de couleurs
     * @return Texte avec codes § uniquement
     */
    public static String processAllColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 1. Traiter les dégradés en premier
        text = processGradients(text);
        
        // 2. Traiter les balises nominales
        text = processNamedColors(text);
        
        // 3. Traiter les codes & classiques
        text = processLegacyColors(text);
        
        return text;
    }
    
    /**
     * Traite les dégradés <gradient:#color1:#color2>text</gradient>
     */
    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String colors = matcher.group(1);
            String content = matcher.group(2);
            
            // Séparer les couleurs
            String[] colorArray = colors.split(":");
            
            if (colorArray.length >= 2) {
                // Appliquer le dégradé avec GradientHelper
                String gradientText = GradientHelper.applyGradient(content, colorArray[0], colorArray[1]);
                matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
            } else {
                // Si le format n'est pas correct, garder le texte original
                matcher.appendReplacement(result, Matcher.quoteReplacement(content));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Traite les balises nominales <color>
     */
    private static String processNamedColors(String text) {
        Matcher matcher = NAMED_COLOR_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String colorName = matcher.group(1);
            String colorCode = getColorCode(colorName);
            
            if (colorCode != null) {
                matcher.appendReplacement(result, "§" + colorCode);
            } else {
                // Si la couleur n'est pas trouvée, garder le texte original
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Traite les codes & classiques
     */
    private static String processLegacyColors(String text) {
        return text.replace("&", "§");
    }
    
    /**
     * Obtient le code couleur pour un nom donné
     */
    private static String getColorCode(String colorName) {
        for (String[] mapping : COLOR_MAPPINGS) {
            if (mapping[0].equals(colorName)) {
                return mapping[1];
            }
        }
        return null;
    }
    
    /**
     * Supprime tous les codes de couleur d'un texte
     * @param text Texte avec codes de couleur
     * @return Texte sans codes de couleur
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Supprimer les dégradés
        text = GRADIENT_PATTERN.matcher(text).replaceAll("$2");
        
        // Supprimer les balises nominales
        text = NAMED_COLOR_PATTERN.matcher(text).replaceAll("");
        
        // Supprimer les codes § et &
        text = text.replaceAll("[§&][0-9a-fk-or]", "");
        
        return text;
    }
    
    /**
     * Vérifie si un texte contient des codes de couleur
     * @param text Texte à vérifier
     * @return true si le texte contient des codes de couleur
     */
    public static boolean hasColors(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return GRADIENT_PATTERN.matcher(text).find() ||
               NAMED_COLOR_PATTERN.matcher(text).find() ||
               text.matches(".*[§&][0-9a-fk-or].*");
    }
}
