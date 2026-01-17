package threeadd.glimpsy.feature.help.article;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.Glimpsy;
import threeadd.glimpsy.util.ScheduleUtil;
import threeadd.glimpsy.util.registry.Registry;

import java.io.File;

public class ArticleRegistry extends Registry<Article> {

    public static final ArticleRegistry INSTANCE = new ArticleRegistry();
    private static final Logger log = LoggerFactory.getLogger(ArticleRegistry.class);

    private ArticleRegistry() {
        File articlesFolder = new File(Glimpsy.getInstance().getDataFolder(), "articles");
        if (!articlesFolder.exists())
            if (!articlesFolder.mkdirs())
                throw new IllegalStateException("Couldn't create articles folder");

        ArticleLoader.loadArticlesAsync(articlesFolder)
                .thenAccept(articles ->
                        ScheduleUtil.scheduleSync(() -> {
                            register(articles);
                            log.info("Registered {} articles", articles.size());
                        })
                );
    }
}
