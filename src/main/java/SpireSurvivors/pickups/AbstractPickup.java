package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines pickup behavior, constants, and fields for managed initialization
 * (e.g. for unique drops which want to extend a class)<br>
 * Use {@link PickupPool} when possible.
 * @see PickupStruct
 */
public class AbstractPickup {
    // Compression combines 2^COMPRESSION_FACTOR pickups of the same type into one pickup
    public final static int COMPRESSION_FACTOR = 3;

    public static void onTouch(long baseAddress) {
        switch (PickupStruct.type(baseAddress)) {
            case XP: onTouchXp(baseAddress);
        }
    }

    public static void onTouchXp(long baseAddress) {
        SurvivorDungeon.player.gainXP(1 << (PickupStruct.compression(baseAddress) * COMPRESSION_FACTOR));
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
