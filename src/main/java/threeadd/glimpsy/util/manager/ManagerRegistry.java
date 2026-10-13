package threeadd.glimpsy.util.manager;

import threeadd.glimpsy.feature.achievement.AchievementManager;
import threeadd.glimpsy.feature.antifilter.AntiFilterManager;
import threeadd.glimpsy.feature.competition.CompetitionManager;
import threeadd.glimpsy.feature.help.article.ArticleOrderManager;
import threeadd.glimpsy.feature.help.article.PlayerArticleManager;
import threeadd.glimpsy.feature.simple.BambooGrowthManager;
import threeadd.glimpsy.feature.simple.SaltBlockManager;
import threeadd.glimpsy.feature.discord.DiscordManager;
import threeadd.glimpsy.feature.discordsrv.DiscordChatMuteManager;
import threeadd.glimpsy.feature.headcollection.HeadCollectionManager;
import threeadd.glimpsy.feature.map.MapResetManager;
import threeadd.glimpsy.feature.nametag.NametagManager;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.randomitems.RandomItemManager;
import threeadd.glimpsy.feature.simple.TabManager;
import threeadd.glimpsy.feature.scoreboard.ScoreboardManager;
import threeadd.glimpsy.feature.spawn.WorldSpawnManager;
import threeadd.glimpsy.feature.stats.StatsManager;
import threeadd.glimpsy.feature.teams.ShieldSkinManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.feature.timezone.TimezoneManager;
import threeadd.glimpsy.feature.tradechest.TradeChestManager;
import threeadd.glimpsy.feature.wind.WindManager;
import threeadd.glimpsy.util.registry.Registry;

public class ManagerRegistry extends Registry<Manager> {

    public final static ManagerRegistry INSTANCE = new ManagerRegistry();

    private ManagerRegistry() {
        register(new RandomItemManager(),
                new CompetitionManager(),
                new MapResetManager(),
                new NametagManager(),
                new WindManager(),
                new TabManager(),
                new DiscordManager(),
                new DiscordChatMuteManager(),
                new WorldSpawnManager(),
                new TeamManager(),
                new ShieldSkinManager(),
                new NicknameManager(),
                new StatsManager(),
                new TradeChestManager(),
                new ScoreboardManager(),
                new TimezoneManager(),
                new AntiFilterManager(),
                new PlayerArticleManager(),
                new ArticleOrderManager(),
                new BambooGrowthManager(),
                new SaltBlockManager(),
                new HeadCollectionManager(),
                new AchievementManager());
    }
}
