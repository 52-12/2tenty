package threeadd.glimpsy.feature.wind;

import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public enum Wind {
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST;

    private static final String[] ARROWS = {"⬆", "⬈", "➡", "⬊", "⬇", "⬋", "⬅", "⬉"};

    private static int yawToIndex(float yaw) {
        yaw = (yaw % 360 + 360) % 360;

        return Math.round((yaw + 180) / 45f) % 8;
    }

    public static Wind getRandom() {
        Wind[] values = Wind.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    public String getRelativeArrow(Player player) {
        float yaw = player.getYaw();

        int wind = this.ordinal();
        int facing = yawToIndex(yaw);

        int relative = ((wind - facing) + 8) % 8;
        return ARROWS[relative];
    }

    public boolean isInWind(Player player) {
        return getRelativeArrow(player).equals("⬆");
    }
}
