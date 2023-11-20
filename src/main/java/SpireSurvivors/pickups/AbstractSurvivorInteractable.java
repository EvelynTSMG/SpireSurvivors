package SpireSurvivors.pickups;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.vfx.BobEffect;

public abstract class AbstractSurvivorInteractable {
    protected float x, y;
    protected float scale = 1f;
    protected float rotation = 0f;
    protected final BobEffect bob = new BobEffect(10, 4);
    protected Hitbox hb;
    protected TextureAtlas.AtlasRegion region;
    protected Texture texture;
    protected boolean collideOnly;
    public boolean isDone;

    public AbstractSurvivorInteractable(Texture texture, float x, float y, float hbW, float hbH) {
        this.texture = texture;
        this.hb = new Hitbox(hbW, hbH);
        this.x = x;
        this.y = y;
        hb.move(x, y);
    }

    public AbstractSurvivorInteractable(TextureAtlas.AtlasRegion region, float x, float y, float hbW, float hbH) {
        this.region = region;
        this.hb = new Hitbox(hbW, hbH);
        this.x = x;
        this.y = y;
        hb.move(x, y);
    }

    public void update() {
        bob.update();
        if (!collideOnly) {
            Vector2 dist = new Vector2(SurvivorDungeon.player.basePlayer.hb.cX - hb.cX, SurvivorDungeon.player.basePlayer.hb.cY - hb.cY);
            if (dist.len() <= SurvivorDungeon.player.pickupRangeMultiplier * AbstractSurvivorPlayer.PICKUP_PULL_RANGE) {
                dist.nor();
                dist.scl(10f * Settings.scale);
                move(dist.x, dist.y);
            }
        }
        if (SurvivorDungeon.player.basePlayer.hb.intersects(hb)) {
            onTouch();
        }
    }

    public abstract void onTouch();

    public void move(float x, float y) {
        this.x += x;
        this.y += y;
        hb.move(hb.cX + x, hb.cY + y);
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (this.region != null) {
            sb.draw(region, x, y + bob.y, region.packedWidth/2f, region.packedHeight/2f, region.packedWidth, region.packedHeight, scale * Settings.scale, scale * Settings.scale, rotation);
        } else if (texture != null) {
            sb.draw(texture, x - texture.getWidth()/2f, y + bob.y -texture.getHeight()/2f, -texture.getWidth()/2f, -texture.getHeight()/2f, texture.getWidth(), texture.getHeight(), scale * Settings.scale, scale * Settings.scale, 0, 0, 0, texture.getWidth(), texture.getHeight(), false, false);
        }
        hb.render(sb);
    }
}
