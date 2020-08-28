package org.modmappings.crispycomputingmachine.utils;

public final class NameUtils {

    private NameUtils() {
        throw new IllegalStateException("Tried to initialize: NameUtils but this is a Utility class.");
    }

    public static String getActualClassName(final String fullName)
    {
        final String[] packageSplit = fullName.split("/");
        final String className = packageSplit[packageSplit.length - 1];
        final String[] innerClassSplit = className.split("\\$");
        return innerClassSplit[innerClassSplit.length-1];
    }
}
