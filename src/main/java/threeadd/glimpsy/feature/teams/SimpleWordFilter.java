package threeadd.glimpsy.feature.teams;

import java.util.Set;

/**
 * A minimal local profanity check for team names. MinehutFilter's actual filtering only works
 * through a localhost service Minehut's own hosting provides (see FilterAPI in
 * MinehutFilter.jar - it posts to http://127.0.0.1:25576), which isn't present off Minehut's
 * infrastructure (e.g. this local dev server), and there's no local wordlist bundled in the jar
 * to reuse - it's all server-side. This is a simple, environment-independent baseline instead.
 *
 * @apiNote Expand BLOCKED as needed - this is intentionally a short baseline list, not an
 * attempt at a comprehensive filter.
 */
public class SimpleWordFilter {

    private static final Set<String> BLOCKED = Set.of(
            "fuck", "shit", "bitch", "cunt", "nigger", "nigga", "faggot", "retard", "rape"
    );

    public static boolean isAppropriate(String plainText) {
        String normalized = plainText.toLowerCase().replaceAll("[^a-z]", "");

        for (String blocked : BLOCKED) {
            if (normalized.contains(blocked)) return false;
        }

        return true;
    }
}
