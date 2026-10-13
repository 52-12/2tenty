package threeadd.glimpsy.feature.achievement;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import threeadd.glimpsy.util.config.Messages;

import java.util.Locale;

/**
 * The server's own achievements - one-off things worth noticing, awarded from the feature that
 * makes each of them possible. Deliberately not vanilla advancements: those need a datapack to
 * register, and every one of these hangs off custom behaviour this plugin added anyway.
 * <p>
 * The name in {@code name()} is what gets written to the database, so entries can be reordered or
 * added freely, but renaming one orphans everybody who already earned it.
 */
public enum Achievement {

    A_NEW_WORLD("A New World", "Bonemeal rooted dirt to grass block", Material.GRASS_BLOCK),
    COUNTER_TERRORISTS_WIN("Counter-Terrorists Win", "Extinquish TNT using a water bucket", Material.WATER_BUCKET),
    OH_HELLO_THERE("Oh... Hello there...", "Fish up a random player", Material.FISHING_ROD),
    ENDER_EXPRESS("EnderExpress™", "Make your first trade on the trade chest", Material.ENDER_CHEST),
    WIND_MASTER("Wind Master", "Fish in the wind for a extra Lure Level", Material.FISHING_ROD),
    MIND_THE_GAP("Mind the Gap", "Throw a Bridge Egg", Material.EGG),
    PUBLISHED_AUTHOR("Published Author", "Get a community article approved", Material.WRITTEN_BOOK);

    private final String defaultTitle;
    private final String defaultDescription;
    private final Material icon;

    Achievement(String defaultTitle, String defaultDescription, Material icon) {
        this.defaultTitle = defaultTitle;
        this.defaultDescription = defaultDescription;
        this.icon = icon;
    }

    // achievements.chaos-prevented.title / .description in messages.yml
    private String key(String suffix) {
        return "achievements." + name().toLowerCase(Locale.ROOT).replace('_', '-') + "." + suffix;
    }

    public Component title() {
        return Messages.msg(key("title"), defaultTitle);
    }

    public Component description() {
        return Messages.msg(key("description"), defaultDescription);
    }

    /** Plain title, for the places that need a string rather than a Component. */
    public String defaultTitle() {
        return defaultTitle;
    }

    public Material icon() {
        return icon;
    }
}
