package threeadd.glimpsy.util.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class HologramLine {

    private final TextDisplay display;

    private float localOffset = 0.2f;

    public HologramLine(Location location) {
        display = location.getWorld().spawn(location, TextDisplay.class);

        display.setBackgroundColor(Color.fromARGB(0)); // transparent
        display.setShadowed(true);
        display.setBillboard(Display.Billboard.CENTER);
    }

    public HologramLine setText(Component text) {
        display.text(text);
        return this;
    }

    public HologramLine setScale(Vector3f newScale) {
        Transformation t = getDisplay().getTransformation();

        Transformation newTransformation = new Transformation(
                t.getTranslation(),
                t.getLeftRotation(),
                newScale,
                t.getRightRotation()
        );

        getDisplay().setTransformation(newTransformation);
        return this;
    }

    protected void applyAbsoluteOffset(float absoluteOffset) {
        Transformation t = display.getTransformation();

        Transformation newT = new Transformation(
                new Vector3f(0, absoluteOffset, 0),
                t.getLeftRotation(),
                t.getScale(),
                t.getRightRotation()
        );

        display.setTransformation(newT);
    }

    public float getLocalOffset() {
        return localOffset;
    }

    public HologramLine setLocalOffset(double offset) {
        this.localOffset = (float) offset;
        return this;
    }

    public TextDisplay getDisplay() {
        return display;
    }

    public void destroy() {
        display.remove();
    }
}
