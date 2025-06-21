package fr.lye.biomentry.Helpers;

import java.awt.Color;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

public class GradientHelper {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final int GRADIENT_STEPS = 16; // Nombre d'étapes pour un dégradé fluide

    public static String applyGradient(String text, String startColorHex, String endColorHex) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Vérifier si les couleurs sont au format hex valide
        if (!HEX_PATTERN.matcher(startColorHex).matches() || !HEX_PATTERN.matcher(endColorHex).matches()) {
            return text;
        }

        // Convertir les hex en couleurs
        Color startColor = Color.decode(startColorHex);
        Color endColor = Color.decode(endColorHex);

        // Récupérer les caractères du texte
        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder();
        int textLength = chars.length;

        // Appliquer le dégradé
        for (int i = 0; i < textLength; i++) {
            if (chars[i] == '§' && i + 1 < textLength) {
                // Préserver les codes de formatage Minecraft
                result.append(chars[i]).append(chars[i + 1]);
                i++; // Sauter le prochain caractère
                continue;
            }

            // Calculer la couleur pour cette position
            float ratio = (float) i / (textLength - 1);
            Color currentColor = interpolateColor(startColor, endColor, ratio);

            // Convertir en format hex Minecraft et ajouter le caractère
            result.append(ChatColor.of(currentColor)).append(chars[i]);
        }

        return result.toString();
    }

    private static Color interpolateColor(Color start, Color end, float ratio) {
        int red = Math.round(start.getRed() + (end.getRed() - start.getRed()) * ratio);
        int green = Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
        int blue = Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);

        return new Color(
            clamp(red, 0, 255),
            clamp(green, 0, 255),
            clamp(blue, 0, 255)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String applyMultiGradient(String text, String... colors) {
        if (text == null || text.isEmpty() || colors.length < 2) {
            return text;
        }

        // Vérifier si toutes les couleurs sont valides
        for (String color : colors) {
            if (!HEX_PATTERN.matcher(color).matches()) {
                return text;
            }
        }

        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder();
        int textLength = chars.length;
        int segments = colors.length - 1;
        int charsPerSegment = textLength / segments;

        for (int i = 0; i < textLength; i++) {
            if (chars[i] == '§' && i + 1 < textLength) {
                result.append(chars[i]).append(chars[i + 1]);
                i++;
                continue;
            }

            // Déterminer dans quel segment se trouve le caractère
            int segment = Math.min(i / charsPerSegment, segments - 1);
            float segmentProgress = (float)(i % charsPerSegment) / charsPerSegment;

            // Obtenir les couleurs pour ce segment
            Color startColor = Color.decode(colors[segment]);
            Color endColor = Color.decode(colors[segment + 1]);

            // Interpoler la couleur
            Color currentColor = interpolateColor(startColor, endColor, segmentProgress);

            // Ajouter la couleur et le caractère
            result.append(ChatColor.of(currentColor)).append(chars[i]);
        }

        return result.toString();
    }
}
