package threeadd.glimpsy.bench;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.concurrent.TimeUnit;

/**
 * Shows the win from caching the "[Team] " prefix component: the nametag and tab updaters used to
 * re-parse a team's MiniMessage name for every player, every second (O(players^2) parses/sec).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class TeamPrefixBenchmark {

    private static final String RAW = "<red><bold>MyTeam</bold></red>";

    @Benchmark
    public Component cached() {
        return ComponentParser.teamPrefix(RAW);
    }

    @Benchmark
    public Component uncached() {
        return Component.textOfChildren(
                Component.text("[", NamedTextColor.GRAY),
                MiniMessage.miniMessage().deserialize(RAW),
                Component.text("] ", NamedTextColor.GRAY));
    }
}
