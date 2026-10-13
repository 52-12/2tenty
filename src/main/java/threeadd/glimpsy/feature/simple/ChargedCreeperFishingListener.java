package threeadd.glimpsy.feature.simple;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.enchantments.CustomEnchantmentRegistry;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ChargedCreeperFishingListener implements Listener {

    private static double chargedCreeperChance() {
        return ConfigManager.getDouble("charged-creeper-fishing.catch-chance", 0.015); // per successful catch
    }

    private static double explosiveBookChance() {
        return ConfigManager.getDouble("charged-creeper-fishing.explosive-book-drop-chance", 0.225); // on kill
    }

    private static final NamespacedKey FISHED_CREEPER_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_charged_creeper");
    private static final NamespacedKey FISHER_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_creeper_owner");

    // A player can only have one fished charged creeper alive at a time - blocks both
    // getting more than one per reel-in and spam-casting for repeat rolls.
    private static final Set<UUID> playersWithActiveCreeper = ConcurrentHashMap.newKeySet();

    // HIGH: last of the three special rolls, and skipped if the guardian (LOW) or player (NORMAL)
    // roll already cancelled the catch - so at most one special outcome fires per reel-in.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        if (playersWithActiveCreeper.contains(player.getUniqueId())) return;

        if (ThreadLocalRandom.current().nextDouble() >= chargedCreeperChance()) return;

        event.setCancelled(true);

        Location location = event.getHook().getLocation();

        Creeper creeper = (Creeper) location.getWorld().spawnEntity(
                location, EntityType.CREEPER, CreatureSpawnEvent.SpawnReason.CUSTOM);
        creeper.setPowered(true);
        creeper.setRemoveWhenFarAway(false); // must be killed, not just abandoned to despawn
        creeper.getPersistentDataContainer().set(FISHED_CREEPER_KEY, PersistentDataType.BOOLEAN, true);
        creeper.getPersistentDataContainer().set(FISHER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

        playersWithActiveCreeper.add(player.getUniqueId());

        location.getWorld().playSound(location, Sound.ENTITY_CREEPER_PRIMED, 1.0F, 1.0F);
        location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 20, 0.3, 0.3, 0.3);

        player.sendMessage(ComponentParser.parseRichString("Something charged bites at your line..."));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!creeper.getPersistentDataContainer().has(FISHED_CREEPER_KEY, PersistentDataType.BOOLEAN)) return;

        String fisherId = creeper.getPersistentDataContainer().get(FISHER_KEY, PersistentDataType.STRING);
        if (fisherId != null)
            playersWithActiveCreeper.remove(UUID.fromString(fisherId));

        if (ThreadLocalRandom.current().nextDouble() >= explosiveBookChance()) return;

        event.getDrops().add(createExplosiveBook());
    }

    private ItemStack createExplosiveBook() {
        int maxLevel = CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.createInfo().maxLevel();
        int level = rollLevel(maxLevel);

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(CustomEnchantmentRegistry.INSTANCE.EXPLOSIVE.toBukkitEnchantment(), level, true);
        book.setItemMeta(meta);

        return book;
    }

    // Weighted so level 1 is most common and each level above it gets linearly rarer
    // (level N has weight maxLevel - N + 1), instead of a flat uniform roll.
    private int rollLevel(int maxLevel) {
        int totalWeight = maxLevel * (maxLevel + 1) / 2;
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        for (int level = 1; level <= maxLevel; level++) {
            int weight = maxLevel - level + 1;
            roll -= weight;
            if (roll < 0) return level;
        }

        throw new IllegalStateException("Couldn't roll an enchant level");
    }
}
