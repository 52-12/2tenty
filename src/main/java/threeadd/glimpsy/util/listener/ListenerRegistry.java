package threeadd.glimpsy.util.listener;

import org.bukkit.event.Listener;
import threeadd.glimpsy.feature.explosive.ExplosiveListener;
import threeadd.glimpsy.feature.nametag.ChatAboveHeadListener;
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
                new BloodListener()
        );
    }
}
