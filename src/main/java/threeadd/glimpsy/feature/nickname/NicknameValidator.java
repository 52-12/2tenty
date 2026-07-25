package threeadd.glimpsy.feature.nickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * Validates a raw MiniMessage colored-nickname string. Same rules as team names (colors only,
 * no decorations, ASCII-only whitelist to prevent homograph tricks), plus the nickname must
 * contain the player's real Mojang username as an exact substring - this is what stops it from
 * being used to impersonate someone else entirely, it can only ever be a styled version of your
 * own real name.
 */
public class NicknameValidator {

    public static final int MAX_LENGTH = 24;

    private static final Pattern SAFE_CHARACTERS = Pattern.compile("^[a-zA-Z0-9 _-]+$");

    private static final TextDecoration[] DISALLOWED_DECORATIONS = {
            TextDecoration.BOLD,
            TextDecoration.ITALIC,
            TextDecoration.UNDERLINED,
            TextDecoration.STRIKETHROUGH,
            TextDecoration.OBFUSCATED
    };

    public record Result(boolean valid, String errorMessage, Component parsed) {

        public static Result ok(Component parsed) {
            return new Result(true, null, parsed);
        }

        public static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    public static Result validate(String rawInput, String realUsername) {
        Component parsed;
        try {
            parsed = MiniMessage.miniMessage().deserialize(rawInput);
        } catch (Exception e) {
            return Result.fail("That nickname couldn't be parsed - check your color tags");
        }

        if (hasDisallowedDecoration(parsed))
            return Result.fail("Nicknames can't use bold, italic, underline, strikethrough, or obfuscated text");

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);

        if (plainText.isBlank())
            return Result.fail("Nicknames can't be empty");

        if (plainText.length() > MAX_LENGTH)
            return Result.fail("Nicknames can't be longer than " + MAX_LENGTH + " characters");

        if (!SAFE_CHARACTERS.matcher(plainText).matches())
            return Result.fail("Nicknames can only contain letters, numbers, spaces, - and _");

        if (!plainText.contains(realUsername))
            return Result.fail("Your nickname must contain your real username (" + realUsername + ")");

        return Result.ok(parsed);
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
