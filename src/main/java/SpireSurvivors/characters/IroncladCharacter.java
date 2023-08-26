package SpireSurvivors.characters;

import SpireSurvivors.entity.AbstractSurvivorPlayer;
import SpireSurvivors.weapons.BashWeapon;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

public class IroncladCharacter extends AbstractSurvivorPlayer {
    public IroncladCharacter(AbstractPlayer p) {
        super(p);
        weapons.add(new BashWeapon());
    }
}
