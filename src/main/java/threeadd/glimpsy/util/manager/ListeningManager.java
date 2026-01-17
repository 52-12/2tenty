package threeadd.glimpsy.util.manager;

import org.bukkit.event.Listener;
import threeadd.glimpsy.util.listener.ListenerRegistry;

public abstract class ListeningManager extends Manager implements Listener {

    @Override
    public final void enable() {
        ListenerRegistry.INSTANCE.register(this); // Register as listener
        onEnable();
    }

    public void onEnable() {
    }
}
