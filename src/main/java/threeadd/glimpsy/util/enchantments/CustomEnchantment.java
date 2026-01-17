package threeadd.glimpsy.util.enchantments;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import threeadd.glimpsy.util.Documentable;

import java.util.List;

public abstract class CustomEnchantment implements Documentable {

    public abstract Key createKey();

    public abstract Component createDescription();

    public abstract CustomEnchantmentInfo createInfo();

    public abstract List<Enchantment> createIncompatibleEnchantments();

    public abstract List<TypedKey<ItemType>> createSupportedItems();

    public abstract EquipmentSlotGroup createActiveSlots();

    public abstract List<TagKey<Enchantment>> createTags();

    public @NotNull Enchantment toBukkitEnchantment() {
        Registry<@NotNull Enchantment> enchantmentRegistry = RegistryAccess
                .registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        Enchantment bukkitEnchantment = enchantmentRegistry.get(createKey());
        if (bukkitEnchantment == null)
            throw new IllegalStateException("Couldn't find registered enchantment for " + createKey());

        return bukkitEnchantment;
    }
}
