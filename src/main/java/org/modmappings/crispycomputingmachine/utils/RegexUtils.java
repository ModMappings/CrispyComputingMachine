package org.modmappings.crispycomputingmachine.utils;

public final class RegexUtils {

    private RegexUtils() {
        throw new IllegalStateException("Tried to initialize: RegexUtils but this is a Utility class.");
    }

    public static String createFullWordRegex(String regex)
    {
        return "\\A" + regex + "\\Z";
    }

    public static String createClassTargetingRegex(String className)
    {
        return createFullWordRegex(className.replace("$", "\\$"));
    }

    public static String createOuterClassTargetingRegex(String className)
    {
        return createClassTargetingRegex(className.substring(0, className.lastIndexOf("$")));
    }
}
