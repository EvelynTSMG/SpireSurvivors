package SpireSurvivors.cards.statCards;

import SpireSurvivors.cards.abstracts.AbstractStatCard;
import SpireSurvivors.entity.AbstractSurvivorPlayer;
import com.megacrit.cardcrawl.cards.green.Accuracy;
import com.megacrit.cardcrawl.cards.green.Finisher;
import com.megacrit.cardcrawl.cards.purple.EmptyBody;

import static SpireSurvivors.SpireSurvivorsMod.makeID;

public class CritDamage extends AbstractStatCard {
    public final static String ID = makeID(CritDamage.class.getSimpleName());
    public CritDamage() {
        super(ID, new Finisher());
    }

    @Override
    public void onPickup(AbstractSurvivorPlayer p) {
        p.critDamage += 0.25f;
    }
}