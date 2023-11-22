package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    /**
     * Compression combines 2<sup>{@code COMPRESSION_FACTOR}</sup> pickups of the same type into one pickup
     */
    public final static int COMPRESSION_FACTOR = 3;
    /**
     * Compression will attempt to find enough items within {@code COMPRESSION_RANGE} radius
     */
    public final static float COMPRESSION_RANGE = 20f;
    /**
     * Compression may compress additional pickups and spawn them within {@code SCATTER_RANGE} radius
     * of the original pickup the compression targeted.<br>
     * Can also be used to scatter pickups on spawn.
     */
    public final static float SCATTER_RANGE = 30f;

    /*===== Textures =====*/
    public final static Texture IMAGE_XP = new Texture("SpireSurvivorsResources/images/pickups/xp.png");

    /**
     * Returns the value of the pickup with a compression level of {@code compression}.
     * @param compression The compression level.
     * @return The value of a pickup at the given compression level.
     */
    public static int value(int compression) {
        return 1 << (compression * COMPRESSION_FACTOR);
    }

    /**
     * Sets the color of {@code sb} (depending on {@code compression}) to one of: <ul>
     * <li>between <span color="#FFFF00">Yellow</span> and <span color="#FFFF4B">Yellow</span>,</li>
     * <li>between <span color="#FFA500">Orange</span> and <span color="#FFF04B">Orange</span>,</li>
     * <li>between <span color="#FF0000">Red</span> and <span color="#FF4B4B">Red</span>,</li>
     * <li>between <span color="#FF00FF">Magenta</span> and <span color="#FF4BFF">Magenta</span>,</li>
     * <li>between <span color="#A020F0">Purple</span> and <span color="#EB6BFF">Purple</span>,</li>
     * <li>between <span color="#0000FF">Blue</span> and <span color="#4B4BFF">Blue</span>,</li>
     * <li>between <span color="#00FFFF">Cyan</span> and <span color="#4BFFFF">Cyan</span>,</li>
     * <li>between <span color="#00FF00">Green</span> and <span color="#4BFF4B">Green</span>.</li></ul>
     * When {@code compression} is over 8, the color loops back to the first one and its lightness is increased.
     * @param sb The SpriteBatch to set the color for
     * @param compression The compression to base the color off of
     */
    public static void setColorForCompression(SpriteBatch sb, int compression) {
        int plus = (compression/8 * 20)/255;
        switch (compression % 8) {
            case 0: sb.setColor(Color.YELLOW.cpy().add(plus, plus, plus, 0)); break;
            case 1: sb.setColor(Color.ORANGE.cpy().add(plus, plus, plus, 0)); break;
            case 2: sb.setColor(Color.RED.cpy().add(plus, plus, plus, 0)); break;
            case 3: sb.setColor(Color.MAGENTA.cpy().add(plus, plus, plus, 0)); break;
            case 4: sb.setColor(Color.PURPLE.cpy().add(plus, plus, plus, 0)); break;
            case 5: sb.setColor(Color.BLUE.cpy().add(plus, plus, plus, 0)); break;
            case 6: sb.setColor(Color.CYAN.cpy().add(plus, plus, plus, 0)); break;
            case 7: sb.setColor(Color.GREEN.cpy().add(plus, plus, plus, 0)); break;
        }
    }

    /**
     * Defines different types of enums, including behavior and type-dependant properties like {@code image} and default {@code flags}.
     */
    public enum PickupType implements PickupBehavior {
        XP(1, IMAGE_XP, 10f, 4f, true, 0) {
            public void onCollect(long address) {
                SurvivorDungeon.player.gainXP(PickupStruct.value(address));
            }
        };

        public final int id;
        public final TextureRegion image;
        public final float bobDistance;
        public final float bobSpeed;
        public final boolean compressable;
        public final int flags;

        static final Map<Integer, PickupType> map = new HashMap<>();

        PickupType(int id, Texture texture, float bobDistance, float bobSpeed, boolean compressable, int flags) {
            if (id == 0) throw new IllegalArgumentException("PickupType id cannot be 0");
            this.id = id;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;
            this.compressable = compressable;
            this.flags = flags;

            this.image = new TextureRegion(texture);
        }

        PickupType(int id, TextureRegion region, float bobDistance, float bobSpeed, boolean compressable, int flags) {
            if (id == 0) throw new IllegalArgumentException("PickupType id cannot be 0");
            this.id = id;
            this.bobDistance = bobDistance;
            this.bobSpeed = bobSpeed;
            this.compressable = compressable;
            this.flags = flags;

            this.image = region;
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

        public boolean noPull() {
            return (flags & PickupStruct.FLAG_NO_PULL) != 0;
        }

        public boolean noBob() {
            return (flags & PickupStruct.FLAG_NO_BOB) != 0;
        }

        public boolean persistent() {
            return (flags & PickupStruct.FLAG_PERSISTENT) != 0;
        }
    }
}
