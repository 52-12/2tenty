package threeadd.glimpsy.feature.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * Validates a raw MiniMessage team name string. Color tags are allowed, but text decorations
 * (bold/italic/underlined/strikethrough/obfuscated) are not. The visible text is restricted to
 * a plain ASCII whitelist - this is what actually prevents homograph attacks (Cyrillic/Greek
 * lookalike characters, zero-width characters, combining marks, etc.), since a whitelist can't
 * be tricked the way a blacklist of "known bad" characters could.
 */
public class TeamNameValidator {

    public static final int MAX_LENGTH = 12;

    private static final Pattern SAFE_CHARACTERS = Pattern.compile("^[a-zA-Z0-9 _-]+$");

    private static final TextDecoration[] DISALLOWED_DECORATIONS = {
            TextDecoration.BOLD,
            TextDecoration.ITALIC,
            TextDecoration.UNDERLINED,
            TextDecoration.STRIKETHROUGH,
            TextDecoration.OBFUSCATED
    };

    public record Result(boolean valid, String errorMessage, Component parsed, String plainText) {

        public static Result ok(Component parsed, String plainText) {
            return new Result(true, null, parsed, plainText);
        }

        public static Result fail(String message) {
            return new Result(false, message, null, null);
        }
    }

    public static Result validate(String rawInput) {
        Component parsed;
        try {
            parsed = MiniMessage.miniMessage().deserialize(rawInput);
        } catch (Exception e) {
            return Result.fail("That name couldn't be parsed - check your color tags");
        }

        if (hasDisallowedDecoration(parsed))
            return Result.fail("Team names can't use bold, italic, underline, strikethrough, or obfuscated text");

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);

        if (plainText.isBlank())
            return Result.fail("Team names can't be empty");

        if (plainText.length() > MAX_LENGTH)
            return Result.fail("Team names can't be longer than " + MAX_LENGTH + " characters");

        if (!SAFE_CHARACTERS.matcher(plainText).matches())
            return Result.fail("Team names can only contain letters, numbers, spaces, - and _");

        return Result.ok(parsed, plainText);
    }

    private static boolean hasDisallowedDecoration(Component component) {
        for (TextDecoration decoration : DISALLOWED_DECORATIONS) {
            if (component.decoration(decoration) == TextDecoration.State.TRUE) return true;
        }

        for (Component child : component.children()) {
            if (hasDisallowedDecoration(child)) return true;
        }

        return false;
    }
}
