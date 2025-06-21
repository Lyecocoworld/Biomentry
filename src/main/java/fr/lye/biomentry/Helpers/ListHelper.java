package fr.lye.biomentry.Helpers;

import java.util.List;

public class ListHelper {
    public static boolean Contains(List<String> list, String value) {
        return list.stream().anyMatch(x -> x.equalsIgnoreCase(value));
    }
}
