package SpireSurvivors.dungeon;

import SpireSurvivors.SpireSurvivorsMod;
import SpireSurvivors.characters.BasicCharacter;
import SpireSurvivors.entity.AbstractSurvivorMonster;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import SpireSurvivors.patches.CardCrawlGamePatches;
import SpireSurvivors.pickups.AbstractPickup;
import SpireSurvivors.pickups.PickupPool;
import SpireSurvivors.screens.survivorGame.SurvivorChoiceScreen;
import SpireSurvivors.screens.survivorGame.SurvivorPauseScreen;
import SpireSurvivors.ui.SurvivorUI;
import SpireSurvivors.util.SpawnController;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.helpers.input.InputAction;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public class SurvivorDungeon {
    public enum CurrentScreen {
        NONE,
        PAUSE,
        CHOICE,
        DEATH
    }
    public static final TextureAtlas.AtlasRegion BACKGROUND = new TextureAtlas(Gdx.files.internal("bottomScene/scene.atlas")).findRegion("event");
    public static final InputAction UP = new InputAction(Input.Keys.W);
    public static final InputAction LEFT = new InputAction(Input.Keys.A);
    public static final InputAction DOWN = new InputAction(Input.Keys.S);
    public static final InputAction RIGHT = new InputAction(Input.Keys.D);
    public static final InputAction PAUSE = new InputAction(Input.Keys.ESCAPE);

    public static AbstractSurvivorPlayer player;
    public static SurvivorUI ui;
    public static ArrayList<AbstractSurvivorMonster> monsters = new ArrayList<>();
    public static ArrayList<PickupPool> pickupPools = new ArrayList<>();
    public static ArrayList<AbstractGameEffect> effects = new ArrayList<>();
    public static ArrayList<AbstractGameEffect> effectsQueue = new ArrayList<>();

    public static float worldX, worldY;
    public static TiledMap map;
    public static OrthographicCamera camera;
    public static OrthogonalTiledMapRenderer mapRenderer;

    public static DynamicBanner dynamicBanner;
    public static SurvivorPauseScreen survivorPauseScreen;
    public static SurvivorChoiceScreen survivorChoiceScreen;
    public static CurrentScreen currentScreen;
    public static boolean isScreenUp;

    public static SpawnController spawnController;

    static {
        try {
            Class<?> lightsOutMod = Class.forName("LightsOut.LightsOutMod");
            Method register = lightsOutMod.getMethod("registerLightManager", String.class, Collection.class);
            register.invoke(null, SpireSurvivorsMod.makeID("DungeonEffects"), effects);
            register.invoke(null, SpireSurvivorsMod.makeID("DungeonMonster"), monsters);
            register.invoke(null, SpireSurvivorsMod.makeID("DungeonPickups"), pickupPools);
            SpireSurvivorsMod.logger.info("Lights Out detected, dungeon supports lighting");
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}
    }

    public SurvivorDungeon(AbstractPlayer player) {
        SurvivorDungeon.player = SpireSurvivorsMod.registeredCharacters.getOrDefault(player.chosenClass, BasicCharacter::new).apply(player);
        AbstractDungeon.miscRng = new Random(Settings.seed);
        CardCrawlGame.music.changeBGM(Exordium.ID);
        Settings.hideCombatElements = false;
        clear();
        currentScreen = CurrentScreen.NONE;
        isScreenUp = false;
        ui = new SurvivorUI();
        dynamicBanner = new DynamicBanner();
        survivorPauseScreen = new SurvivorPauseScreen();
        survivorChoiceScreen = new SurvivorChoiceScreen();
        spawnController = new SpawnController();
        CardCrawlGame.fadeIn(0.5f);
        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        map = new TmxMapLoader().load(SpireSurvivorsMod.getModID()+"Resources/tiled/TestMap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 16f * Settings.scale);

        pickupPools.add(new PickupPool());
    }

    public void update() {
        if (player.rewards > 0) {
            survivorChoiceScreen.open(false);
            player.rewards--;
        }
        switch (currentScreen) {
            case PAUSE:
                survivorPauseScreen.update();
                break;
            case CHOICE:
                survivorChoiceScreen.update();
                break;
            case DEATH:
                break;
            case NONE:
                updateGameLogic();
                break;
        }
        ui.update();
        dynamicBanner.update();
    }

    public void updateGameLogic() {
        player.update();
        updateInput();
        monsters.removeIf(m -> {
            if (m.monster.isDead) {
                for (int i = 0; i < m.xpCount; i++) {
                    float x = m.monster.hb.cX;
                    float y = m.monster.hb.cY;
                    if (m.xpCount > 1) PickupPool.spawnScattered(x, y, AbstractPickup.PickupType.XP, m.xpCompression, false);
                    else PickupPool.spawn(x, y, AbstractPickup.PickupType.XP, m.xpCompression, false);
                }
            }
            return m.monster.isDead;
        });

        for (AbstractSurvivorMonster m : monsters) {
            m.update();
        }
        spawnController.update();

        effects.addAll(effectsQueue);
        effectsQueue.clear();
        effects.addAll(AbstractDungeon.effectsQueue);
        AbstractDungeon.effectsQueue.clear();

        for (AbstractGameEffect e : effects) {
            e.update();
        }
        effects.removeIf(e -> e.isDone);

        PickupPool.update();

        if (player.basePlayer.isDead) {
            CardCrawlGame.startOver();
            CardCrawlGamePatches.survivorGame.clear();
            CardCrawlGamePatches.survivorGame = null;
        }
    }

    public void updateInput() {
        if (PAUSE.isJustPressed()) {
            survivorPauseScreen.open(true);
        }

        if (player.movementTutorial.alpha != 0) {
            player.movementTutorial.up.justPressed = UP.isJustPressed();
            player.movementTutorial.down.justPressed = DOWN.isJustPressed();
            player.movementTutorial.right.justPressed = RIGHT.isJustPressed();
            player.movementTutorial.left.justPressed = LEFT.isJustPressed();
        }

        Vector2 dir = new Vector2();
        if (UP.isPressed()) {
            dir.y += 1;
        }
        if (DOWN.isPressed()) {
            dir.y -= 1;
        }
        if (LEFT.isPressed()) {
            dir.x -= 1;
        }
        if (RIGHT.isPressed()) {
            dir.x += 1;
        }

        if (dir.len() != 0) {
            dir.nor();
            dir.scl(player.speed * player.speedMultiplier);
            //worldX -= dir.x;
            //worldY -= dir.y;
            transformWorld(dir);
        }
    }

    public void transformWorld(Vector2 dir) {
        for (AbstractSurvivorMonster m : monsters) {
            m.move(-dir.x, -dir.y);
        }
        PickupPool.move(-dir.x, -dir.y);
        worldX += dir.x;
        worldY += dir.y;
        camera.translate(dir);
    }

    public void render(SpriteBatch sb) {
        sb.draw(BACKGROUND, 0, 0, Settings.WIDTH, Settings.HEIGHT);
        sb.end();
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();
        sb.begin();

        PickupPool.render(sb);

        for (AbstractSurvivorMonster m : monsters) {
            if (m.monster.hb.cY <= Settings.HEIGHT/2f) {
                m.render(sb);
            }
        }

        player.render(sb);

        for (AbstractSurvivorMonster m : monsters) {
            if (m.monster.hb.cY > Settings.HEIGHT/2f) {
                m.render(sb);
            }
        }

        for (AbstractGameEffect e : effects) {
            e.render(sb);
        }

        ui.render(sb);
        switch (currentScreen) {
            case PAUSE:
                survivorPauseScreen.render(sb);
                break;
            case CHOICE:
                survivorChoiceScreen.render(sb);
                break;
            case DEATH:
                break;
            case NONE:
                break;
        }

        dynamicBanner.render(sb);
    }

    public void clear() {
        monsters.clear();
        effects.clear();
        effectsQueue.clear();
        pickupPools.clear();
        if (map != null) {
            map.dispose();
        }
    }
}
