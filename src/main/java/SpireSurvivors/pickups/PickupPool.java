package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import SpireSurvivors.pickups.AbstractPickup.PickupType;
import basemod.Pair;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Defines and manages a pool of pickups
 * @see PickupStruct
 * @see AbstractPickup
 */
public class PickupPool {
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");
    public final static int POOL_SIZE = 1024;
    private final long poolAddress;
    private int usedPickups = 0;

    public PickupPool() {
        poolAddress = unsafe.allocateMemory(POOL_SIZE * PickupStruct.SIZE);

        // We have to set everything to Inactive so we don't accidentally clog
        long address = poolAddress + (int)ReflectionHacks.getPrivateStatic(PickupStruct.class, "OFFSET_TYPE");
        for (int i = 0; i < POOL_SIZE; i++, address += PickupStruct.SIZE) {
            // Serializing kinda slow, so we do it manually :3
            unsafe.putInt(address, 0);
        }
    }

    public boolean isFull() {
        return usedPickups >= POOL_SIZE;
    }

    public long spawn(float x, float y, PickupType type, int compression, boolean shouldCompress) {
        if (shouldCompress) compression = tryCompress(x, y, type, compression);

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

    public void remove(long address) {
        PickupStruct.type(address, PickupType.INACTIVE);
        usedPickups -= 1;
    }

    public int tryCompress(float x, float y, PickupType type, int compression) {
        // How to optimize:
        // use literally anything that doesn't require getter methods and constantly creating new instances
        // instead of these Pair<Integer, ArrayList<Long>> objects

        @SuppressWarnings("unchecked")
        Pair<Integer, ArrayList<Long>>[] buckets = new Pair[32];
        for (int i = 0; i < buckets.length; i++) {
            if (i == compression * AbstractPickup.COMPRESSION_FACTOR) buckets[i] = new Pair<>(1, new ArrayList<>());
            buckets[i] = new Pair<>(0, new ArrayList<>());
        }

        runForNearby(x, y, AbstractPickup.COMPRESSION_RANGE, address -> {
            if (PickupStruct.type(address) == type) {
                Pair<Integer, ArrayList<Long>> p = buckets[PickupStruct.compression(address) * AbstractPickup.COMPRESSION_FACTOR];
                p.getValue().add(address);
                buckets[PickupStruct.compression(address) * AbstractPickup.COMPRESSION_FACTOR] = new Pair<>(p.getKey() + 1, p.getValue());
            }
        });

        int goal = (compression + 1) * AbstractPickup.COMPRESSION_FACTOR;
        int goal_n = buckets[goal].getKey() + 1;
        int last_goal_reached = 0; // may not be needed?
        int last_goal_n = 0; // may not be needed?
        for (int i = 0; i < buckets.length; i++) {
            if (i == goal) {
                if (buckets[i].getKey() >= goal_n) {
                    last_goal_reached = goal;
                    last_goal_n = goal_n;

                    if (!canAchieveNextCompression(buckets, goal, i)) break;

                    goal += AbstractPickup.COMPRESSION_FACTOR;
                    goal_n = buckets[goal].getKey() + 1;
                } else break;
            }
            if (i == buckets.length - 1) break;

            int count = buckets[i].getKey();
            ArrayList<Long> list = buckets[i].getValue();
            ArrayList<Long> list_next = buckets[i + 1].getValue();

            if (count % 2 == 1) {
                list_next.addAll(list.subList(1, list.size()));

                long first = 0;
                list.clear();
                list.add(first);
            } else {
                list_next.addAll(list);
                list.clear();
            }

            buckets[i] = new Pair<>(count % 2, list);
            buckets[i + 1] = new Pair<>(buckets[i + 1].getKey() + (count/2), list_next);
        }

        int extras = buckets[last_goal_reached].getKey() - last_goal_n;

        for (long address : buckets[last_goal_reached].getValue()) {
            if (PickupStruct.compression(address) != last_goal_reached) {
                remove(address);
            }
        }
        compression = last_goal_reached / AbstractPickup.COMPRESSION_FACTOR;

        for (int i = 0; i < extras; i++) {
            float extraX = x + MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);
            float extraY = y + MathUtils.random(-AbstractPickup.SCATTER_RANGE, AbstractPickup.SCATTER_RANGE);
            spawn(extraX, extraY, type, compression, false);
        }

        return compression;
    }

    private boolean canAchieveNextCompression(Pair<Integer, ArrayList<Long>>[] buckets, int goal, int i) {
        // Max compression achieved
        int next_goal = goal + AbstractPickup.COMPRESSION_FACTOR;
        if (next_goal >= buckets.length) return false;

        // There's not enough to compress further
        int next_goal_n = buckets[next_goal].getKey() + 1;
        int sum = 0;
        for (int j = 0; j < AbstractPickup.COMPRESSION_FACTOR; j++) {
            sum /= 2;
            sum += buckets[i + j].getKey();
        }
        return sum >= next_goal_n;
    }

    public void runForNearby(float x, float y, float r, Consumer<Long> action) {
        forEach(address -> {
            float dx = PickupStruct.x(address) - x;
            float dy = PickupStruct.y(address) - y;
            if (dx*dx + dy*dy <= r*r) {
                action.accept(address);
            }
        });
    }

    public ArrayList<Long> nearby(float x, float y, float r) {
        ArrayList<Long> pickups = new ArrayList<>();
        forEach(address -> {
            float dx = PickupStruct.x(address) - x;
            float dy = PickupStruct.y(address) - y;
            if (dx*dx + dy*dy <= r*r) {
                pickups.add(address);
            }
        });
        return pickups;
    }

    public ArrayList<Long> nearby(float x, float y, float r, Predicate<Long> filter) {
        ArrayList<Long> pickups = new ArrayList<>();
        forEach(address -> {
            float dx = PickupStruct.x(address) - x;
            float dy = PickupStruct.y(address) - y;
            if (dx*dx + dy*dy <= r*r && filter.test(address)) {
                pickups.add(address);
            }
        });
        return pickups;
    }

    private long getInactive() {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += PickupStruct.SIZE) {
            if (PickupStruct.type(baseAddress) == PickupType.INACTIVE) {
                return baseAddress;
            }
        }

        // Unreachable
        throw new OutOfMemoryError("Pickup Pool out of inactive slots");
    }

    public void forEach(Consumer<Long> action) {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += PickupStruct.SIZE) {
            if (PickupStruct.type(baseAddress) != PickupType.INACTIVE) {
                action.accept(baseAddress);
            }
        }
    }

    public void update() {
        float playerX = SurvivorDungeon.player.basePlayer.hb.cX;
        float playerY = SurvivorDungeon.player.basePlayer.hb.cY;

        double pullRange = Math.pow(SurvivorDungeon.player.pickupRangeMultiplier * AbstractSurvivorPlayer.PICKUP_PULL_RANGE, 2);
        double collectRange = Math.pow(AbstractSurvivorPlayer.PICKUP_COLLECT_RANGE, 2);

        forEach(address -> {
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

    public void render(SpriteBatch sb) {
        forEach(address -> {
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

    public void move(float x, float y) {
        forEach(address -> {
            PickupStruct.x(address, PickupStruct.x(address) + x);
            PickupStruct.y(address, PickupStruct.y(address) + y);
        });
    }
}
