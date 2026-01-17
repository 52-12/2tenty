package threeadd.glimpsy.util.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import threeadd.glimpsy.util.ScheduleUtil;

import java.util.ArrayList;
import java.util.List;

public class Hologram {

    private final List<HologramLine> lines = new ArrayList<>();
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final Location baseLocation;

    private double lineSpacing = 0.25;

    public Hologram(Location baseLocation) {
        this.baseLocation = baseLocation.clone();
    }

    public Hologram withSpacing(double spacing) {
        this.lineSpacing = spacing;
        return this;
    }

    public HologramLine addLine(Component text) {
        cleanupDeadLines();

        HologramLine line = new HologramLine(baseLocation);
        line.setText(text);
        line.setLocalOffset(lineSpacing);

        if (!lines.isEmpty()) {
            HologramLine prev = lines.getLast();
            prev.getDisplay().addPassenger(line.getDisplay());
        }

        lines.add(line);
        rebuild();
        return line;
    }

    public HologramLine addTempLine(Component text, long duration, ScheduleUtil.Unit unit) {
        cleanupDeadLines();

        HologramLine line = addLine(text);

        BukkitTask task = ScheduleUtil.scheduleSync(() ->
                        removeLine(lines.indexOf(line)),
                (int) duration,
                unit);

        activeTasks.add(task);
        return line;
    }

    public HologramLine addFadingLine(Component base,
                                      TextColor start,
                                      TextColor end,
                                      long duration,
                                      ScheduleUtil.Unit unit) {

        long totalTicks = unit.toTicks(duration);

        HologramLine line = addLine(base);
        if (totalTicks <= 0) return line;

        final long[] tick = {0};

        BukkitTask fadeTask = ScheduleUtil.scheduleSync(() -> {

            if (!lines.contains(line)) {
                // hologram removed before fade finished
                return;
            }

            if (tick[0] >= totalTicks) {
                removeLine(lines.indexOf(line));
                return;
            }

            float fraction = (float) tick[0] / totalTicks;

            int r = (int) (start.red() + fraction * (end.red() - start.red()));
            int g = (int) (start.green() + fraction * (end.green() - start.green()));
            int b = (int) (start.blue() + fraction * (end.blue() - start.blue()));

            TextColor fade = TextColor.color(r, g, b);
            Component updated = base.color(fade);

            line.setText(updated);

            tick[0]++;

        }, 0, 1, ScheduleUtil.Unit.TICK);

        activeTasks.add(fadeTask);
        return line;
    }

    protected void cleanupDeadLines() {
        boolean dead = lines.removeIf(line ->
                line.getDisplay() == null || !line.getDisplay().isValid());

        if (dead) rebuild();
    }

    public void clearLines() {

        for (HologramLine line : new ArrayList<>(lines)) {
            line.destroy();
            lines.remove(line);

            rebuild();
        }
    }

    private void rebuildOffsets() {
        float currentOffset = 0f;

        for (HologramLine line : lines) {
            currentOffset += line.getLocalOffset();
            line.applyAbsoluteOffset(currentOffset);
        }
    }

    private void rebuildMountChain() {
        if (lines.isEmpty()) return;

        for (HologramLine line : lines) {
            new ArrayList<>(line.getDisplay().getPassengers()).forEach(line.getDisplay()::removePassenger);
        }

        for (int i = 1; i < lines.size(); i++) {
            var prev = lines.get(i - 1).getDisplay();
            var next = lines.get(i).getDisplay();
            prev.addPassenger(next);
        }
    }

    public void rebuild() {
        rebuildOffsets();
        rebuildMountChain();
    }

    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) return;

        lines.get(index).destroy();
        lines.remove(index);

        rebuild();
    }

    public HologramLine getLine(int index) {
        if (index < 0 || index >= lines.size())
            return null;

        return lines.get(index);
    }

    public void remove() {

        for (BukkitTask task : activeTasks) {
            if (task != null) task.cancel();
        }
        activeTasks.clear();

        for (HologramLine line : lines)
            line.destroy();
        lines.clear();
    }

    public List<HologramLine> getLines() {
        cleanupDeadLines();
        return lines;
    }
}
