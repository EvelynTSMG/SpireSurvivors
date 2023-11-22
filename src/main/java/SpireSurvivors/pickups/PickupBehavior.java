package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import com.badlogic.gdx.math.Vector2;

/**
 * Defines default behavior for pickup types.
 */
public interface PickupBehavior {
    /**
     * Called while the pickup is within the player's pickup collection range.
     * @param address A pointer to an instance of {@link PickupStruct}.
     */
    default void onTouch(long address) { }

    /**
     * Called when the pickup is collected. After this resolves, the pickup will be removed.
     * @param address A pointer to an instance of {@link PickupStruct}.
     */
    default void onCollect(long address) { }

    /**
     * Called while the pickup is within the player's pickup collection range.
     * @param address A pointer to an instance of {@link PickupStruct}.
     * @return Whether the pickup can be collected.
     */
    default boolean canCollect(long address) {
        return !PickupStruct.persistent(address);
    }

    /**
     * Called before the pickup at {@code address} is updated by a pool.<br>
     * Return {@code false} if normal update process should not continue.<br>
     * For example, if the pointer is no longer valid.
     * @param address A pointer to an instance of {@link PickupStruct}.
     * @return Whether the update can continue as normal.
     */
    default boolean update(long address) {
        return true;
    }

    /**
     * Called while the pickup is in the player's pickup pull range and the pickup's {@code NO_PULL} flag isn't set.<br>
     * The first frame the pickup started being pulled, the {@code BEING_PULLED} flag will be false.<br>
     * The timer of the struct will also not be reset to 0.
     * @param address A pointer to an instance of {@link PickupStruct}.
     */
    default void pull(long address) {
        // By default, pull directly towards the player at the player's pull speed + bonus from pull time
        float dx = SurvivorDungeon.player.basePlayer.hb.cX - PickupStruct.x(address);
        float dy = SurvivorDungeon.player.basePlayer.hb.cY - PickupStruct.y(address);
        // Double speed every 2s
        float pullTime = PickupStruct.beingPulled(address) ? PickupStruct.timer(address) : 0;
        float pullTimeBonus = AbstractSurvivorPlayer.PICKUP_PULL_SPEED * pullTime / 2f;

        Vector2 distance = new Vector2(dx, dy).nor().scl(AbstractSurvivorPlayer.PICKUP_PULL_SPEED + pullTimeBonus);
        PickupStruct.x(address, PickupStruct.x(address) + distance.x);
        PickupStruct.y(address, PickupStruct.y(address) + distance.y);
    }
}
