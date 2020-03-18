package org.modmappings.crispycomputingmachine.utils;

import java.util.Locale;

public final class GameVersionUtils {

    private GameVersionUtils() {
        throw new IllegalStateException("Tried to initialize: GameVersionUtils but this is a Utility class.");
    }

    public static boolean isPreRelease(String version)
    {
        String lower = version.toLowerCase(Locale.ENGLISH);

        if ("15w14a".equals(lower)) { //2015 April Fools
            return false;
        } else if ("1.rv-pre1".equals(lower)) { //2016 April Fools
            return false;
        } else if ("3d shareware v1.34".equals(lower)) { //2019 April Fools
            return false;
        } else if (lower.charAt(0) == 'b' || lower.charAt(0) == 'a') {
            return false;
        } else if (lower.length() == 6 && lower.charAt(2) == 'w') {
            return false;
        } else {
            if (lower.contains("-pre")) {
                return true;
            } else if (lower.contains("_Pre-Release_".toLowerCase())) {
                return true;
            } else return lower.contains(" Pre-Release ".toLowerCase());
        }
    }

    public static boolean isSnapshot(String version)
    {
        String lower = version.toLowerCase(Locale.ENGLISH);
        switch (lower) {
            case "15w14a":  //2015 April Fools
                return true;
            case "1.rv-pre1":  //2016 April Fools
                return true;
            case "3d shareware v1.34":  //2019 April Fools
                return true;
            default:
                return lower.length() == 6 && lower.charAt(2) == 'w';
        }
    }
}
