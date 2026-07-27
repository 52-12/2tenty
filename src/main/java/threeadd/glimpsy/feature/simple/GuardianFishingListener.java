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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("UnstableApiUsage")
public class GuardianFishingListener implements Listener {

    private static double fishGuardianChance() {
        return ConfigManager.getDouble("guardian-fishing.fish-guardian-chance", 0.05); // per successful catch - common
    }

    private static double elderGuardianChance() {
        return ConfigManager.getDouble("guardian-fishing.elder-guardian-chance", 0.15); // of those catches, rarer
    }

    private static double trimTemplateDropChance() {
        return ConfigManager.getDouble("guardian-fishing.trim-template-drop-chance", 0.3); // on kill
    }

    private static double trimmedArmorPieceDropChance() {
        return ConfigManager.getDouble("guardian-fishing.trimmed-armor-piece-drop-chance", 0.02); // on kill, mutually exclusive with the template drop
    }

    private static int elderLuckDurationTicks() {
        return ConfigManager.getInt("guardian-fishing.elder-luck-duration-seconds", 300) * 20; // 5 minutes
    }

    private static int regularLuckDurationTicks() {
        return ConfigManager.getInt("guardian-fishing.regular-luck-duration-seconds", 120) * 20; // 2 minutes
    }

    private static double damagedArmorMaxDurabilityFraction() {
        return ConfigManager.getDouble("guardian-fishing.damaged-armor-max-durability-fraction", 0.5);
    }

    private static final NamespacedKey FISHED_GUARDIAN_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_guardian");
    private static final NamespacedKey FISHER_KEY = new NamespacedKey(Glimpsy.getInstance(), "fished_guardian_owner");

    // Defaults; the live pools are read from config (see armorPieces() / trimTemplates()) so
    // they can be customised in config.yml.
    private static final Material[] LEATHER_OR_IRON_ARMOR_PIECES_DEFAULT = {
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
    };

    private static Material[] armorPieces() {
        return ConfigManager.getMaterialList("guardian-fishing.armor-pieces", LEATHER_OR_IRON_ARMOR_PIECES_DEFAULT);
    }

    private static Material[] trimTemplates() {
        return ConfigManager.getMaterialList("guardian-fishing.trim-templates", TRIM_SMITHING_TEMPLATES_DEFAULT);
    }

    // All 18 armor trim smithing templates - Elder Guardian exclusive drop.
    private static final Material[] TRIM_SMITHING_TEMPLATES_DEFAULT = {
            Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    // Mirrors ChargedCreeperFishingListener's anti-spam gate - one fished guardian per player at a time.
    private static final Set<UUID> playersWithActiveGuardian = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        if (playersWithActiveGuardian.contains(player.getUniqueId())) return;

        if (ThreadLocalRandom.current().nextDouble() >= fishGuardianChance()) return;

        event.setCancelled(true);

        Location location = event.getHook().getLocation();
        boolean elder = ThreadLocalRandom.current().nextDouble() < elderGuardianChance();

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

        if (elder) {
            // Mutually exclusive - a kill either yields the rare full trimmed piece, or (much
            // more often) just a template, never both.
            if (ThreadLocalRandom.current().nextDouble() < trimmedArmorPieceDropChance())
                event.getDrops().add(createDamagedTrimmedArmor());
            else if (ThreadLocalRandom.current().nextDouble() < trimTemplateDropChance())
                event.getDrops().add(createRandomTrimTemplate());
        }

        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        int durationTicks = elder ? elderLuckDurationTicks() : regularLuckDurationTicks();
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, durationTicks, elder ? 1 : 0));
    }

    private ItemStack createRandomTrimTemplate() {
        Material[] templates = trimTemplates();
        Material template = templates[ThreadLocalRandom.current().nextInt(templates.length)];
        return new ItemStack(template);
    }

    // Base piece is leather or iron, but the trim material applied to it can be anything -
    // trims aren't restricted to matching the base armor material. Comes pre-worn, below 50%
    // durability remaining, like it's been sitting in a guardian's lair for a while.
    private ItemStack createDamagedTrimmedArmor() {
        Material[] pieces = armorPieces();
        Material piece = pieces[ThreadLocalRandom.current().nextInt(pieces.length)];

        ItemStack armor = new ItemStack(piece);
        ItemMeta meta = armor.getItemMeta();

        if (meta instanceof ArmorMeta armorMeta) {
            TrimPattern pattern = randomEntry(RegistryKey.TRIM_PATTERN);
            TrimMaterial material = randomEntry(RegistryKey.TRIM_MATERIAL);
            armorMeta.setTrim(new ArmorTrim(material, pattern));
        }

        if (meta instanceof Damageable damageable) {
            int maxDurability = piece.getMaxDurability();
            int minDamage = (int) (maxDurability * damagedArmorMaxDurabilityFraction()) + 1; // strictly below that fraction of durability remaining
            int damage = minDamage + ThreadLocalRandom.current().nextInt(maxDurability - minDamage + 1);
            damageable.setDamage(damage);
        }

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
