package fr.lye.biomentry.Models;

import java.util.List;

public class BiomeTitleInfo {
    public String title;
    public String subtitle;
    public String display;
    public String titleDisplay;    // Mode d'affichage spécifique pour le titre
    public String subtitleDisplay; // Mode d'affichage spécifique pour le sous-titre
    public String sound;
    public String particle;
    public String separator;  // Séparateur personnalisé pour ce biome
    public List<String> commands;
    public String timeDisplay;
    public String weatherDisplay;
    public boolean useGradient;
    public String gradientStartColor;
    public String gradientEndColor;
}
