package threeadd.glimpsy.util.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HologramBuilder {

    private final Location baseLocation;
    private final List<Component> pendingLines = new ArrayList<>();
    private double spacing = 0.25;
    private Consumer<HologramLine> lineModifier = null;

    public HologramBuilder(Location baseLocation) {
        this.baseLocation = baseLocation.clone();
    }

    public HologramBuilder spacing(double spacing) {
        this.spacing = spacing;
        return this;
    }

    public HologramBuilder line(Component text) {
        pendingLines.add(text);
        return this;
    }

    public HologramBuilder style(Consumer<HologramLine> modifier) {
        this.lineModifier = modifier;
        return this;
    }

    public Hologram build() {
        Hologram holo = new Hologram(baseLocation).withSpacing(spacing);

        for (Component c : pendingLines) {
            HologramLine line = holo.addLine(c);

            if (lineModifier != null) {
                lineModifier.accept(line);
            }
        }

        return holo;
    }
}
