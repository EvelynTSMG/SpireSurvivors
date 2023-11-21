package SpireSurvivors.pickups;

import basemod.ReflectionHacks;
import com.badlogic.gdx.math.MathUtils;
import jdk.internal.vm.annotation.ForceInline;
import sun.misc.Unsafe;
import SpireSurvivors.pickups.AbstractPickup.PickupType;

// This place is not a place of honor
// No highly esteemed deed is commemorated here
// Nothing managed is here

/**
 * Defines methods for interacting with a pickup structure in memory.<br>
 * Use {@link PickupPool} when possible.
 * @see AbstractPickup
 */
public final class PickupStruct {
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");

    public final static int SIZE = 0x20;
    private final static int OFFSET_X = 0x0;
    private final static int OFFSET_Y = 0x4;
    private final static int OFFSET_SCALE = 0x8;
    private final static int OFFSET_ROT = 0xC;
    private final static int OFFSET_BOB_TIMER = 0x10;
    private final static int OFFSET_TYPE = 0x14;
    private final static int OFFSET_COMPRESSION = 0x18;
    private final static int OFFSET_FLAGS = 0x1C;
        public final static int FLAG_NO_PULL = 1;
        public final static int FLAG_NO_COMPRESSION = 1 << 1;
        public final static int FLAG_NO_BOB = 1 << 2;
        public final static int FLAG_PERSISTENT = 1 << 3;
        public final static int FLAG_ALL = 0xFFFFFFFF;

    @ForceInline
    public static long alloc() {
        return unsafe.allocateMemory(SIZE);
    }

    @ForceInline
    public static void free(long address) {
        unsafe.freeMemory(address);
    }

    @ForceInline
    public static void clear(long address) {
        unsafe.setMemory(address, SIZE, (byte)0);
    }

    @ForceInline
    public static float x(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_X);
    }

    @ForceInline
    public static void x(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_X, value);
    }

    @ForceInline
    public static float y(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_Y);
    }

    @ForceInline
    public static void y(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_Y, value);
    }

    @ForceInline
    public static float scale(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_SCALE);
    }

    @ForceInline
    public static void scale(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_SCALE, value);
    }

    @ForceInline
    public static float rotation(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_ROT);
    }

    @ForceInline
    public static void rotation(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_ROT, value);
    }

    @ForceInline
    public static float bobTimer(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_BOB_TIMER);
    }

    @ForceInline
    public static void bobTimer(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_BOB_TIMER, value);
    }

    @ForceInline
    public static PickupType type(long baseAddress) {
        return PickupType.deserialize(unsafe.getInt(baseAddress + OFFSET_TYPE));
    }

    @ForceInline
    public static void type(long baseAddress, PickupType value) {
        unsafe.putInt(baseAddress + OFFSET_TYPE, PickupType.serialize(value));
    }

    @ForceInline
    public static int compression(long baseAddress) {
        return unsafe.getInt(baseAddress + OFFSET_COMPRESSION);
    }

    @ForceInline
    public static void compression(long baseAddress, int value) {
        unsafe.putInt(baseAddress + OFFSET_COMPRESSION, value);
    }

    @ForceInline
    public static int flags(long baseAddress) {
        return unsafe.getInt(baseAddress + OFFSET_FLAGS);
    }

    @ForceInline
    public static void flags(long baseAddress, int value) {
        unsafe.putInt(baseAddress + OFFSET_FLAGS, value);
    }

    @ForceInline
    public static boolean noPull(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_NO_PULL) != 0;
    }

    @ForceInline
    public static void noPull(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_NO_PULL : x & ~FLAG_NO_PULL;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    @ForceInline
    public static boolean noCompression(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_NO_COMPRESSION) != 0;
    }

    @ForceInline
    public static void noCompression(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_NO_COMPRESSION : x & ~FLAG_NO_COMPRESSION;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    @ForceInline
    public static boolean noBob(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_NO_BOB) != 0;
    }

    @ForceInline
    public static void noBob(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_NO_BOB : x & ~FLAG_NO_BOB;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    @ForceInline
    public static boolean persistent(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_PERSISTENT) != 0;
    }

    @ForceInline
    public static void persistent(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_PERSISTENT : x & ~FLAG_PERSISTENT;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    @ForceInline
    public static float drawY(long baseAddress, float bobDistance) {
        return PickupStruct.y(baseAddress) + MathUtils.sin(PickupStruct.bobTimer(baseAddress)) * bobDistance;
    }

    @ForceInline
    public static int value(long baseAddress) {
        return 1 << (compression(baseAddress) * AbstractPickup.COMPRESSION_FACTOR);
    }

    @ForceInline
    public static int value(int compression) {
        return 1 << (compression * AbstractPickup.COMPRESSION_FACTOR);
    }
}
