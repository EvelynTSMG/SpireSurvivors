package SpireSurvivors.entity;

import SpireSurvivors.SpireSurvivorsMod;
import SpireSurvivors.cards.abstracts.AbstractRelicCard;
import SpireSurvivors.cards.abstracts.AbstractWeaponCard;
import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.ui.MovementTutorial;
import SpireSurvivors.ui.elements.ProgressBar;
import SpireSurvivors.util.PolygonHelper;
import SpireSurvivors.weapons.abstracts.AbstractSurvivorWeapon;
import basemod.ReflectionHacks;
import basemod.abstracts.CustomPlayer;
import basemod.animations.AbstractAnimation;
import basemod.animations.SpriterAnimation;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.brashmonkey.spriter.Player;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.combat.StrikeEffect;

import java.lang.reflect.Field;
import java.util.ArrayList;

public abstract class AbstractSurvivorPlayer extends AbstractSurvivorEntity {
    public static final float PICKUP_RANGE = 100f * Settings.scale;
    public static final float INV_TIME = 0.5f;

    public AbstractPlayer basePlayer;
    public ArrayList<AbstractWeaponCard> weaponCards = new ArrayList<>();
    public ArrayList<AbstractRelicCard> relicCards = new ArrayList<>();

    public float speedMultiplier = 1f;
    public float attackspeedModifier = 1f;
    public float critChance = 0f;
    public float critDamage = 2f;
    public float attackSizeModifier = 1f;
    public float projectileSpeed = 1f;
    public float projectileDuration = 1f;
    public int projectileBounce = 0;
    public int extraProjectiles = 0;
    public float summonDamage = 0f;
    public float summonSpeed = 1f;
    public float cooldownModifier = 1f;
    public float regeneration = 0f;
    public float lifesteal = 0f;
    public int armor = 0;
    public float dodge = 0f;
    public float area = 1f;
    public float luck = 1f;
    public float experienceModifier = 1f;
    public float levelModifier = 1f;
    public float curse = 1f; // Makes enemies stronger and more numerous. Drawback to certain strong options
    public float pickupRangeMultiplier = 1f;
    public int revives = 0;
    public int rerolls = 0;
    public int skips = 0;
    public int banishes = 0;
    public int currentXP = 0;
    public int currentLevel = 1;
    public int rewards = 0;
    public float invTime = 0;

    public MovementTutorial movementTutorial = new MovementTutorial();
    public static final float MOVEMENT_TUTORIAL_OFFSET = 80f * Settings.scale;

    public ProgressBar healthBar;
    public static final Texture HEALTH_BAR_BG = new Texture("SpireSurvivorsResources/images/ui/bars/health-bg.png");
    public static final Texture HEALTH_BAR_FILL = new Texture("SpireSurvivorsResources/images/ui/bars/health-fill.png");
    public static final float HEALTH_BAR_SCALE = 0.5f;
    public static final float HEALTH_BAR_OFFSET = 60f;

    public AbstractSurvivorPlayer(AbstractPlayer p) {
        speed = 5f;
        damageModifier = 1f;
        basePlayer = p;
        basePlayer.drawX = Settings.WIDTH / 2f;
        basePlayer.drawY = Settings.HEIGHT / 2f;
        basePlayer.hb.width /= 4f;
        basePlayer.hb.height /= 6f;
        basePlayer.hb.move(Settings.WIDTH / 2f, Settings.HEIGHT / 2f + basePlayer.hb.height / 2f);
        hitbox = PolygonHelper.fromHitbox(basePlayer.hb);
        basePlayer.showHealthBar();

        healthBar = new ProgressBar(HEALTH_BAR_BG, HEALTH_BAR_FILL, null,
                0, basePlayer.maxHealth, basePlayer.currentHealth, 0);

        if (basePlayer instanceof CustomPlayer) {
            AbstractAnimation animation = ReflectionHacks.getPrivate(basePlayer, CustomPlayer.class, "animation");
            if (animation instanceof SpriterAnimation) {
                ((SpriterAnimation) animation).myPlayer.setScale(((SpriterAnimation) animation).myPlayer.getScale() * 0.5f);
                if (!animation.getClass().equals(SpriterAnimation.class)) {
                    for (Field f : animation.getClass().getFields()) {
                        if (f.getType().equals(Player.class)) {
                            f.setAccessible(true);
                            try {
                                Player spriterPlayer = (Player) f.get(animation);
                                spriterPlayer.setScale(spriterPlayer.getScale() * 0.5f);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void damage(AbstractSurvivorEntity attacker, AbstractSurvivorWeapon weapon) {
        if (invTime > 0) return;
        basePlayer.currentHealth -= weapon.damage;
        basePlayer.healthBarUpdatedEvent();
        SurvivorDungeon.effectsQueue.add(new StrikeEffect(basePlayer, basePlayer.hb.cX, basePlayer.hb.cY, weapon.damage));
        invTime = INV_TIME;

        if (basePlayer.currentHealth <= 0) {
            CardCrawlGame.music.dispose();
            CardCrawlGame.sound.play("DEATH_STINGER", true);
            /*String bgmKey = null;
            switch (MathUtils.random(0, 3)) {
                case 0:
                    bgmKey = "STS_DeathStinger_1_v3_MUSIC.ogg";
                    break;
                case 1:
                    bgmKey = "STS_DeathStinger_2_v3_MUSIC.ogg";
                    break;
                case 2:
                    bgmKey = "STS_DeathStinger_3_v3_MUSIC.ogg";
                    break;
                case 3:
                    bgmKey = "STS_DeathStinger_4_v3_MUSIC.ogg";
                    break;
            }
            CardCrawlGame.music.playTempBgmInstantly(bgmKey, false);*/
            basePlayer.playDeathAnimation();
            basePlayer.isDead = true;
        }
    }

    @Override
    public void update() {
        super.update();
        if (invTime > 0) invTime -= Gdx.graphics.getDeltaTime();
        if (invTime < 0) invTime = 0;
        this.basePlayer.flipHorizontal = InputHelper.mX < basePlayer.hb.cX;

        healthBar.setProgress(basePlayer.currentHealth);
        healthBar.update();

        ReflectionHacks.privateMethod(AbstractCreature.class, "updateHealthBar").invoke(basePlayer);
    }

    @Override
    public void render(SpriteBatch sb) {
        basePlayer.renderPlayerImage(sb);

        // TODO: Fix healthbar overlapping some characters, finetune healthbar position
        healthBar.render(sb, basePlayer.hb.cX, basePlayer.hb.y + basePlayer.hb.height + HEALTH_BAR_OFFSET, HEALTH_BAR_SCALE);

        if (!SpireSurvivorsMod.seenMovementTutorial) {
            movementTutorial.render(sb, basePlayer.hb.cX, basePlayer.hb.y - MOVEMENT_TUTORIAL_OFFSET);
        }

        basePlayer.hb.render(sb);
    }

    public void gainXP(int amount) {
        currentXP += amount;
        while (currentXP >= xpToNextLevel()) {
            currentXP -= xpToNextLevel();
            currentLevel++;
            rewards++;
        }
    }

    public int xpToNextLevel() {
        return (int) (2 * currentLevel + 5 * Math.pow(currentLevel-1, 2));
    }
}
