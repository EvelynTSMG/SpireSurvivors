package SpireSurvivors.pickups;

import SpireSurvivors.pickups.AbstractPickup.PickupType;
import basemod.ReflectionHacks;
import com.badlogic.gdx.math.MathUtils;
import jdk.internal.vm.annotation.ForceInline;
import sun.misc.Unsafe;

/**
 * Defines methods for interacting with a pickup structure in memory.<br>
 * Use {@link PickupPool} when possible.
 * @see AbstractPickup
 */
public final class PickupStruct {
    /**
     * This place is not a place of honor.<br>
     * No highly esteemed deed is commemorated here.<br>
     * Nothing managed is here.
     */
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");
    /**
     * The total size of the pickup struct.<br>
     * Equal to the biggest {@code OFFSET} value + the size of the field at that offset.
     */
    public final static int SIZE = 0x20;

    // We define the offsets of fields in the struct manually
    private final static int OFFSET_X = 0x0;
    private final static int OFFSET_Y = 0x4;
    private final static int OFFSET_SCALE = 0x8;
    private final static int OFFSET_ROT = 0xC;
    private final static int OFFSET_TIMER = 0x10;
    private final static int OFFSET_TYPE = 0x14;
    private final static int OFFSET_COMPRESSION = 0x18;
    private final static int OFFSET_FLAGS = 0x1C;
        public final static int FLAG_NO_PULL = 1;
        public final static int FLAG_NO_BOB = 1 << 1;
        public final static int FLAG_BEING_PULLED = 1 << 2;
        public final static int FLAG_PERSISTENT = 1 << 3;

    /**
     * Allocates memory for a {@code PickupStruct}.<br>
     * Does NOT make sure the {@code PickupStruct} is inactive.
     * @return A pointer to the allocated memory.
     */
    @ForceInline
    public static long alloc() {
        return unsafe.allocateMemory(SIZE);
    }

    /**
     * Allocates memory for an array of {@code PickupStruct}s {@code amount} long.<br>
     * Does NOT make sure the {@code PickupStruct}s are inactive.
     * @param amount The amount of {@code PickupStruct}s to allocate memory for.
     * @return A pointer to the allocated memory.
     */
    @ForceInline
    public static long allocMany(int amount) {
        return unsafe.allocateMemory((long)SIZE * amount);
    }

    /**
     * Frees memory at {@code address}.
     * @param address A pointer to the memory that should be freed.
     */
    @ForceInline
    public static void free(long address) {
        unsafe.freeMemory(address);
    }

    /**
     * Clears all data of a pickup located at {@code address}.
     * @param address A pointer to an instance of a {@code PointerStruct}.
     */
    @ForceInline
    public static void clear(long address) {
        unsafe.setMemory(address, SIZE, (byte)0);
    }

    /**
     * Returns the x coordinate of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The x coordinate of the pickup.
     */
    @ForceInline
    public static float x(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_X);
    }

    /**
     * Sets the x coordinate of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the x coordinate to.
     */
    @ForceInline
    public static void x(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_X, value);
    }

    /**
     * Returns the y coordinate of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The y coordinate of the pickup.
     */
    @ForceInline
    public static float y(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_Y);
    }

    /**
     * Sets the y coordinate of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the y coordinate to.
     */
    @ForceInline
    public static void y(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_Y, value);
    }

    /**
     * Returns the scale of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The scale of the pickup.
     */
    @ForceInline
    public static float scale(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_SCALE);
    }

    /**
     * Sets the scale of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the scale to.
     */
    @ForceInline
    public static void scale(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_SCALE, value);
    }

    /**
     * Returns the rotation of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The rotation of the pickup.
     */
    @ForceInline
    public static float rotation(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_ROT);
    }

    /**
     * Sets the rotation of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the rotation to.
     */
    @ForceInline
    public static void rotation(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_ROT, value);
    }

    /**
     * Returns the timer of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The timer of the pickup.
     */
    @ForceInline
    public static float timer(long baseAddress) {
        return unsafe.getFloat(baseAddress + OFFSET_TIMER);
    }

    /**
     * Sets the timer of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the timer to.
     */
    @ForceInline
    public static void timer(long baseAddress, float value) {
        unsafe.putFloat(baseAddress + OFFSET_TIMER, value);
    }

    /**
     * Returns the type of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The type of the pickup.
     */
    @ForceInline
    public static PickupType type(long baseAddress) {
        return PickupType.deserialize(unsafe.getInt(baseAddress + OFFSET_TYPE));
    }

    /**
     * Sets the type of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The type to set the pickup to.
     */
    @ForceInline
    public static void type(long baseAddress, PickupType value) {
        unsafe.putInt(baseAddress + OFFSET_TYPE, PickupType.serialize(value));
    }

    /**
     * Returns whether the pickup located at {@code baseAddress} is active.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup is active.
     */
    @ForceInline
    public static boolean active(long baseAddress) {
        return unsafe.getInt(baseAddress + OFFSET_TYPE) != 0;
    }

    /**
     * Deactivates the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     */
    @ForceInline
    public static void deactivate(long baseAddress) {
        unsafe.putInt(baseAddress + OFFSET_TYPE, 0);
    }

    /**
     * Returns the compression of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The compression of the pickup.
     */
    @ForceInline
    public static int compression(long baseAddress) {
        return unsafe.getInt(baseAddress + OFFSET_COMPRESSION);
    }

    /**
     * Sets the compressions of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the compression to.
     */
    @ForceInline
    public static void compression(long baseAddress, int value) {
        unsafe.putInt(baseAddress + OFFSET_COMPRESSION, value);
    }

    /**
     * Returns the flags of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The flags of the pickup.
     * @see PickupStruct#noPull(long) noPull()
     * @see PickupStruct#noBob(long) noBob()
     * @see PickupStruct#persistent(long) persistent()
     */
    @ForceInline
    public static int flags(long baseAddress) {
        return unsafe.getInt(baseAddress + OFFSET_FLAGS);
    }

    /**
     * Sets the flags of the pickup located at {@code baseAddress} to {@code value}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value The value to set the flags to.
     * @see PickupStruct#noPull(long, boolean) noPull()
     * @see PickupStruct#noBob(long, boolean) noBob()
     * @see PickupStruct#persistent(long, boolean) persistent()
     */
    @ForceInline
    public static void flags(long baseAddress, int value) {
        unsafe.putInt(baseAddress + OFFSET_FLAGS, value);
    }

    /**
     * Returns whether the pickup located at {@code baseAddress} is able to be pulled by the player.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup is pullable.
     */
    @ForceInline
    public static boolean noPull(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_NO_PULL) != 0;
    }

    /**
     * Sets whether the pickup located at {@code baseAddress} is able to be pulled by the player.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value Whether the pickup should be pullable.
     */
    @ForceInline
    public static void noPull(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_NO_PULL : x & ~FLAG_NO_PULL;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    /**
     * Returns whether the pickup located at {@code baseAddress} should bob up and down.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup is supposed to bob.
     */
    @ForceInline
    public static boolean noBob(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_NO_BOB) != 0;
    }

    /**
     * Sets whether the pickup located at {@code baseAddress} should bob up and down.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value Whether the pickup should bob.
     */
    @ForceInline
    public static void noBob(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_NO_BOB : x & ~FLAG_NO_BOB;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    /**
     * Returns whether the pickup located at {@code baseAddress} is being pulled.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup is being pulled.
     */
    @ForceInline
    public static boolean beingPulled(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_BEING_PULLED) != 0;
    }

    /**
     * Sets whether the pickup located at {@code baseAddress} is being pulled.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param value Whether the pickup is being pulled.
     */
    @ForceInline
    public static void beingPulled(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_BEING_PULLED : x & ~FLAG_BEING_PULLED;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    /**
     * Returns whether the pickup located at {@code baseAddress} persists after being touched.<br>
     * If {@code true}, the pickup won't be collected.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup is persistent.
     * @see PickupBehavior#onTouch(long) PickupBehavior.onTouch()
     * @see PickupBehavior#canCollect(long) PickupBehavior.canCollect()
     * @see PickupBehavior#onCollect(long) PickupBehavior.onCollect()
     */
    @ForceInline
    public static boolean persistent(long baseAddress) {
        return (unsafe.getInt(baseAddress + OFFSET_FLAGS) & FLAG_PERSISTENT) != 0;
    }

    /**
     * Sets whether the pickup located at {@code baseAddress} persists after being touched.<br>
     * If {@code true}, the pickup won't be collected.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return Whether the pickup should be persistent.
     * @see PickupBehavior#onTouch(long) PickupBehavior.onTouch()
     * @see PickupBehavior#canCollect(long) PickupBehavior.canCollect()
     * @see PickupBehavior#onCollect(long) PickupBehavior.onCollect()
     */
    @ForceInline
    public static void persistent(long baseAddress, boolean value) {
        int x = unsafe.getInt(baseAddress + OFFSET_FLAGS);
        x = value ? x | FLAG_PERSISTENT : x & ~FLAG_PERSISTENT;
        unsafe.putInt(baseAddress + OFFSET_FLAGS, x);
    }

    /**
     * Returns the y coordinate the pickup located at {@code baseAddress} should be drawn at.<br>
     * A higher {@code bobDistance} will result in a bigger maximum difference between the y coordinate the pickup
     * should be drawn at and the actual y coordinate of the pickup.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @param bobDistance How much the pickup should bob up and down.
     * @return The y coordinate the pickup should be drawn at.
     */
    @ForceInline
    public static float drawY(long baseAddress, float bobDistance) {
        return PickupStruct.y(baseAddress) + MathUtils.sin(PickupStruct.timer(baseAddress)) * bobDistance;
    }

    /**
     * Returns the value of the pickup located at {@code baseAddress}.
     * @param baseAddress A pointer to an instance of {@code PickupStruct}.
     * @return The value of the pickup.
     */
    @ForceInline
    public static int value(long baseAddress) {
        return 1 << (compression(baseAddress) * AbstractPickup.COMPRESSION_FACTOR);
    }
}
