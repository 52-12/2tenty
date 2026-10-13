package threeadd.glimpsy.util.enchantments;

import threeadd.glimpsy.feature.explosive.ExplosiveEnchantment;
import threeadd.glimpsy.feature.telekinesis.TelekinesisEnchantment;
import threeadd.glimpsy.util.registry.Registry;

public class CustomEnchantmentRegistry extends Registry<CustomEnchantment> {

    public final static CustomEnchantmentRegistry INSTANCE = new CustomEnchantmentRegistry();

    public final CustomEnchantment EXPLOSIVE;
    public final CustomEnchantment TELEKINESIS;

    private CustomEnchantmentRegistry() {
        this.EXPLOSIVE = register(new ExplosiveEnchantment());
        this.TELEKINESIS = register(new TelekinesisEnchantment());
    }
}
