package threeadd.glimpsy.util.listener;

import org.bukkit.event.Listener;
import threeadd.glimpsy.feature.entity.GuardianListener;
import threeadd.glimpsy.feature.entity.PiglinConvertListener;
import threeadd.glimpsy.feature.explosive.ExplosiveListener;
import threeadd.glimpsy.feature.help.article.ArticleEditListener;
import threeadd.glimpsy.feature.nametag.ChatAboveHeadListener;
import threeadd.glimpsy.feature.powerskull.PiglinSkullListener;
import threeadd.glimpsy.feature.powerskull.SkeletonSkullListener;
import threeadd.glimpsy.feature.powerskull.ZombieSkullListener;
import threeadd.glimpsy.feature.telekinesis.TelekinesisListener;
import threeadd.glimpsy.feature.simple.*;
import threeadd.glimpsy.feature.wind.WindFishingListener;
import threeadd.glimpsy.feature.teams.TeamChatListener;
import threeadd.glimpsy.util.hologram.HologramVisibilityListener;
import threeadd.glimpsy.util.inventory.CustomInventoryListener;
import threeadd.glimpsy.util.registry.Registry;

public class ListenerRegistry extends Registry<Listener> {

    public static final ListenerRegistry INSTANCE = new ListenerRegistry();

    private ListenerRegistry() {
        register(
                new CustomInventoryListener(),
                new ArticleEditListener(),
                new TNTTrailListener(),
                new ExplosiveListener(),
                new LuckBottleListener(),
                new ExpensiveAnvilListener(),
                new ChatAboveHeadListener(),
                new WindFishingListener(),
                new ChatListener(),
                new InteractCraftingListener(),
                new BloodListener(),
                new ZombieSkullListener(),
                new SkeletonSkullListener(),
                new PiglinSkullListener(),
                new GuardianListener(),
                new PiglinConvertListener(),
                new NautilusShellListener(),
                new LightningRodListener(),
                new EnderPortalDebuffListener(),
                new DirtMossListener(),
                new MudClayListener(),
                new SuspiciousSandListener(),
                new SuspiciousGravelListener(),
                new SaddleRideListener(),
                new ChatShareListener(),
                new SuicideDeathListener(),
                new BedrockBoatListener(),
                new ChargedCreeperFishingListener(),
                new GuardianFishingListener(),
                new PlayerFishingListener(),
                new VillagerBreedListener(),
                new SaltListener(),
                new MyceliumSilkTouchListener(),
                new VoidInstakillListener(),
                new SuspiciousSandFallListener(),
                new VillagerTradeListener(),
                new CreeperSpawnListener(),
                new DeepslateExplosionListener(),
                new BridgeEggListener(),
                new FishingBobberTracker(),
                new TNTExplosionRadiusListener(),
                new TNTExtinguishListener(),
                new TridentDropListener(),
                new SeagrassDropListener(),
                new MendingListener(),
                new HologramVisibilityListener(),
                new BedOwnershipListener(),
                new HeadDropListener(),
                new TeamChatListener(),
                new TelekinesisListener()
        );
    }
}
