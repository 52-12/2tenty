package threeadd.glimpsy.bench;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import threeadd.glimpsy.util.text.ComponentParser;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks the two things that show up everywhere: the proximity-chat colour maths (cheap) and
 * building a component from a MiniMessage string (the cost Messages.msg pays on every call).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ComponentParserBenchmark {

    private static final TextColor NEAR = TextColor.color(0xFFF8EB);
    private static final TextColor FAR = TextColor.color(0x4B4B4B);

    @Benchmark
    public TextColor interpolateColor() {
        return ComponentParser.interpolateColor(NEAR, FAR, 0.37);
    }

    @Benchmark
    public Component parseRichString() {
        return ComponentParser.parseRichString("<muted>Map reset <white>{reset}");
    }
}
