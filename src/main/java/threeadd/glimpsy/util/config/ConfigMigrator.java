package threeadd.glimpsy.util.config;

import threeadd.glimpsy.Glimpsy;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds any keys that exist in the bundled {@code config.yml} but are missing from the server's
 * {@code config.yml}, in place - keeping the existing file's values, comments and order.
 * <p>
 * {@code saveDefaultConfig()} never touches an existing file, so without this, every new config key
 * (and every key we rename) silently falls back to a code default on existing servers. This runs on
 * startup and only ever <b>adds</b> missing keys; it never changes or deletes anything you set.
 */
public final class ConfigMigrator {

    private ConfigMigrator() {
    }

    /** @return true if the file was changed and should be reloaded. */
    public static boolean migrate() {
        Glimpsy plugin = Glimpsy.getInstance();
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) return false; // saveDefaultConfig already wrote the fresh, complete file

        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return false;

            List<String> defaults = readAll(in);
            List<String> user = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

            List<String> merged = merge(defaults, user);
            if (merged.equals(user)) return false;

            Files.write(file.toPath(), merged, StandardCharsets.UTF_8);
            plugin.getLogger().info("Updated config.yml with any new keys (your existing values were kept)");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Couldn't auto-update config.yml: " + e.getMessage());
            return false;
        }
    }

    // --- Merge ------------------------------------------------------------------------------

    private static List<String> merge(List<String> defaults, List<String> user) {
        Map<String, Section> defaultSections = parse(defaults);
        Map<String, Section> userSections = parse(user);

        List<String> out = new ArrayList<>();
        String currentSection = null;

        for (String line : user) {
            String top = topLevelKey(line);
            if (top != null) {
                if (currentSection != null) appendMissingKeys(out, defaultSections.get(currentSection), userSections.get(currentSection));
                currentSection = top;
            }
            out.add(line);
        }
        if (currentSection != null) appendMissingKeys(out, defaultSections.get(currentSection), userSections.get(currentSection));

        // Whole sections present in the default but entirely absent from the user's file.
        for (Map.Entry<String, Section> entry : defaultSections.entrySet()) {
            if (userSections.containsKey(entry.getKey())) continue;

            out.add("");
            out.addAll(entry.getValue().rawLines);
        }

        return out;
    }

    private static void appendMissingKeys(List<String> out, Section defaults, Section user) {
        if (defaults == null || user == null) return;

        for (Map.Entry<String, List<String>> block : defaults.blocks.entrySet()) {
            if (user.keys.contains(block.getKey())) continue;

            out.add("");
            out.addAll(block.getValue());
        }
    }

    // --- Parsing ----------------------------------------------------------------------------

    private static Map<String, Section> parse(List<String> lines) {
        Map<String, Section> sections = new LinkedHashMap<>();
        Section current = null;
        List<String> commentBuffer = new ArrayList<>();
        String currentKey = null;

        for (String line : lines) {
            String top = topLevelKey(line);
            if (top != null) {
                current = new Section();
                current.rawLines.add(line);
                sections.put(top, current);
                commentBuffer.clear();
                currentKey = null;
                continue;
            }

            if (current == null) continue; // header comments before the first section

            current.rawLines.add(line);

            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                commentBuffer.add(line);
                continue;
            }

            if (indentOf(line) == 2 && trimmed.contains(":")) {
                String key = trimmed.split(":", 2)[0].strip();
                List<String> block = new ArrayList<>(commentBuffer);
                block.add(line);
                current.blocks.put(key, block);
                current.keys.add(key);
                currentKey = key;
                commentBuffer.clear();
            } else if (currentKey != null) {
                // continuation of the last key (a list item or nested value)
                current.blocks.get(currentKey).add(line);
                commentBuffer.clear();
            }
        }

        return sections;
    }

    private static String topLevelKey(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || indentOf(line) != 0) return null;
        if (!trimmed.contains(":")) return null;
        return trimmed.split(":", 2)[0].strip();
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static List<String> readAll(InputStream in) throws Exception {
        List<String> lines = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    /** One top-level section: its raw lines, and a comment+value block per key. */
    private static final class Section {
        final List<String> rawLines = new ArrayList<>();
        final Map<String, List<String>> blocks = new LinkedHashMap<>();
        final Set<String> keys = new LinkedHashSet<>();
    }
}
