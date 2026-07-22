package threeadd.glimpsy.util.listener;

import org.bukkit.event.Listener;
import threeadd.glimpsy.feature.entity.GuardianListener;
import threeadd.glimpsy.feature.entity.PiglinConvertListener;
import threeadd.glimpsy.feature.explosive.ExplosiveListener;
import threeadd.glimpsy.feature.nametag.ChatAboveHeadListener;
import threeadd.glimpsy.feature.powerskull.PiglinSkullListener;
import threeadd.glimpsy.feature.powerskull.SkeletonSkullListener;
import threeadd.glimpsy.feature.powerskull.ZombieSkullListener;
import threeadd.glimpsy.feature.simple.*;
import threeadd.glimpsy.feature.vote.VoteListener;
import threeadd.glimpsy.feature.wind.WindFishingListener;
import threeadd.glimpsy.util.inventory.CustomInventoryListener;
import threeadd.glimpsy.util.registry.Registry;

public class ListenerRegistry extends Registry<Listener> {

    public static final ListenerRegistry INSTANCE = new ListenerRegistry();

    private ListenerRegistry() {
        register(
                new CustomInventoryListener(),
                new TNTTrailListener(),
                new ExplosiveListener(),
                new LuckBottleListener(),
                new ExpensiveAnvilListener(),
                new ChatAboveHeadListener(),
                new WindFishingListener(),
                new ChatListener(),
                new InteractCraftingListener(),
                new VoteListener(),
                new BloodListener(),
                new ZombieSkullListener(),
                new SkeletonSkullListener(),
                new PiglinSkullListener(),
                new GuardianListener(),
                new PiglinConvertListener(),
                new CorianRootListener(),
                new NautilusShellListener(),
                new LightningRodListener(),
                new EnderPortalDebuffListener(),
                new DirtMossListener(),
                new MudClayListener(),
                new SuspiciousSandListener(),
                new SuspiciousGravelListener(),
                new LavaCauldronListener(),
                new SaddleRideListener(),
                new ChatShareListener(),
                new SuicideDeathListener()
        );
    }
}
