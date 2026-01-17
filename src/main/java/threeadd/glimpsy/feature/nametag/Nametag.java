package threeadd.glimpsy.feature.nametag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.util.hologram.Hologram;

import java.util.Locale;

public class Nametag extends Hologram {

    private final Player player;

    public Nametag(Player player) {
        super(player.getLocation());

        this.player = player;
        spawn();
    }

    private void spawn() {
        addLine(Component.empty());
        mount(player);
    }

    private void mount(Entity entity) {
        if (getLines().isEmpty()) return;

        TextDisplay root = getRoot();
        if (root == null)
            throw new IllegalStateException("Can't mount without root line");

        entity.addPassenger(root);
    }

    public void update() {
        cleanupDeadLines();

        if (getRoot() == null
                || getRoot().getType().isAlive()
                || !player.getPassengers().contains(getRoot())) {
            clearLines();
            spawn();
            return;
        }

        Component health = Component.text(String.format(Locale.US, "%.1f ❤", player.getHealth() / 2))
                .color(TextColor.color(0xFF6A6A));

        if (getLine(1) == null)
            addLine(health);
        else
            getLine(1).setText(health);

        Component name = Component.text(player.getName())
                .color(TextColor.color(0xD4D4D4));

        Component ping = Component.text(player.getPing() + "ms")
                .color(TextColor.color(0xFFE08F));

        getRoot().text(Component.textOfChildren(
                name,
                Component.space(),
                ping
        ));
    }

    private @Nullable TextDisplay getRoot() {
        if (getLines().isEmpty())
            return null;

        return getLines().getFirst().getDisplay();
    }
}
