package fr.lye.biomentry.Helpers;

// Cette classe est maintenant obsolète car remplacée par TitleAnimation
// Elle est conservée uniquement pour la compatibilité avec d'anciennes versions
@Deprecated
public class AnimationHelper {
    @Deprecated
    public static String applyTypewriter(String text, int speed, int currentTick) {
        int visibleChars = speed * currentTick;
        if (visibleChars >= text.length()) {
            return text;
        }
        return text.substring(0, visibleChars);
    }
}
