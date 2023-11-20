package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import basemod.ReflectionHacks;
import com.badlogic.gdx.math.MathUtils;
import jdk.internal.vm.annotation.ForceInline;
import sun.misc.Unsafe;
import SpireSurvivors.pickups.PickupPool.PickupType;

// This place is not a place of honor
// No highly esteemed deed is commemorated here
// Nothing managed is here
public final class AbstractPickup {
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");

    // Compression combines 2^COMPRESSION_FACTOR pickups of the same type into one pickup
    public final static int COMPRESSION_FACTOR = 3;

    public final static int SIZE = 0x20;
    private final static int OFFSET_X = 0x0;
    private final static int OFFSET_Y = 0x4;
    private final static int OFFSET_SCALE = 0x8;
    private final static int OFFSET_ROT = 0xC;
    private final static int OFFSET_BOB_TIMER = 0x10;
    private final static int OFFSET_TYPE = 0x14;
    private final static int OFFSET_COMPRESSION = 0x18;
    private final static int OFFSET_FLAGS = 0x1C;
        private final static int FLAG_NO_PULL = 0x1;

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
    public static float drawY(long baseAddress, float bobDistance) {
        return AbstractPickup.y(baseAddress) + MathUtils.sin(AbstractPickup.bobTimer(baseAddress)) * bobDistance;
    }

    @ForceInline
    public static int value(long baseAddress) {
        return 1 << (compression(baseAddress) * COMPRESSION_FACTOR);
    }

    public static void onTouch(long baseAddress) {
        switch (type(baseAddress)) {
            case XP: onTouchXp(baseAddress);
        }
    }

    public static void onTouchXp(long baseAddress) {
        SurvivorDungeon.player.gainXP(1 << (compression(baseAddress) * COMPRESSION_FACTOR));
    }
}
