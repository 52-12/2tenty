package threeadd.glimpsy.feature.help.article;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import threeadd.glimpsy.util.ScheduleUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ArticleLoader {

    private static final Logger log = LoggerFactory.getLogger(ArticleLoader.class);

    public static List<Article> loadArticlesSync(File folder) {
        File[] files = folder.listFiles((dir, name) ->
                name.endsWith(".yaml") || name.endsWith(".yml"));

        List<Article> articles = new ArrayList<>();
        if (files == null)
            return articles;

        Yaml yaml = new Yaml();

        for (File file : files) {
            try (InputStream input = new FileInputStream(file)) {
                Map<String, Object> data = yaml.load(input);
                if (data == null || !data.containsKey("article")) {
                    log.error("File missing article: {}", file.getName());
                    continue;
                }

                Object articleObj = data.get("article");
                if (!(articleObj instanceof Map<?, ?> rawMap)) {
                    log.error("Invalid article format: {}", file.getName());
                    continue;
                }

                Map<String, Object> articleMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (!(entry.getKey() instanceof String))
                        throw new IllegalArgumentException("Map key must be a string in " + file.getName());
                    articleMap.put((String) entry.getKey(), entry.getValue());
                }

                Article article = Article.deserialize(articleMap);
                articles.add(article);
            } catch (IOException e) {
                log.error("Something went wrong loading article", e);
            }
        }

        return articles;
    }


    public static CompletableFuture<List<Article>> loadArticlesAsync(File folder) {
        CompletableFuture<List<Article>> future = new CompletableFuture<>();

        ScheduleUtil.scheduleAsync(() -> {
            try {
                List<Article> articles = loadArticlesSync(folder);

                ScheduleUtil.scheduleSync(() ->
                        future.complete(articles));

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}
