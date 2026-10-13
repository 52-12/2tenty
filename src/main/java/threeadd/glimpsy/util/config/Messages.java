package threeadd.glimpsy.util.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.text.ComponentParser;

import java.io.File;

/**
 * Thin wrapper around messages.yml for user-facing text and branding colors. Defaults live at
 * every call site, so a missing key (or a missing file) never breaks anything - the yml only
 * needs entries for what an admin wants to override.
 * <p>
 * Messages support {placeholder} substitution and the branding color tags below - e.g.
 * {@code <primary>Teams:</primary>} - which resolve to whatever colors the branding section
 * sets before MiniMessage parsing.
 */
public class Messages {

    private static YamlConfiguration messages = new YamlConfiguration();

    // tag name -> branding key + default color (any MiniMessage color: named or #hex)
    private static final String[][] BRAND_TAGS = {
            {"primary", "branding.primary", "#DBC258"},
            {"alert", "branding.alert", "#DB5858"},
            {"success", "branding.success", "green"},
            {"error", "branding.error", "red"},
            {"muted", "branding.muted", "gray"},
            {"highlight", "branding.highlight", "yellow"},
            {"ally", "branding.ally", "aqua"},
            {"title", "branding.title", "#2881C6"}, // the "2tenty" logo colour - tab list + scoreboard
    };

    public static void load() {
        File file = new File(Glimpsy.getInstance().getDataFolder(), "messages.yml");
        if (!file.exists()) Glimpsy.getInstance().saveResource("messages.yml", false);

        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static void reload() {
        load();
    }

    /**
     * Raw branded string - placeholder pairs are (name, value) varargs, so
     * {@code get("teams.renamed", "Your team is now: {name}", "name", rawName)}.
     */
    public static String get(String key, String def, String... placeholders) {
        String value = messages.getString(key, def);

        for (int i = 0; i + 1 < placeholders.length; i += 2)
            value = value.replace("{" + placeholders[i] + "}", placeholders[i + 1]);

        return brand(value);
    }

    /** Same as {@link #get} but parsed into a Component. */
    public static Component msg(String key, String def, String... placeholders) {
        return ComponentParser.parseRichString(get(key, def, placeholders));
    }

    /**
     * A GUI title in solid black. parseRichString's light-gray fallback color lands on a child
     * component, so coloring the parsed component's root doesn't take - strip to plain text
     * and rebuild instead (same approach as RecipeInfoInventory's title).
     */
    public static Component blackTitle(String key, String def, String... placeholders) {
        String plain = PlainTextComponentSerializer.plainText().serialize(msg(key, def, placeholders));
        return Component.text(plain, NamedTextColor.BLACK);
    }

    /** The plugin's display name shown to players - branding.name in messages.yml. */
    public static String brandName() {
        return messages.getString("branding.name", "2tenty");
    }

    /** Resolves branding color tags in any string (already applied by get/msg). */
    public static String brand(String value) {
        for (String[] tag : BRAND_TAGS) {
            String color = messages.getString(tag[1], tag[2]);
            value = value.replace("<" + tag[0] + ">", "<" + color + ">")
                    .replace("</" + tag[0] + ">", "</" + color + ">");
        }
        return value;
    }
}
