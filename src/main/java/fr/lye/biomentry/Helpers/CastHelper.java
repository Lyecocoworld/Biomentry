package fr.lye.biomentry.Helpers;

public class CastHelper {
    public static boolean TryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
