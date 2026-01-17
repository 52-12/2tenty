package threeadd.glimpsy.util.text.markdown;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import threeadd.glimpsy.util.text.SmallCapsConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownRenderer {

    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("\\*\\*\\*(.*?)\\*\\*\\*");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.*?)__");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("~~(.*?)~~");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)]\\((.*?)\\)");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component render(String markdown) {
        if (markdown == null || markdown.isEmpty()) return Component.empty();

        TextComponent.Builder root = Component.text();
        String[] lines = markdown.split("\n");

        boolean inCodeBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if (inCodeBlock) {
                root.append(MM.deserialize(SmallCapsConverter.toSmallCaps(line))
                        .color(TextColor.color(0xC4C4C4)));
                if (i < lines.length - 1) root.append(Component.newline());
                continue;
            }

            Component lineComponent = parseBlock(line);
            root.append(lineComponent);

            if (i < lines.length - 1) {
                root.append(Component.newline());
            }
        }

        return root.build()
                .color(TextColor.color(0xC4C4C4))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static Component parseBlock(String line) {
        String trimmed = line.trim();
        String convertedLine;
        Component finalComponent;

        if (trimmed.startsWith("# ")) {
            convertedLine = convertInlineMarkdownToMiniMessage(trimmed.substring(2));
            finalComponent = MM.deserialize(convertedLine)
                    .decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED);
            return Component.newline().append(finalComponent).append(Component.newline());
        } else if (trimmed.startsWith("## ")) {
            convertedLine = convertInlineMarkdownToMiniMessage(trimmed.substring(3));
            finalComponent = MM.deserialize(convertedLine)
                    .decorate(TextDecoration.BOLD);
            return Component.newline().append(finalComponent);
        } else if (trimmed.startsWith("### ")) {
            convertedLine = convertInlineMarkdownToMiniMessage(trimmed.substring(4));
            return MM.deserialize(convertedLine).decorate(TextDecoration.BOLD);
        }

        if (trimmed.startsWith("> ")) {
            convertedLine = convertInlineMarkdownToMiniMessage(trimmed.substring(2));
            finalComponent = MM.deserialize(convertedLine).decorate(TextDecoration.ITALIC);
            return Component.text("▎ ").color(TextColor.color(0xC4C4C4))
                    .append(finalComponent);
        }

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            convertedLine = convertInlineMarkdownToMiniMessage(trimmed.substring(2));
            finalComponent = MM.deserialize(convertedLine);
            return Component.text("• ").append(finalComponent);
        }

        convertedLine = convertInlineMarkdownToMiniMessage(line);
        return MM.deserialize(convertedLine);
    }

    private static String convertInlineMarkdownToMiniMessage(String text) {
        Matcher linkMatcher = LINK_PATTERN.matcher(text);
        StringBuilder linkSb = new StringBuilder();
        while (linkMatcher.find()) {
            String linkText = linkMatcher.group(1);
            String url = linkMatcher.group(2);

            String parsedLinkText = convertInlineMarkdownToMiniMessage(linkText);

            String replacement = String.format("<hover:show_text:'Click to open <color:#79ADD6>%s</color>'><click:open_url:%s><color:#79ADD6>%s</color></click></hover>",
                    url.replace("'", "\\'"),
                    url,
                    parsedLinkText
            );
            linkMatcher.appendReplacement(linkSb, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(linkSb);
        text = linkSb.toString();

        text = BOLD_ITALIC_PATTERN.matcher(text).replaceAll(match -> "<b><i>" + Matcher.quoteReplacement(match.group(1)) + "</i></b>");
        text = BOLD_PATTERN.matcher(text).replaceAll(match -> "<b>" + Matcher.quoteReplacement(match.group(1)) + "</b>");
        text = UNDERLINE_PATTERN.matcher(text).replaceAll(match -> "<u>" + Matcher.quoteReplacement(match.group(1)) + "</u>");
        text = STRIKE_PATTERN.matcher(text).replaceAll(match -> "<st>" + Matcher.quoteReplacement(match.group(1)) + "</st>");
        text = ITALIC_PATTERN.matcher(text).replaceAll(match -> "<i>" + Matcher.quoteReplacement(match.group(1)) + "</i>");
        text = CODE_PATTERN.matcher(text).replaceAll(match -> "<gray>" + Matcher.quoteReplacement(match.group(1)) + "</gray>");

        return text;
    }
}