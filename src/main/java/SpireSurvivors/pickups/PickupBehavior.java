package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import com.badlogic.gdx.math.Vector2;

public interface PickupBehavior {
    default void onTouch(long address) {

    }

    default boolean canCollect(long address) {
        return !PickupStruct.persistent(address);
    }

    default void pull(long address) {
        float dx = PickupStruct.x(address) - SurvivorDungeon.player.basePlayer.hb.cX;
        float dy = PickupStruct.y(address) - SurvivorDungeon.player.basePlayer.hb.cY;

        Vector2 distance = new Vector2(dx, dy).nor().scl(AbstractSurvivorPlayer.PICKUP_PULL_SPEED);
        PickupStruct.x(address, PickupStruct.x(address) + distance.x);
        PickupStruct.y(address, PickupStruct.y(address) + distance.y);
    }
}
