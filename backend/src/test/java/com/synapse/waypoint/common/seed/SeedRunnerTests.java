package com.synapse.waypoint.common.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SeedRunnerTests {

    private final List<String> executed = new ArrayList<>();
    private final FakeMarker marker = new FakeMarker();

    @Test
    void shouldRunStepsInOrderAndMarkSeeded() {
        SeedRunner runner = new SeedRunner(List.of(step(20, "second"), step(10, "first")), marker);

        runner.runOnce();

        assertThat(executed).containsExactly("first", "second");
        assertThat(marker.seeded).isTrue();
    }

    @Test
    void shouldSkipEverythingWhenAlreadySeeded() {
        marker.seeded = true;

        new SeedRunner(List.of(step(10, "first")), marker).runOnce();

        assertThat(executed).isEmpty();
    }

    @Test
    void shouldNotMarkSeededWhenAStepFails() {
        SeedStep failing = new SeedStep() {
            public int order() { return 20; }
            public String name() { return "failing"; }
            public void run() { throw new IllegalStateException("boom"); }
        };
        SeedRunner runner = new SeedRunner(List.of(step(10, "first"), failing), marker);

        assertThatThrownBy(runner::runOnce).hasMessage("boom");
        assertThat(marker.seeded).isFalse();
    }

    private SeedStep step(int order, String name) {
        return new SeedStep() {
            public int order() { return order; }
            public String name() { return name; }
            public void run() { executed.add(name); }
        };
    }

    private static final class FakeMarker implements SeedMarker {
        boolean seeded;

        public boolean isSeeded() { return seeded; }

        public void markSeeded() { seeded = true; }
    }
}
