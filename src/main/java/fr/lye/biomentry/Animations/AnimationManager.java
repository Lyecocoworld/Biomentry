package fr.lye.biomentry.Animations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

public class AnimationManager {
    private static final Map<String, TitleAnimation> activeAnimations = new ConcurrentHashMap<>();
    
    public static void startAnimation(TitleAnimation animation, Player player) {
        String playerName = player.getName();
        TitleAnimation currentAnimation = activeAnimations.get(playerName);
        
        // Annuler l'animation en cours si elle existe
        if (currentAnimation != null) {
            currentAnimation.cancel();
            activeAnimations.remove(playerName);
        }
        
        // Démarrer la nouvelle animation
        activeAnimations.put(playerName, animation);
        // Call the animation's start method directly to avoid recursion
        animation.start();
    }
    
    public static void stopAnimation(Player player) {
        String playerName = player.getName();
        TitleAnimation currentAnimation = activeAnimations.get(playerName);
        
        if (currentAnimation != null) {
            currentAnimation.cancel();
            activeAnimations.remove(playerName);
        }
    }
}
