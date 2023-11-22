package SpireSurvivors.ui.elements;

import SpireSurvivors.SpireSurvivorsMod;
import SpireSurvivors.util.ImageHelper;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

public class ProgressBar {
    public static float SNAP_MARGIN = 1f / Math.max(Settings.WIDTH, Settings.HEIGHT);
    public static float INSTANT_MARGIN = 1 - SNAP_MARGIN;
    public Texture background;
    public Texture fill;
    public Texture overlay;
    public float min;
    public float max;
    private float value;
    private float current;
    private float lerpInitial;
    private float lerpScalar;
    private float lerpProgress;

    public ProgressBar(Texture background, Texture fill, Texture overlay, float min, float max, float value, float lerpLength) {
        this.background = background;
        this.fill = fill;
        this.overlay = overlay;
        this.min = min;
        this.max = max;
        this.value = this.current = value;
        setLerpLength(lerpLength);
    }

    public void update() {
        if (lerpScalar >= INSTANT_MARGIN) current = value;
        if (current - value <= SNAP_MARGIN) return;
        lerpProgress += Gdx.graphics.getDeltaTime() * lerpScalar;
        current = MathUtils.lerp(lerpInitial, value, lerpProgress);
    }

    public void setProgress(float progress) {
        if (lerpScalar >= INSTANT_MARGIN) {
            lerpProgress = 1;
            lerpInitial = progress;
            current = value = MathUtils.clamp(progress, min, max);
            return;
        }

        if (value == progress) return;

        lerpProgress = 0;
        lerpInitial = value;
        value = MathUtils.clamp(progress, min, max);
    }

    public float getProgress() {
        return value;
    }

    public float getPercentage() {
        return (value - min) / (max - min);
    }

    public void setLerpLength(float lerpLength) {
        float oldScalar = lerpScalar;
        if (lerpLength == 0) lerpScalar = 1;
        else lerpScalar = 1/lerpLength;

        lerpProgress *= oldScalar/lerpScalar;
    }

    public void render(SpriteBatch sb, float x, float y, float scale) {
        render(sb, x, y, scale, scale);
    }

    public void render(SpriteBatch sb, float x, float y, float scaleX, float scaleY) {
        sb.setColor(Color.WHITE);
        if (background != null) {
            sb.draw(new TextureRegion(background), x - background.getWidth()/2f, y - background.getHeight()/2f,
                background.getWidth() / 2f, background.getHeight() / 2f,
                background.getWidth(), background.getHeight(),
                Settings.scale * scaleX, Settings.scale * scaleY, 0);
        }

        sb.draw(new TextureRegion(fill), x - fill.getWidth()/2f, y - fill.getHeight()/2f,
            fill.getWidth()/2f, fill.getHeight()/2f,
            fill.getWidth() * getPercentage(), fill.getHeight(),
            Settings.scale * scaleX, Settings.scale * scaleY, 0);

        if (overlay != null) {
            sb.draw(new TextureRegion(overlay), x - overlay.getWidth()/2f, y - overlay.getHeight()/2f,
                overlay.getWidth() / 2f, overlay.getHeight() / 2f,
                overlay.getWidth(), overlay.getHeight(),
                Settings.scale * scaleX, Settings.scale * scaleY, 0);
        }
    }
}
