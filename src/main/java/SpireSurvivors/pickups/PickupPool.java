package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PickupPool {
    private final static Unsafe unsafe = ReflectionHacks.getPrivateStatic(Unsafe.class, "theUnsafe");
    public final static int POOL_SIZE = 1024;
    private final long poolAddress;
    private int usedPickups = 0;

    public PickupPool() {
        poolAddress = unsafe.allocateMemory(POOL_SIZE * AbstractPickup.SIZE);
    }

    public void spawn(float x, float y, PickupType type) {
        long address = getInactive();
        AbstractPickup.clear(address);

        AbstractPickup.type(address, type);
        usedPickups += 1;

        AbstractPickup.x(address, x);
        AbstractPickup.y(address, y);

        AbstractPickup.scale(address, 1f);
        AbstractPickup.bobTimer(address, MathUtils.random(0, (float)Math.PI * 2));
    }

    public void remove(long address) {
        AbstractPickup.type(address, PickupType.INACTIVE);
        usedPickups -= 1;
    }

    public void runForNearby(float x, float y, float r, Consumer<Long> action) {
        forEach(address -> {
            float dx = AbstractPickup.x(address) - x;
            float dy = AbstractPickup.y(address) - y;
            if (dx*dx + dy*dy <= r*r) {
                action.accept(address);
            }
        });
    }

    public ArrayList<Long> nearby(float x, float y, float r) {
        ArrayList<Long> pickups = new ArrayList<>();
        forEach(address -> {
            float dx = AbstractPickup.x(address) - x;
            float dy = AbstractPickup.y(address) - y;
            if (dx*dx + dy*dy <= r*r) {
                pickups.add(address);
            }
        });
        return pickups;
    }

    private long getInactive() {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += AbstractPickup.SIZE) {
            if (AbstractPickup.type(baseAddress) == PickupType.INACTIVE) {
                return baseAddress;
            }
        }

        // Unreachable
        throw new OutOfMemoryError("Pickup Pool out of inactive slots");
    }

    public void forEach(Consumer<Long> action) {
        long baseAddress = poolAddress;
        for (int i = 0; i < POOL_SIZE; i++, baseAddress += AbstractPickup.SIZE) {
            if (AbstractPickup.type(baseAddress) != PickupType.INACTIVE) {
                action.accept(baseAddress);
            }
        }
    }

    public void update() {
        float playerX = SurvivorDungeon.player.basePlayer.hb.cX;
        float playerY = SurvivorDungeon.player.basePlayer.hb.cY;
        float pullRange = SurvivorDungeon.player.pickupRangeMultiplier * AbstractSurvivorPlayer.PICKUP_PULL_RANGE;
        forEach(address -> {
            float dx = AbstractPickup.x(address) - playerX;
            float dy = AbstractPickup.y(address) - playerY;

            // Try to collect pickup
            if (dx*dx + dy*dy <= AbstractSurvivorPlayer.PICKUP_COLLECT_RANGE) {
                AbstractPickup.onTouch(address);
                remove(address);
            }

            // Bobby Pickups
            AbstractPickup.bobTimer(address, AbstractPickup.bobTimer(address) + Gdx.graphics.getDeltaTime());
            if (AbstractPickup.bobTimer(address) > Math.PI * 2) {
                // Preemptively avoiding reaching limits in long runs
                AbstractPickup.bobTimer(address, AbstractPickup.bobTimer(address) - (float)Math.PI * 2);
            }

            // Try to pull towards player
            if (!AbstractPickup.noPull(address)) {
                if (dx*dx + dy*dy <= pullRange*pullRange) {
                    Vector2 distance = new Vector2(dx, dy).nor().scl(AbstractSurvivorPlayer.PICKUP_PULL_SPEED);
                    AbstractPickup.x(address, AbstractPickup.x(address) + distance.x);
                    AbstractPickup.y(address, AbstractPickup.y(address) + distance.y);
                }
            }
        });
    }

    public enum PickupType {
        INACTIVE(0, ImageMaster.WHITE_RING, true, 0, 0),
        XP(1, ImageMaster.SCROLL_BAR_TRAIN, false, 10f, 4f);

        public final int id;
        public final Texture texture;
        public final TextureRegion region;
        public final float bobDistance;
        public final float bobSpeed;
        public final boolean noPull;

        static final Map<Integer, PickupType> map = new HashMap<>();

        PickupType(int id, Texture texture, boolean noPull, float bobDistance, float bobSpeed) {
            this.id = id;
            this.noPull = noPull;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;

            this.texture = texture;
            this.region = null;
        }

        PickupType(int id, TextureRegion region, boolean noPull, float bobDistance, float bobSpeed) {
            this.id = id;
            this.noPull = noPull;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;

            this.texture = null;
            this.region = region;
        }

        static {
            for (PickupType type : PickupType.values()) {
                map.put(type.id, type);
            }
        }

        public static PickupType deserialize(int id) {
            return map.get(id);
        }

        public static int serialize(PickupType type) {
            return type.id;
        }
    }
}
