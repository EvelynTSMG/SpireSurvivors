package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.HashMap;
import java.util.Map;

import static SpireSurvivors.pickups.PickupStruct.*;

/**
 * Defines pickup behavior, constants, and fields for managed initialization
 * (e.g. for unique drops which want to extend a class)<br>
 * Use {@link PickupPool} when possible.
 * @see PickupStruct
 */
public class AbstractPickup {
    // Compression combines 2^COMPRESSION_FACTOR pickups of the same type into one pickup
    public final static int COMPRESSION_FACTOR = 3;
    // Compression will attempt to find enough items within COMPRESSION_RANGE radius
    public final static float COMPRESSION_RANGE = 10f;
    // Compression may compress extra pickups and put them within COMPRESSION_SPAWN_RANGE radius
    public final static float COMPRESSION_SPAWN_RANGE = 10f;

    public enum PickupType implements PickupBehavior {
        INACTIVE(0, ImageMaster.WHITE_RING, 0, 0, FLAG_ALL),
        XP(1, ImageMaster.SCROLL_BAR_TRAIN, 10f, 4f, 0) {
            public void onTouch(long address) {
                SurvivorDungeon.player.gainXP(PickupStruct.value(address));
            }
        };

        public final int id;
        public final Texture texture;
        public final TextureRegion region;
        public final float bobDistance;
        public final float bobSpeed;
        public final int flags;

        static final Map<Integer, PickupType> map = new HashMap<>();

        PickupType(int id) {
            this(id, (Texture)null, 0, 0, 0);
        }

        PickupType(int id, Texture texture, float bobDistance, float bobSpeed, int flags) {
            this.id = id;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;
            this.flags = flags;

            this.texture = texture;
            this.region = null;
        }

        PickupType(int id, TextureRegion region, float bobDistance, float bobSpeed, int flags) {
            this.id = id;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;
            this.flags = flags;

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
