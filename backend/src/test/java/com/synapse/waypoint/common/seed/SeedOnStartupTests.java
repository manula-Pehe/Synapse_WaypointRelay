package com.synapse.waypoint.common.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SeedOnStartupTests {

    @Autowired
    ObjectProvider<SeedOnStartup> startup;

    @Test
    void shouldBeSwitchedOffInTests() {
        assertThat(startup.getIfAvailable()).isNull();
    }

    @Test
    void shouldRunTheSeedOnStartup() {
        SeedRunner runner = mock(SeedRunner.class);

        new SeedOnStartup(runner).run(new DefaultApplicationArguments());

        verify(runner).runOnce();
    }

    @Test
    void shouldStopStartupWhenSeedingFails() {
        SeedRunner runner = mock(SeedRunner.class);
        doThrow(new IllegalStateException("file x.csv not found")).when(runner).runOnce();

        assertThatThrownBy(() -> new SeedOnStartup(runner).run(new DefaultApplicationArguments()))
                .hasMessageContaining("x.csv");
    }
}
