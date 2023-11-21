package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import com.badlogic.gdx.math.Vector2;

public interface PickupBehavior {
    /**
     * Called while the pickup is within the player's pickup collection range
     * @param address Pointer to an instance of {@link PickupStruct}
     */
    default void onTouch(long address) { }

    /**
     * Called when the pickup is collected. After this resolves, the pickup will be removed.
     * @param address Pointer to an instance of {@link PickupStruct}
     */
    default void onCollect(long address) { }

    /**
     * Called while the pickup is within the player's pickup collection range
     * @param address Pointer to an instance of {@link PickupStruct}
     * @return Whether the pickup can be collected
     */
    default boolean canCollect(long address) {
        return !PickupStruct.persistent(address);
    }

    /**
     * Called before the pickup at {@code address} is updated by a pool.
     * @param address Pointer to an instance of {@link PickupStruct}
     * @return Whether the update can continue as normal. Return {@code false} if e.g. {@code remove(address)} was called
     */
    default boolean update(long address) {
        return true;
    }

    /**
     * Called while the pickup is in the player's pickup pull range and the pickup's NO_PULL flag isn't set
     * @param address Pointer to an instance of {@link PickupStruct}
     */
    default void pull(long address) {
        // By default, pull directly towards the player at the player's pull speed
        float dx = SurvivorDungeon.player.basePlayer.hb.cX - PickupStruct.x(address);
        float dy = SurvivorDungeon.player.basePlayer.hb.cY - PickupStruct.y(address);

        Vector2 distance = new Vector2(dx, dy).nor().scl(AbstractSurvivorPlayer.PICKUP_PULL_SPEED);
        PickupStruct.x(address, PickupStruct.x(address) + distance.x);
        PickupStruct.y(address, PickupStruct.y(address) + distance.y);
    }
}
