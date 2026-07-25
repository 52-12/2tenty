package threeadd.glimpsy.util.command;

import threeadd.glimpsy.feature.competition.view.CompetitionCommand;
import threeadd.glimpsy.feature.competition.view.CompetitionMessagesCommand;
import threeadd.glimpsy.feature.discord.DiscordCommand;
import threeadd.glimpsy.feature.discord.DiscordMessagesCommand;
import threeadd.glimpsy.feature.discordsrv.DiscordChatMuteCommand;
import threeadd.glimpsy.feature.help.HelpCommand;
import threeadd.glimpsy.feature.help.list.RecipesCommand;
import threeadd.glimpsy.feature.map.MapResetCommand;
import threeadd.glimpsy.feature.nickname.ColorNickCommand;
import threeadd.glimpsy.feature.nonchest.NonChestCommand;
import threeadd.glimpsy.feature.randomitems.RandomItemCommand;
import threeadd.glimpsy.feature.simple.FishImmuneCommand;
import threeadd.glimpsy.feature.simple.HatCommand;
import threeadd.glimpsy.feature.simple.RightClickCraftingCommand;
import threeadd.glimpsy.feature.simple.SuicideCommand;
import threeadd.glimpsy.feature.spawn.WorldSpawnCommand;
import threeadd.glimpsy.feature.teams.ShieldSkinCommand;
import threeadd.glimpsy.feature.teams.TeamSetCommand;
import threeadd.glimpsy.feature.teams.TeamsCommand;
import threeadd.glimpsy.feature.wind.WindCommand;
import threeadd.glimpsy.util.registry.Registry;

public class CustomCommandRegistry extends Registry<CustomCommand> {

    public final static CustomCommandRegistry INSTANCE = new CustomCommandRegistry();

    private CustomCommandRegistry() {
        register(new RandomItemCommand(),
                new HelpCommand(),
                new CompetitionCommand(),
                new NonChestCommand(),
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
                new TeamSetCommand(),
                new ShieldSkinCommand(),
                new ColorNickCommand());
    }
}
