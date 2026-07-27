package threeadd.glimpsy.feature.nickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;

import java.util.regex.Pattern;

/**
 * Validates a raw MiniMessage colored-nickname string. Same rules as team names (colors only,
 * no decorations, ASCII-only whitelist to prevent homograph tricks), plus the nickname's plain
 * text must equal the player's real Mojang username exactly - no text before or after it. This
 * is what stops it from being used to impersonate someone else, or to smuggle extra text (e.g.
 * a fake rank or message) into what's displayed as someone's name - it can only ever be a
 * styled version of your own real name, nothing more.
 */
public class NicknameValidator {

    private static int maxLength() {
        return ConfigManager.getInt("nicknames.max-length", 24);
    }

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
            return Result.fail(Messages.get("nicknames.unparseable", "That nickname couldn't be parsed - check your color tags"));
        }

        if (hasDisallowedDecoration(parsed))
            return Result.fail(Messages.get("nicknames.decorated", "Nicknames can't use bold, italic, underline, strikethrough, or obfuscated text"));

        String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);

        if (plainText.isBlank())
            return Result.fail(Messages.get("nicknames.empty", "Nicknames can't be empty"));

        if (plainText.length() > maxLength())
            return Result.fail(Messages.get("nicknames.too-long", "Nicknames can't be longer than {max} characters",
                    "max", String.valueOf(maxLength())));

        if (!SAFE_CHARACTERS.matcher(plainText).matches())
            return Result.fail(Messages.get("nicknames.bad-characters", "Nicknames can only contain letters, numbers, spaces, - and _"));

        if (!plainText.equals(realUsername))
            return Result.fail(Messages.get("nicknames.must-match-username",
                    "Your nickname must be exactly your real username ({username}), just styled with color tags",
                    "username", realUsername));

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
