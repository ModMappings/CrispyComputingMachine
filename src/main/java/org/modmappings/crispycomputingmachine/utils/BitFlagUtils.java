package org.modmappings.crispycomputingmachine.utils;

public final class BitFlagUtils {

    private BitFlagUtils() {
        throw new IllegalStateException("Tried to initialize: BitFlagUtils but this is a Utility class.");
    }

    public static boolean isFlagSet(int value, int flag)
    {
        return (value & flag) == flag;
    }
}
