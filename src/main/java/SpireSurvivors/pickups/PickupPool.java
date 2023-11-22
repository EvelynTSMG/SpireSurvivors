package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import SpireSurvivors.pickups.AbstractPickup.PickupType;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static SpireSurvivors.dungeon.SurvivorDungeon.pickupPools;

/**
 * Defines and manages a pool of pickups
 * @see PickupStruct
 * @see AbstractPickup
 */
public class PickupPool {
    /**
     * The amount of pickups the pool can store.
     */
    public final static int POOL_SIZE = 1024;
    /**
     * A pointer to the start of the pool
     */
    private final long poolAddress;
    /**
     * The amount of active pickups currently in the pool
     */
    private int usedPickups = 0;

    public PickupPool() {
        poolAddress = PickupStruct.allocMany(POOL_SIZE);

        // We have to set everything to Inactive so we don't accidentally clog
        long address = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, address += PickupStruct.SIZE) {
            PickupStruct.deactivate(address);
        }
    }

    // Deprecated in Java 9+, we're in Java 8
    // Also there isn't a better way (that's not to say this way is good)
    @Override
    @SuppressWarnings("removal")
    protected void finalize() {
        // Gotta make sure we don't leak memory!
        PickupStruct.free(poolAddress);
    }

    /*===== Basic Functionality =====*/

    /**
     * @return Whether the pool is full
     */
    public boolean isFull() {
        return usedPickups >= POOL_SIZE;
    }

    /**
     * Spawn a pickup of {@code type} at ({@code x}, {@code y}) with the given {@code compression}.<br>
     * If {@code mayCompress} is {@code true}, it may attempt to compress the spawned pickup using {@link PickupPool#tryCompress(float, float, PickupType, int) tryCompress()}.
     * @param x The x coordinate where the pickup should spawn.
     * @param y The y coordinate where the pickup should spawn.
     * @param type The type of the pickup to spawn.
     * @param compression The compression level of the pickup to spawn.
     * @param mayCompress Whether the pool is allowed to make a compression attempt.<br>
     *                    Respects {@link PickupType#compressable type.compressable} regardless.
     * @return The address of the spawned pickup.
     */
    public static long spawn(float x, float y, PickupType type, int compression, boolean mayCompress) {
        for (PickupPool pool : pickupPools) {
            if (!pool.isFull()) {
                return pool.spawnLocal(x, y, type, compression, mayCompress);
            }
        }

        // All pools must be full, so we add one more
        pickupPools.add(new PickupPool());
        return pickupPools.get(pickupPools.size() - 1).spawnLocal(x, y, type, compression, mayCompress);
    }

    /**
     * Spawn a pickup of {@code type} at a random position near ({@code x}, {@code y}) with the given {@code compression}.<br>
     * If {@code mayCompress} is {@code true}, it may attempt to compress the spawned pickup using {@link PickupPool#tryCompress(float, float, PickupType, int) tryCompress()}.
     * @param x The x coordinate where the pickup should spawn.
     * @param y The y coordinate where the pickup should spawn.
     * @param type The type of the pickup to spawn.
     * @param compression The compression level of the pickup to spawn.
     * @param mayCompress Whether the pool is allowed to make a compression attempt.<br>
     *                    Respects {@link PickupType#compressable type.compressable} regardless.
     * @return The address of the spawned pickup.
     */
    public static long spawnScattered(float x, float y, PickupType type, int compression, boolean mayCompress) {
        x += MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);
        y += MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);

        return spawn(x, y, type, compression, mayCompress);
    }

    /**
     * Spawn a pickup of {@code type} at ({@code x}, {@code y}) with the given {@code compression} in this pool.<br>
     * If {@code mayCompress} is {@code true}, it may attempt to compress the spawned pickup using {@link PickupPool#tryCompress(float, float, PickupType, int) tryCompress()}.
     * @param x The x coordinate where the pickup should spawn.
     * @param y The y coordinate where the pickup should spawn.
     * @param type The type of the pickup to spawn.
     * @param compression The compression level of the pickup to spawn.
     * @param mayCompress Whether the pool is allowed to make a compression attempt.<br>
     *                    Respects {@link PickupType#compressable type.compressable} regardless.
     * @return The address of the spawned pickup.
     */
    public long spawnLocal(float x, float y, PickupType type, int compression, boolean mayCompress) {
        if (type.compressable && mayCompress) compression = tryCompress(x, y, type, compression);

        long address = getInactive();
        PickupStruct.clear(address);

        PickupStruct.type(address, type);
        usedPickups += 1;

        PickupStruct.x(address, x);
        PickupStruct.y(address, y);

        PickupStruct.scale(address, 1f);
        PickupStruct.bobTimer(address, MathUtils.random(0, (float)Math.PI * 2));

        PickupStruct.compression(address, compression);
        PickupStruct.flags(address, type.flags);
        return address;
    }

    /**
     * Removes a pickup from the pool.
     * @param address A pointer to the {@link PickupStruct} that should be removed.
     */
    public void remove(long address) {
        PickupStruct.deactivate(address);
        usedPickups -= 1;
    }

    /**
     * Returns a pointer to the first inactive {@link PickupStruct} in this pool.
     * @return A pointer to a {@link PickupStruct}.
     */
    private long getInactive() {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += PickupStruct.SIZE) {
            if (!PickupStruct.active(baseAddress)) {
                return baseAddress;
            }
        }

        // Unreachable
        throw new OutOfMemoryError("Pickup Pool out of inactive slots");
    }


    /*===== Compression =====*/

    public int tryCompress(float x, float y, PickupType type, int compression) {
        final int BUCKET_COUNT = 32;

        @SuppressWarnings("unchecked")
        ArrayList<Long>[] buckets_pickups = new ArrayList[BUCKET_COUNT];
        int[] buckets_count = new int[BUCKET_COUNT];
        for (int i = 0; i < BUCKET_COUNT; i++) {
            if (i == compression * AbstractPickup.COMPRESSION_FACTOR) buckets_count[i] += 1;
            buckets_pickups[i] = new ArrayList<>(32);
        }

        runForNearby(x, y, AbstractPickup.COMPRESSION_RANGE, address -> {
            if (PickupStruct.type(address) == type) {
                buckets_count[PickupStruct.compression(address) * AbstractPickup.COMPRESSION_FACTOR] += 1;
                buckets_pickups[PickupStruct.compression(address) * AbstractPickup.COMPRESSION_FACTOR].add(address);
            }
        });

        int goal = (compression + 1) * AbstractPickup.COMPRESSION_FACTOR;
        int goal_n = buckets_count[goal] + 1;
        int last_goal_reached = 0; // may not be needed?
        int last_goal_n = 0; // may not be needed?

        if (!canAchieveNextCompression(buckets_count, compression * AbstractPickup.COMPRESSION_FACTOR, 0)) {
            // We can't do anything
            return compression;
        }

        for (int i = 0; i < BUCKET_COUNT; i++) {
            if (i == goal) {
                if (buckets_count[i] >= goal_n) {
                    last_goal_reached = goal;
                    last_goal_n = goal_n;

                    if (!canAchieveNextCompression(buckets_count, goal, i)) break;

                    goal += AbstractPickup.COMPRESSION_FACTOR;
                    goal_n = buckets_count[goal] + 1;
                } else break;
            }
            if (i == BUCKET_COUNT - 1) break;

            int count = buckets_count[i];
            ArrayList<Long> list = buckets_pickups[i];
            ArrayList<Long> list_next = buckets_pickups[i + 1];

            if (count % 2 == 1) {
                list_next.addAll(list.subList(1, list.size()));

                long first = list.get(0);
                list.clear();
                list.add(first);
            } else {
                list_next.addAll(list);
                list.clear();
            }

            buckets_count[i + 1] += count/2;
            buckets_count[i] %= 2;
        }

        for (long address : buckets_pickups[last_goal_reached]) {
            if (PickupStruct.compression(address) != last_goal_reached) {
                remove(address);
            }
        }
        compression = last_goal_reached / AbstractPickup.COMPRESSION_FACTOR;

        // Spawn collateral
        int collateral = buckets_count[last_goal_reached] - last_goal_n;
        for (int i = 0; i < collateral; i++) {
            float extraX = x + MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);
            float extraY = y + MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);

            // During compression we'll always remove more pickups than we'll spawn extras
            // So we can use the pool's local spawn method
            spawnLocal(extraX, extraY, type, compression, false);
        }

        return compression;
    }

    private boolean canAchieveNextCompression(int[] buckets_count, int goal, int i) {
        // Max compression achieved
        int next_goal = goal + AbstractPickup.COMPRESSION_FACTOR;
        if (next_goal >= buckets_count.length) return false;

        // There's not enough to compress further
        int next_goal_n = buckets_count[next_goal] + 1;
        int sum = 0;
        for (int j = i; j < next_goal; j++) {
            sum /= 2;
            sum += buckets_count[j];
        }
        return sum >= next_goal_n;
    }


    /*====== ITERATION ======*/

    /**
     * Calls {@code action} for all pickups within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @param action The action to perform.<br>
     *               Takes a pointer to a {@link PickupStruct}.
     * @see PickupPool#runForNearbyLocal(float, float, float, Consumer) runForNearbyLocal()
     */
    public static void runForNearby(float x, float y, float r, Consumer<Long> action) {
        pickupPools.forEach(
            pool -> pool.runForNearbyLocal(x, y, r, action)
        );
    }

    /**
     * Returns a list of pointers to all pickups within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @return ArrayList&lt;{@link PickupStruct}*&gt;.
     * @see PickupPool#nearbyLocal(float, float, float) nearby()
     */
    public static ArrayList<Long> nearby(float x, float y, float r) {
        return nearby(x, y, r, __ -> true);
    }

    /**
     * Returns a filtered list of pointers to all pickups within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @param filter The filter that determines whether a given pickup will be added to a list.<br>
     *               Takes in a pointer to a {@link PickupStruct}.
     * @return ArrayList&lt;{@link PickupStruct}*&gt;.
     * @see PickupPool#nearbyLocal(float, float, float, Predicate) nearbyLocal()
     */
    public static ArrayList<Long> nearby(float x, float y, float r, Predicate<Long> filter) {
        ArrayList<Long> pickups = new ArrayList<>();
        pickupPools.forEach(
            pool -> pickups.addAll(pool.nearbyLocal(x, y, r, filter))
        );
        return pickups;
    }

    /**
     * Calls {@code action} for all pickups.
     * @param action The action to perform.<br>
     *               Takes a pointer to a {@link PickupStruct}
     * @see PickupPool#forEachLocal(Consumer) forEachLocal()
     */
    public static void forEach(Consumer<Long> action) {
        pickupPools.forEach(
            pool -> pool.forEachLocal(action)
        );
    }

    /**
     * Calls {@code action} for pickups in this pool within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @param action The action to perform.<br>
     *               Takes a pointer to a {@link PickupStruct}.
     * @see PickupPool#runForNearby(float, float, float, Consumer) runForNearby()
     */
    public void runForNearbyLocal(float x, float y, float r, Consumer<Long> action) {
        forEachLocal(address -> {
            float dx = PickupStruct.x(address) - x;
            float dy = PickupStruct.y(address) - y;
            if (dx*dx + dy*dy <= r*r) {
                action.accept(address);
            }
        });
    }

    /**
     * Returns a list of pointers to pickups in this pool within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @return ArrayList&lt;{@link PickupStruct}*&gt;.
     * @see PickupPool#nearby(float, float, float) nearby()
     */
    public ArrayList<Long> nearbyLocal(float x, float y, float r) {
        // Negligibly suboptimal
        return nearbyLocal(x, y, r, __ -> true);
    }

    /**
     * Returns a filtered list of pointers to pickups in this pool within a circle with radius {@code r} at position ({@code x}, {@code y}).
     * @param x The x coordinate of the center of the circle.
     * @param y The y coordinate of the center of the circle.
     * @param r The radius of the circle.
     * @param filter The filter that determines whether a given pickup will be added to a list.<br>
     *               Takes in a pointer to a {@link PickupStruct}.
     * @return ArrayList&lt;{@link PickupStruct}*&gt;.
     * @see PickupPool#nearby(float, float, float, Predicate) nearby()
     */
    public ArrayList<Long> nearbyLocal(float x, float y, float r, Predicate<Long> filter) {
        ArrayList<Long> pickups = new ArrayList<>();
        forEachLocal(address -> {
            float dx = PickupStruct.x(address) - x;
            float dy = PickupStruct.y(address) - y;
            if (dx*dx + dy*dy <= r*r && filter.test(address)) {
                pickups.add(address);
            }
        });
        return pickups;
    }

    /**
     * Calls {@code action} for pickups in this pool.
     * @param action The action to perform.<br>
     *               Takes a pointer to a {@link PickupStruct}
     * @see PickupPool#forEach(Consumer) forEach()
     */
    public void forEachLocal(Consumer<Long> action) {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += PickupStruct.SIZE) {
            if (PickupStruct.active(baseAddress)) {
                action.accept(baseAddress);
            }
        }
    }


    /*===== POOL HANDLING =====*/

    /**
     * Updates all pickups. Should only be called once per frame.
     * @see PickupPool#updateLocal()
     */
    public static void update() {
        pickupPools.forEach(PickupPool::updateLocal);
    }

    /**
     * Renders all pickups onto {@code sb}.
     * @param sb The {@link SpriteBatch} to draw onto.
     */
    public static void render(SpriteBatch sb) {
        pickupPools.forEach(
            pool -> pool.renderLocal(sb)
        );
    }

    /**
     * Moves all pickups by the vector [{@code x}, {@code y}].
     * @param x The x coordinate to move by.
     * @param y The y coordinate to move by.
     */
    public static void move(float x, float y) {
        pickupPools.forEach(
            pool -> pool.moveLocal(x, y)
        );
    }

    /**
     * Updates pickups in this pool. Should only be called once per frame.
     * @see PickupPool#update()
     */
    public void updateLocal() {
        // Store some variables for all pickups instead of recalculating or re-accessing them every time
        float playerX = SurvivorDungeon.player.basePlayer.hb.cX;
        float playerY = SurvivorDungeon.player.basePlayer.hb.cY;

        double pullRange = Math.pow(SurvivorDungeon.player.pickupRangeMultiplier * AbstractSurvivorPlayer.PICKUP_PULL_RANGE, 2);
        double collectRange = Math.pow(AbstractSurvivorPlayer.PICKUP_COLLECT_RANGE, 2);

        forEachLocal(address -> {
            float dx = PickupStruct.x(address) - playerX;
            float dy = PickupStruct.y(address) - playerY;

            if (!PickupStruct.type(address).update(address)) return;

            // Try to collect pickup
            if (dx*dx + dy*dy <= collectRange) {
                PickupStruct.type(address).onTouch(address);
                if (PickupStruct.type(address).canCollect(address)) {
                    PickupStruct.type(address).onCollect(address);
                    remove(address);
                    return;
                }
            }

            // Bobby Pickups
            if (!PickupStruct.noBob(address)) {
                float bobTimerDelta = Gdx.graphics.getDeltaTime() * PickupStruct.type(address).bobSpeed;
                PickupStruct.bobTimer(address, PickupStruct.bobTimer(address) + bobTimerDelta);
                if (PickupStruct.bobTimer(address) > Math.PI * 2) {
                    // Preemptively avoiding reaching limits in long runs
                    PickupStruct.bobTimer(address, PickupStruct.bobTimer(address) - (float)Math.PI * 2);
                }

            }

            // Try to pull towards player
            if (!PickupStruct.noPull(address)) {
                if (dx*dx + dy*dy <= pullRange) {
                    PickupStruct.type(address).pull(address);
                }
            }
        });
    }

    /**
     * Renders pickups in this pool onto {@code sb}.
     * @param sb The {@link SpriteBatch} to draw onto.
     */
    public void renderLocal(SpriteBatch sb) {
        forEachLocal(address -> {
            PickupType type = PickupStruct.type(address);
            if (type.image != null) {
                Texture t = type.image.getTexture();
                sb.draw(new TextureRegion(type.image), PickupStruct.x(address), PickupStruct.drawY(address, type.bobDistance),
                        t.getWidth()/2f, t.getHeight()/2f,
                        t.getWidth(), t.getHeight(),
                        PickupStruct.scale(address) * Settings.scale, PickupStruct.scale(address) * Settings.scale,
                        PickupStruct.rotation(address));
            }
        });
    }

    /**
     * Moves pickups in this pool by the vector [{@code x}, {@code y}].
     * @param x The x coordinate to move by.
     * @param y The y coordinate to move by.
     */
    public void moveLocal(float x, float y) {
        forEachLocal(address -> {
            PickupStruct.x(address, PickupStruct.x(address) + x);
            PickupStruct.y(address, PickupStruct.y(address) + y);
        });
    }
}
