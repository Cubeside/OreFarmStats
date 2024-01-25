package de.cubeside.orefarmstats.util;

import org.bukkit.Location;
import org.bukkit.util.BlockVector;

/**
 * In bytes: xxxzzzyy
 */
public final class CompressedLocation {
    public static final int THREE_BYTE_INT_BITS = 24;
    public static final int THREE_BYTE_INT_MASK = 0xffffff;
    public static final int THREE_BYTE_INT_MIN_VALUE = 0xff800000;
    public static final int THREE_BYTE_INT_MAX_VALUE = 0x7fffff;
    public static final int TWO_BYTE_INT_BITS = 16;
    public static final int TWO_BYTE_INT_MASK = 0xffff;
    public static final int TWO_BYTE_INT_MIN_VALUE = 0xffff8000;
    public static final int TWO_BYTE_INT_MAX_VALUE = 0x7fff;

    private CompressedLocation() {
    }

    public static long fromLocation(Location l) {
        return fromLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public static long fromLocation(int x, int y, int z) {
        if (x < THREE_BYTE_INT_MIN_VALUE || x > THREE_BYTE_INT_MAX_VALUE) {
            throw new IllegalArgumentException("x");
        }
        if (z < THREE_BYTE_INT_MIN_VALUE || z > THREE_BYTE_INT_MAX_VALUE) {
            throw new IllegalArgumentException("z");
        }
        if (y < TWO_BYTE_INT_MIN_VALUE || y > TWO_BYTE_INT_MAX_VALUE) {
            throw new IllegalArgumentException("y");
        }
        return (((long) x & THREE_BYTE_INT_MASK) << (THREE_BYTE_INT_BITS + TWO_BYTE_INT_BITS)) | (((long) z & THREE_BYTE_INT_MASK) << TWO_BYTE_INT_BITS) | ((long) y & TWO_BYTE_INT_MASK);
    }

    public static BlockVector toBlockVector(long compressedLocation) {
        int x = ((int) (compressedLocation >> Integer.SIZE)) >> (Integer.SIZE - THREE_BYTE_INT_BITS);
        int z = ((int) (compressedLocation >> Integer.SIZE - THREE_BYTE_INT_BITS)) >> (Integer.SIZE - THREE_BYTE_INT_BITS);
        int y = ((int) (compressedLocation << (Integer.SIZE - TWO_BYTE_INT_BITS))) >> (Integer.SIZE - TWO_BYTE_INT_BITS);

        return new BlockVector(x, y, z);
    }
}
