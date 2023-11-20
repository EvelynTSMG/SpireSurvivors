package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import SpireSurvivors.pickups.AbstractPickup.PickupType;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PickupPool {
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");
    public final static int POOL_SIZE = 1024;
    private final long poolAddress;
    private int usedPickups = 0;

    public PickupPool() {
        poolAddress = unsafe.allocateMemory(POOL_SIZE * PickupStruct.SIZE);
    }

    public void spawn(float x, float y, PickupType type) {
        long address = getInactive();
        PickupStruct.clear(address);

        PickupStruct.type(address, type);
        usedPickups += 1;

        PickupStruct.x(address, x);
        PickupStruct.y(address, y);

        PickupStruct.scale(address, 1f);
        PickupStruct.bobTimer(address, MathUtils.random(0, (float)Math.PI * 2));
    }

    public void remove(long address) {
        PickupStruct.type(address, PickupType.INACTIVE);
        usedPickups -= 1;
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
        float pullRange = SurvivorDungeon.player.pickupRangeMultiplier * AbstractSurvivorPlayer.PICKUP_PULL_RANGE;
        forEach(address -> {
            float dx = PickupStruct.x(address) - playerX;
            float dy = PickupStruct.y(address) - playerY;

            // Try to collect pickup
            if (dx*dx + dy*dy <= AbstractSurvivorPlayer.PICKUP_COLLECT_RANGE) {
                AbstractPickup.onTouch(address);
                remove(address);
            }

            // Bobby Pickups
            PickupStruct.bobTimer(address, PickupStruct.bobTimer(address) + Gdx.graphics.getDeltaTime());
            if (PickupStruct.bobTimer(address) > Math.PI * 2) {
                // Preemptively avoiding reaching limits in long runs
                PickupStruct.bobTimer(address, PickupStruct.bobTimer(address) - (float)Math.PI * 2);
            }

            // Try to pull towards player
            if (!PickupStruct.noPull(address)) {
                if (dx*dx + dy*dy <= pullRange*pullRange) {
                    Vector2 distance = new Vector2(dx, dy).nor().scl(AbstractSurvivorPlayer.PICKUP_PULL_SPEED);
                    PickupStruct.x(address, PickupStruct.x(address) + distance.x);
                    PickupStruct.y(address, PickupStruct.y(address) + distance.y);
                }
            }
        });
    }
}
