package threeadd.glimpsy.util.command;

import threeadd.glimpsy.feature.collection.CollectionCommand;
import threeadd.glimpsy.feature.competition.view.CompetitionCommand;
import threeadd.glimpsy.feature.help.HelpCommand;
import threeadd.glimpsy.feature.map.MapResetCommand;
import threeadd.glimpsy.feature.nonchest.NonChestCommand;
import threeadd.glimpsy.feature.randomitems.RandomItemCommand;
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
                new CollectionCommand());
    }
}
