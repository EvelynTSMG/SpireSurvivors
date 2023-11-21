package SpireSurvivors.entity;

import SpireSurvivors.dungeon.SurvivorDungeon;
import SpireSurvivors.util.PolygonHelper;
import SpireSurvivors.weapons.abstracts.AbstractSurvivorWeapon;
import SpireSurvivors.weapons.monster.MonsterCollisionWeapon;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.DamageNumberEffect;

public abstract class AbstractSurvivorMonster extends AbstractSurvivorEntity {
    public static final Color DAMAGE_TAKEN_COLOR = Color.RED.cpy();
    public static final Color DAMAGE_BLOCKED_COLOR = Color.CYAN.cpy();

    public AbstractMonster monster;
    public MonsterCollisionWeapon collisionWeapon;
    public int xpCompression = 0;
    public int xpCount = 1;

    public AbstractSurvivorMonster(AbstractMonster m, int collisionDamage, float moveSpeed) {
        monster = m;
        monster.hb.height /= 4f;
        monster.hb.width /= 4f;
        monster.hb_y -= monster.hb.height*3/2;
        hitbox = PolygonHelper.fromPosition(monster.hb.cX, monster.hb.cY - monster.hb.height*3/2, monster.hb.width, m.hb.height);
        //monster.showHealthBar();
        collisionWeapon = new MonsterCollisionWeapon(collisionDamage, 1f, 1);
        speed = moveSpeed;
        damageModifier = 1f;
    }

    public void move(float dx, float dy) {
        monster.drawX += dx;
        monster.drawY += dy;
        hitbox.translate(dx, dy);
        //monster.hb.move(monster.hb.cX + dx, monster.hb.cY + dy - monster.hb.height*3/2);
    }

    @Override
    public void damage(AbstractSurvivorEntity source, AbstractSurvivorWeapon weapon) {
        if (!monster.isDead) {
            float damage = weapon.damage*source.damageModifier;
            if (source instanceof AbstractSurvivorPlayer) {
                int crits = 0;
                float chance = ((AbstractSurvivorPlayer) source).critChance;
                while (chance >= 1) {
                    crits++;
                    chance--;
                }
                if (AbstractDungeon.cardRandomRng.random() < chance) {
                    crits++;
                }
                damage *= 1 + crits * (((AbstractSurvivorPlayer) source).critDamage-1);
            }

            for (AbstractPower p : SurvivorDungeon.player.basePlayer.powers) {
                damage = p.atDamageGive(damage, DamageInfo.DamageType.NORMAL);
            }
            for (AbstractPower p : monster.powers) {
                damage = p.atDamageReceive(damage, DamageInfo.DamageType.NORMAL);
            }
            for (AbstractPower p : SurvivorDungeon.player.basePlayer.powers) {
                damage = p.atDamageFinalGive(damage, DamageInfo.DamageType.NORMAL);
            }
            for (AbstractPower p : monster.powers) {
                damage = p.atDamageFinalReceive(damage, DamageInfo.DamageType.NORMAL);
            }

            monster.tint.color.set(damage > 0 ? DAMAGE_TAKEN_COLOR : DAMAGE_BLOCKED_COLOR);

            monster.currentHealth -= damage;
            //monster.healthBarUpdatedEvent();
            //SurvivorDungeon.effectsQueue.add(new StrikeEffect(monster, monster.hb.cX, monster.hb.cY, (int)damage));
            SurvivorDungeon.effectsQueue.add(new DamageNumberEffect(monster, monster.hb.cX, monster.hb.cY, (int)damage));
            if (monster.currentHealth <= 0) {
                monster.currentHealth = 0;
                monster.useFastShakeAnimation(1.0f);
                monster.isDead = true;
            }
        }
    }

    @Override
    public void update() {
        super.update();
        movementUpdate();
        monster.update();
        monster.flipHorizontal = SurvivorDungeon.player.basePlayer.hb.cX > monster.hb.cX;
        collisionWeapon.update();
        if (collisionWeapon.offCooldown() && PolygonHelper.collides(hitbox, SurvivorDungeon.player.hitbox)) {
            SurvivorDungeon.player.damage(this, collisionWeapon);
            collisionWeapon.onHit();
        }
    }

    public abstract void movementUpdate();

    @Override
    public void render(SpriteBatch sb) {
        Texture img = ReflectionHacks.getPrivate(monster, AbstractMonster.class, "img");
        Skeleton skeleton = ReflectionHacks.getPrivate(monster, AbstractCreature.class, "skeleton");
        if (ReflectionHacks.getPrivate(monster, AbstractCreature.class, "atlas") == null) {
            sb.setColor(monster.tint.color);
            if (img != null) {
                sb.draw(img, monster.drawX - (float)img.getWidth() * Settings.scale / 2.0F + monster.animX, monster.drawY + monster.animY, (float)img.getWidth() * Settings.scale, (float)img.getHeight() * Settings.scale, 0, 0, img.getWidth(), img.getHeight(), monster.flipHorizontal, monster.flipVertical);
            }
        } else {
            monster.state.update(Gdx.graphics.getDeltaTime());
            monster.state.apply(skeleton);
            skeleton.updateWorldTransform();
            skeleton.setPosition(monster.drawX + monster.animX, monster.drawY + monster.animY);
            skeleton.setColor(monster.tint.color);
            skeleton.setFlip(monster.flipHorizontal, monster.flipVertical);
            sb.end();
            CardCrawlGame.psb.begin();
            AbstractCreature.sr.draw(CardCrawlGame.psb, skeleton);
            CardCrawlGame.psb.end();
            sb.begin();
            sb.setBlendFunction(770, 771);
        }
        //monster.renderHealth(sb);
        monster.hb.render(sb);
    }
}
