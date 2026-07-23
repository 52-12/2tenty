package threeadd.glimpsy.feature.simple;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("UnstableApiUsage")
public class GuardianFishingListener implements Listener {

    private static final double FISH_GUARDIAN_CHANCE = 0.05; // 5% per successful catch - common
    private static final double ELDER_GUARDIAN_CHANCE = 0.15; // 15% of those catches are an Elder Guardian instead - rarer
    private static final double TRIMMED_ARMOR_DROP_CHANCE = 0.3; // 30% chance on kill

    private static final NamespacedKey FISHED_GUARDIAN_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_guardian");
    private static final NamespacedKey FISHER_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_guardian_owner");

    private static final Material[] ARMOR_PIECES = {
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
    };

    // Mirrors ChargedCreeperFishingListener's anti-spam gate - one fished guardian per player at a time.
    private static final Set<UUID> playersWithActiveGuardian = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        if (playersWithActiveGuardian.contains(player.getUniqueId())) return;

        if (ThreadLocalRandom.current().nextDouble() >= FISH_GUARDIAN_CHANCE) return;

        event.setCancelled(true);

        Location location = event.getHook().getLocation();
        boolean elder = ThreadLocalRandom.current().nextDouble() < ELDER_GUARDIAN_CHANCE;

        LivingEntity guardian = (LivingEntity) location.getWorld().spawnEntity(
                location, elder ? EntityType.ELDER_GUARDIAN : EntityType.GUARDIAN, CreatureSpawnEvent.SpawnReason.CUSTOM);

        guardian.getPersistentDataContainer().set(FISHED_GUARDIAN_KEY, PersistentDataType.BOOLEAN, true);
        guardian.getPersistentDataContainer().set(FISHER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        if (guardian instanceof Mob mob)
            mob.setRemoveWhenFarAway(false); // must be killed, not just abandoned to despawn

        playersWithActiveGuardian.add(player.getUniqueId());

        location.getWorld().playSound(location, Sound.ENTITY_GUARDIAN_AMBIENT, 1.0F, elder ? 0.6F : 1.0F);
        location.getWorld().spawnParticle(Particle.BUBBLE_POP, location, 20, 0.3, 0.3, 0.3);

        player.sendMessage(ComponentParser.parseRichString(
                elder ? "Something ancient stirs beneath the surface..." : "You feel eyes watching from below..."));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        boolean elder = event.getEntity() instanceof ElderGuardian;
        if (!elder && !(event.getEntity() instanceof Guardian)) return;
        if (!event.getEntity().getPersistentDataContainer().has(FISHED_GUARDIAN_KEY, PersistentDataType.BOOLEAN)) return;

        String fisherId = event.getEntity().getPersistentDataContainer().get(FISHER_KEY, PersistentDataType.STRING);
        if (fisherId != null)
            playersWithActiveGuardian.remove(UUID.fromString(fisherId));

        if (ThreadLocalRandom.current().nextDouble() < TRIMMED_ARMOR_DROP_CHANCE)
            event.getDrops().add(createTrimmedArmor());

        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        int durationTicks = elder ? 20 * 60 * 5 : 20 * 60 * 2; // Elder: 5 minutes, regular: 2 minutes
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, durationTicks, elder ? 1 : 0));
    }

    private ItemStack createTrimmedArmor() {
        Material piece = ARMOR_PIECES[ThreadLocalRandom.current().nextInt(ARMOR_PIECES.length)];

        ItemStack armor = new ItemStack(piece);
        ArmorMeta meta = (ArmorMeta) armor.getItemMeta();

        TrimPattern pattern = randomEntry(RegistryKey.TRIM_PATTERN);
        TrimMaterial material = randomEntry(RegistryKey.TRIM_MATERIAL);
        meta.setTrim(new ArmorTrim(material, pattern));

        armor.setItemMeta(meta);
        return armor;
    }

    private <T extends Keyed> T randomEntry(RegistryKey<T> key) {
        Registry<T> registry = RegistryAccess.registryAccess().getRegistry(key);

        List<T> values = new ArrayList<>();
        registry.forEach(values::add);

        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }
}
