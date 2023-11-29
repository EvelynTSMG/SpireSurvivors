package SpireSurvivors.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.util.Arrays;

/**
 * A replacement to Mega Crit's {@link com.megacrit.cardcrawl.helpers.input.InputAction InputAction}.
 */
public class InputAction {
    private final static int KEY_COUNT = 3;

    private final int[] keys = new int[KEY_COUNT];
    private int button;
    private boolean buttonSet = false;
    private boolean buttonJustPressed = false;
    private boolean buttonWasPressed = false;

    /**
     * Create a new {@code InputAction}.<br>
     * The first key in {@code keys} is considered the main one for the purposes of {@link #getKeyString()}.<br>
     * To remap a key, use {@link #remap} instead.
     * @param keys The keys this {@code InputAction} should use.
     */
    public InputAction(int... keys) {
        System.arraycopy(keys, 0, this.keys, 0, Math.min(KEY_COUNT, keys.length));
    }

    /**
     * Set a mouse alternative to the keys.
     * @param button The mouse button to use.
     * @return Itself.
     */
    public InputAction alt(int button) {
        this.button = button;
        buttonSet = true;
        return this;
    }

    /**
     * Remap a key this {@code InputAction} should use.
     * @param idx The index of the key to be remapped.
     * @param key The key to remap.
     */
    public void remap(int idx, int key) {
        keys[idx] = key;
    }

    /**
     * @return The string representation of this {@code InputAction}'s main key.
     */
    public String getKeyString() {
        return Input.Keys.toString(keys[0]);
    }

    public boolean isJustPressed() {
        if (buttonSet) {
            if (!buttonWasPressed) buttonJustPressed = Gdx.input.isButtonPressed(button);
            else buttonJustPressed = false;
            buttonWasPressed = Gdx.input.isButtonPressed(button);
        }
        return Arrays.stream(keys).anyMatch(Gdx.input::isKeyJustPressed) || buttonJustPressed;
    }

    public boolean isPressed() {
        return Arrays.stream(keys).anyMatch(Gdx.input::isKeyPressed) || buttonWasPressed;
    }
}
