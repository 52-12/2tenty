package threeadd.glimpsy.feature.help.list;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import threeadd.glimpsy.feature.help.HelpListInventory;
import threeadd.glimpsy.feature.help.article.Article;
import threeadd.glimpsy.feature.help.article.ArticleRegistry;
import threeadd.glimpsy.util.inventory.ItemBuilder;
import threeadd.glimpsy.util.inventory.ListInventory;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.function.Consumer;

public class ArticleListInventory extends ListInventory<Article> {

    public ArticleListInventory(Player viewer) {
        super(Rows.SIX, Component.text("Help Articles"), ArticleRegistry.INSTANCE.getRegisteredItems(), Rows.SIX.getInnerSlots(), viewer);

        setTransferSlot(Rows.SIX.getTotalSlots() - 9,
                new ItemBuilder(Material.RED_DYE)
                        .withName(Component.text("Return")
                                .color(TextColor.color(0xDB5858)))
                        .build(),
                () -> new HelpListInventory(getViewer()));
    }

    @Override
    protected @NotNull ItemStack mapToItem(@NotNull Article article) {
        return new ItemBuilder(Material.PAPER)
                .withName(article.title()
                        .color(TextColor.color(0xFF3B3B))
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                )
                .withLore(
                        ComponentParser.parseRichString("<#D75A5A>Authors:</#D75A5A> " + String.join("<#C4C4C4>,</#C4C4C4> ", article.authors())),
                        ComponentParser.parseRichString("<#D75A5A>Creation Date:</#D75A5A> " + article.creationDate()),
                        Component.empty(),
                        article.content()
                )
                .build();
    }

    @Override
    protected @Nullable Consumer<InventoryClickEvent> mapClick(@NotNull Article article) {
        return event -> event.getView().getPlayer().sendMessage(article.content());
    }
}
