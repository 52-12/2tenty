package threeadd.glimpsy.feature.teams;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Parses a /shieldskin design typed as plain words - a base color, then any number of
 * "&lt;pattern&gt; &lt;color&gt;" pairs layered on top, e.g. "red cross white border black".
 * No item required, no GUI - just vanilla-style typed arguments.
 */
public class ShieldSkinDesignParser {

    public record Result(boolean valid, String errorMessage, List<Pattern> patterns) {
        static Result ok(List<Pattern> patterns) {
            return new Result(true, null, patterns);
        }

        static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    public static Result parse(String raw) {
        String[] tokens = raw.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank())
            return Result.fail("Specify a base color, e.g. /shieldskin red");

        DyeColor base = parseColor(tokens[0]);
        if (base == null)
            return Result.fail("Unknown color '" + tokens[0] + "' - run /shieldskin colors to see valid options");

        if ((tokens.length - 1) % 2 != 0)
            return Result.fail("Patterns must be given in pairs of <pattern> <color>, e.g. /shieldskin red cross white");

        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(base, PatternType.BASE));

        for (int i = 1; i < tokens.length; i += 2) {
            PatternType type = parsePatternType(tokens[i]);
            if (type == null)
                return Result.fail("Unknown pattern '" + tokens[i] + "' - run /shieldskin patterns to see valid options");

            DyeColor color = parseColor(tokens[i + 1]);
            if (color == null)
                return Result.fail("Unknown color '" + tokens[i + 1] + "' - run /shieldskin colors to see valid options");

            patterns.add(new Pattern(color, type));
        }

        return Result.ok(patterns);
    }

    public static @Nullable DyeColor parseColor(String word) {
        try {
            return DyeColor.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // "base" is synthesized from the base color argument, not selectable as its own layer.
    public static @Nullable PatternType parsePatternType(String word) {
        if (word.equalsIgnoreCase("base")) return null;
        return bannerPatternRegistry().get(NamespacedKey.minecraft(word.toLowerCase(Locale.ROOT)));
    }

    public static Registry<PatternType> bannerPatternRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
    }

    public static String listColors() {
        return Arrays.stream(DyeColor.values())
                .map(color -> color.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }

    public static String listPatterns() {
        List<String> names = new ArrayList<>();
        Registry<PatternType> registry = bannerPatternRegistry();
        for (PatternType type : registry) {
            String key = registry.getKeyOrThrow(type).getKey();
            if (!key.equals("base")) names.add(key);
        }
        return String.join(", ", names);
    }
}
