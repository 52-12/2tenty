package threeadd.glimpsy.util.manager;

import threeadd.glimpsy.feature.collection.CollectionManager;
import threeadd.glimpsy.feature.competition.CompetitionManager;
import threeadd.glimpsy.feature.discord.DiscordManager;
import threeadd.glimpsy.feature.discordsrv.DiscordChatMuteManager;
import threeadd.glimpsy.feature.map.MapResetManager;
import threeadd.glimpsy.feature.nametag.NametagManager;
import threeadd.glimpsy.feature.nickname.NicknameManager;
import threeadd.glimpsy.feature.nonchest.NonChestManager;
import threeadd.glimpsy.feature.randomitems.RandomItemManager;
import threeadd.glimpsy.feature.simple.TabManager;
import threeadd.glimpsy.feature.spawn.WorldSpawnManager;
import threeadd.glimpsy.feature.teams.ShieldSkinManager;
import threeadd.glimpsy.feature.teams.TeamManager;
import threeadd.glimpsy.feature.wind.WindManager;
import threeadd.glimpsy.util.registry.Registry;

public class ManagerRegistry extends Registry<Manager> {

    public final static ManagerRegistry INSTANCE = new ManagerRegistry();

    private ManagerRegistry() {
        register(new RandomItemManager(),
                new CompetitionManager(),
                new NonChestManager(),
                new MapResetManager(),
                new NametagManager(),
                new WindManager(),
                new CollectionManager(),
                new TabManager(),
                new DiscordManager(),
                new DiscordChatMuteManager(),
                new WorldSpawnManager(),
                new TeamManager(),
                new ShieldSkinManager(),
                new NicknameManager());
    }
}
