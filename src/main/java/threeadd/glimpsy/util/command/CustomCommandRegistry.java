package threeadd.glimpsy.util.command;

import threeadd.glimpsy.feature.achievement.AchievementsCommand;
import threeadd.glimpsy.feature.competition.view.CompetitionCommand;
import threeadd.glimpsy.feature.competition.view.CompetitionMessagesCommand;
import threeadd.glimpsy.feature.discord.DiscordCommand;
import threeadd.glimpsy.feature.discord.DiscordMessagesCommand;
import threeadd.glimpsy.feature.discordsrv.DiscordChatMuteCommand;
import threeadd.glimpsy.feature.help.HelpCommand;
import threeadd.glimpsy.feature.help.article.ArticleCommand;
import threeadd.glimpsy.feature.help.list.RecipesCommand;
import threeadd.glimpsy.feature.antifilter.AntiDropCommand;
import threeadd.glimpsy.feature.antifilter.AntiPickupCommand;
import threeadd.glimpsy.feature.headcollection.HeadCollectionsCommand;
import threeadd.glimpsy.feature.map.FullResetCommand;
import threeadd.glimpsy.feature.map.MapResetCommand;
import threeadd.glimpsy.feature.map.WhenIsMapResetCommand;
import threeadd.glimpsy.feature.scoreboard.ScoreboardToggleCommand;
import threeadd.glimpsy.feature.timezone.TimezoneCommand;
import threeadd.glimpsy.feature.nickname.ColorNickCommand;
import threeadd.glimpsy.feature.randomitems.RandomItemAlertCommand;
import threeadd.glimpsy.feature.randomitems.RandomItemCommand;
import threeadd.glimpsy.feature.simple.ChatDropoffCommand;
import threeadd.glimpsy.feature.simple.ClearBedSpawnCommand;
import threeadd.glimpsy.feature.simple.FishImmuneCommand;
import threeadd.glimpsy.feature.simple.HatCommand;
import threeadd.glimpsy.feature.simple.RightClickCraftingCommand;
import threeadd.glimpsy.feature.simple.SuicideCommand;
import threeadd.glimpsy.feature.simple.ToggleFishingSoundsCommand;
import threeadd.glimpsy.feature.spawn.WorldSpawnCommand;
import threeadd.glimpsy.feature.stats.PlaytimeCommand;
import threeadd.glimpsy.feature.stats.StatsCommand;
import threeadd.glimpsy.feature.stats.TopCommand;
import threeadd.glimpsy.feature.teams.FriendlyFireAlertCommand;
import threeadd.glimpsy.feature.teams.ShieldSkinCommand;
import threeadd.glimpsy.feature.teams.TeamChatCommand;
import threeadd.glimpsy.feature.teams.TeamsCommand;
import threeadd.glimpsy.feature.wind.WindCommand;
import threeadd.glimpsy.util.config.ConfigReloadCommand;
import threeadd.glimpsy.util.registry.Registry;

public class CustomCommandRegistry extends Registry<CustomCommand> {

    public final static CustomCommandRegistry INSTANCE = new CustomCommandRegistry();

    private CustomCommandRegistry() {
        register(new RandomItemCommand(),
                new RandomItemAlertCommand(),
                new HelpCommand(),
                new CompetitionCommand(),
                new MapResetCommand(),
                new WindCommand(),
                new HatCommand(),
                new RecipesCommand(),
                new SuicideCommand(),
                new CompetitionMessagesCommand(),
                new RightClickCraftingCommand(),
                new DiscordCommand(),
                new DiscordMessagesCommand(),
                new FishImmuneCommand(),
                new DiscordChatMuteCommand(),
                new WorldSpawnCommand(),
                new TeamsCommand(),
                new ShieldSkinCommand(),
                new ColorNickCommand(),
                new TeamChatCommand(),
                new FriendlyFireAlertCommand(),
                new ConfigReloadCommand(),
                new StatsCommand(),
                new TopCommand(),
                new WhenIsMapResetCommand(),
                new ScoreboardToggleCommand(),
                new FullResetCommand(),
                new TimezoneCommand(),
                new AntiDropCommand(),
                new AntiPickupCommand(),
                new PlaytimeCommand(),
                new ChatDropoffCommand(),
                new ArticleCommand(),
                new HeadCollectionsCommand(),
                new AchievementsCommand(),
                new ClearBedSpawnCommand(),
                new ToggleFishingSoundsCommand());
    }
}
