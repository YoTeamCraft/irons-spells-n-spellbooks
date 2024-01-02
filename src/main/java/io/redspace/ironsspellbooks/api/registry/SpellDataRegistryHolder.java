package io.redspace.ironsspellbooks.api.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import net.minecraftforge.registries.RegistryObject;

public class SpellDataRegistryHolder {

    RegistryObject<AbstractSpell> registrySpell;
    int spellLevel;

    public SpellDataRegistryHolder(RegistryObject<AbstractSpell> registrySpell, int spellLevel) {
        this.registrySpell = registrySpell;
        this.spellLevel = spellLevel;
    }

    public SpellSlot getSpellSlot() {
        return new SpellSlot(registrySpell.get(), spellLevel);
    }
}
