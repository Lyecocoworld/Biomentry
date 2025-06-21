package fr.lye.biomentry.Animations;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import fr.lye.biomentry.Managers.PriorityManager;
import fr.lye.biomentry.Models.BiomeTitleInfo;
import fr.lye.biomentry.Models.TitleInfo;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class TitleAnimation {
    private final JavaPlugin plugin;
    private final Player player;
    private final BiomeTitleInfo biomeInfo;
    private final TitleInfo titleInfo;
    private final PriorityManager priorityManager;
    private BukkitRunnable task;
    private int currentCharIndex = 0;

    public TitleAnimation(JavaPlugin plugin, Player player, BiomeTitleInfo biomeInfo, TitleInfo titleInfo, PriorityManager priorityManager) {
        this.plugin = plugin;
        this.player = player;
        this.biomeInfo = biomeInfo;
        this.titleInfo = titleInfo;
        this.priorityManager = priorityManager;
    }

    public static void startFor(JavaPlugin plugin, Player player, BiomeTitleInfo biomeInfo, TitleInfo titleInfo, PriorityManager priorityManager) {
        TitleAnimation animation = new TitleAnimation(plugin, player, biomeInfo, titleInfo, priorityManager);
        AnimationManager.startAnimation(animation, player);
    }

    public void start() {
        // Vérifier si l'affichage peut se faire selon le système de priorité
        if (priorityManager != null && priorityManager.shouldPauseForExternalPlugin(player)) {
            // Si l'affichage n'est pas autorisé, programmer une nouvelle tentative
            plugin.getServer().getScheduler().runTaskLater(plugin, this::start, priorityManager.getResumeDelay());
            return;
        }
        
        // Effacer tous les affichages précédents avant de commencer
        clearAllDisplays();
        
        // Nettoyer les textes pour l'animation (enlever les codes couleur pour compter les caractères)
        String cleanTitle = stripColorCodes(biomeInfo.title);
        String cleanSubtitle = stripColorCodes(biomeInfo.subtitle);
        int maxLength = Math.max(cleanTitle.length(), cleanSubtitle.length());
        
        if (maxLength == 0) {
            showWithFade();
            return;
        }
    
        task = new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier à chaque tick si l'affichage peut continuer
                if (priorityManager != null && !priorityManager.canDisplayBiomeMessage(player)) {
                    // Mettre en pause l'animation
                    this.cancel();
                    // Programmer une reprise
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> start(), priorityManager.getResumeDelay());
                    return;
                }
                
                currentCharIndex += titleInfo.TypewriterSpeed;
                
                // Créer les versions partielles en préservant les codes couleur
                String currentTitle = getPartialText(biomeInfo.title, Math.min(currentCharIndex, cleanTitle.length()));
                String currentSubtitle = getPartialText(biomeInfo.subtitle, Math.min(currentCharIndex, cleanSubtitle.length()));
                
                // Afficher selon le type
                displayMessage(currentTitle, currentSubtitle);
                
                // Vérifier si l'animation est terminée
                if (currentCharIndex >= maxLength) {
                    this.cancel();
                    // Afficher le message final complet
                    displayMessage(biomeInfo.title, biomeInfo.subtitle);
                }
            }
        };
        
        // Démarrer l'animation avec un délai de 2 ticks (configurable)
        task.runTaskTimer(plugin, 0L, Math.max(1L, 3L - titleInfo.TypewriterSpeed));
    }

    private void displayMessage(String title, String subtitle) {
        // Déterminer les modes d'affichage pour le titre et le sous-titre
        String titleDisplay = biomeInfo.titleDisplay != null ? biomeInfo.titleDisplay.toLowerCase() : biomeInfo.display.toLowerCase();
        String subtitleDisplay = biomeInfo.subtitleDisplay != null ? biomeInfo.subtitleDisplay.toLowerCase() : biomeInfo.display.toLowerCase();

        // Cas spécial : si les deux utilisent le même mode d'affichage "title"
        if (titleDisplay.equals("title") && subtitleDisplay.equals("title")) {
            player.sendTitle(title, subtitle, 0, 60, 10);
            return;
        }

        // Cas spécial : si les deux utilisent le même mode d'affichage "actionbar"
        if (titleDisplay.equals("actionbar") && subtitleDisplay.equals("actionbar")) {
            if (!title.isEmpty() && !subtitle.isEmpty()) {
                String message = String.format("%s%s%s", title, biomeInfo.separator, subtitle);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            } else if (!title.isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(title));
            } else if (!subtitle.isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(subtitle));
            }
            return;
        }

        // Afficher le titre selon son mode
        if (!title.isEmpty()) {
            switch (titleDisplay) {
                case "title":
                    player.sendTitle(title, "", 0, 60, 10);
                    break;
                case "chat":
                    player.spigot().sendMessage(ChatMessageType.CHAT, new TextComponent(title));
                    break;
                case "actionbar":
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(title));
                    break;
            }
        }

        // Afficher le sous-titre selon son mode (avec un délai pour éviter les conflits)
        if (!subtitle.isEmpty()) {
            // Créer une tâche avec un délai pour éviter la superposition
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                switch (subtitleDisplay) {
                    case "title":
                        player.sendTitle("", subtitle, 0, 60, 10);
                        break;
                    case "chat":
                        player.spigot().sendMessage(ChatMessageType.CHAT, new TextComponent(subtitle));
                        break;
                    case "actionbar":
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(subtitle));
                        break;
                }
            }, 2L); // Délai de 2 ticks (0.1 seconde)
        }
    }

    private String stripColorCodes(String text) {
        if (text == null) return "";
        // Enlever les codes couleur Minecraft (§ et &) et les balises de gradient
        return text.replaceAll("§[0-9a-fk-or]", "")
                  .replaceAll("&[0-9a-fk-or]", "")
                  .replaceAll("<[^>]*>", "")
                  .replaceAll("</[^>]*>", "");
    }

    private String getPartialText(String originalText, int targetLength) {
        if (originalText == null || originalText.isEmpty() || targetLength <= 0) {
            return "";
        }
        
        String cleanText = stripColorCodes(originalText);
        if (targetLength >= cleanText.length()) {
            return originalText;
        }
        
        // Construire le texte partiel en préservant les codes couleur
        StringBuilder result = new StringBuilder();
        int visibleChars = 0;
        boolean inColorCode = false;
        boolean inTag = false;
        
        for (int i = 0; i < originalText.length() && visibleChars < targetLength; i++) {
            char c = originalText.charAt(i);
            
            if (c == '§' || c == '&') {
                inColorCode = true;
                result.append(c);
            } else if (inColorCode) {
                result.append(c);
                inColorCode = false;
            } else if (c == '<') {
                inTag = true;
                result.append(c);
            } else if (c == '>' && inTag) {
                inTag = false;
                result.append(c);
            } else if (inTag) {
                result.append(c);
            } else {
                result.append(c);
                visibleChars++;
            }
        }
        
        return result.toString();
    }

    /**
     * Efface tous les types d'affichage pour éviter la superposition
     */
    private void clearAllDisplays() {
        // Effacer le titre/sous-titre
        player.sendTitle("", "", 0, 0, 0);
        
        // Effacer l'actionbar
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
    }

    private void showWithFade() {
        displayMessage(biomeInfo.title, biomeInfo.subtitle);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }
}
