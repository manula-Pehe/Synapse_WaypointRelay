package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** app.data-dir is a plain file path, including relative ones such as the default "../data". */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=../backend/src/test/resources/seed")
class DatasetLocatorConfigurationTests {

    @Autowired DatasetLocator locator;

    @Test
    void shouldAcceptARelativePathWithParentSegments() {
        assertThat(locator.locate("outlets.csv")).exists();
    }
}
