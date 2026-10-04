package com.synapse.waypoint.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.dto.DriverStatusDto;
import com.synapse.waypoint.driver.service.DeliveryQueryService;
import com.synapse.waypoint.loader.LoaderService.ShortfallResult;
import com.synapse.waypoint.loader.LoadingQueryService;

/** How the store reshapes driver and loader facts; both services are mocked and the values invented. */
class StoreDeliveryFactsTests {

    private static final Instant AT = Instant.parse("2030-01-02T03:04:05Z");
    private static final ArrivalView ARRIVAL =
            new ArrivalView(OffsetDateTime.now(), OffsetDateTime.now(), BigDecimal.ZERO, "VEH-X", 1, null);

    private final DeliveryQueryService driver = mock(DeliveryQueryService.class);
    private final LoadingQueryService loader = mock(LoadingQueryService.class);
    private final StoreDeliveryFacts facts = new StoreDeliveryFacts(driver, loader);

    @Test
    void shouldLinkProofFilesThroughTheFilesEndpoint() {
        when(driver.deliveryForOrder("o-1")).thenReturn(Optional.of(delivery("ph-1", "sg-1")));

        DeliveryProofView proof = facts.delivery("o-1").orElseThrow();

        assertThat(proof.outcome()).isEqualTo(DeliveryOutcome.DELIVERED);
        assertThat(proof.units()).isEqualTo(7);
        assertThat(proof.photoUrl()).isEqualTo("/api/files/ph-1");
        assertThat(proof.signatureUrl()).isEqualTo("/api/files/sg-1");
        assertThat(proof.receivedBy()).isEqualTo("Test Receiver");
        assertThat(proof.at()).isEqualTo(AT);
    }

    @Test
    void shouldLeaveUrlsNullWhenThereIsNoProofFile() {
        when(driver.deliveryForOrder("o-1")).thenReturn(Optional.of(delivery(null, null)));

        DeliveryProofView proof = facts.delivery("o-1").orElseThrow();

        assertThat(proof.photoUrl()).isNull();
        assertThat(proof.signatureUrl()).isNull();
    }

    @Test
    void shouldHaveNoDeliveryBeforeTheDriverRecordsOne() {
        when(driver.deliveryForOrder("o-1")).thenReturn(Optional.empty());

        assertThat(facts.delivery("o-1")).isEmpty();
    }

    @Test
    void shouldShowTheShortfallWithItsRemainderOrderRef() {
        when(loader.shortfallForOrder("o-1"))
                .thenReturn(Optional.of(new ShortfallResult("o-1", "o-2", "T-1-R", 3, "MISSING", "")));

        assertThat(facts.shortfall("o-1")).contains(new ShortfallView(3, "MISSING", "T-1-R"));
    }

    @Test
    void shouldReportWhetherTheTrucksPhoneIsOfflineWhileOnTheWay() {
        when(driver.driverStatus("VEH-X"))
                .thenReturn(new DriverStatusDto("VEH-X", "d-1", "t-1", 2, AT, 0, true));

        assertThat(facts.driverStatus(OrderStatus.ON_THE_WAY, ARRIVAL))
                .contains(new DriverStatusView(true, AT));
    }

    @Test
    void shouldNotAskAboutTheDriverUnlessTheOrderIsOnTheWay() {
        assertThat(facts.driverStatus(OrderStatus.PLANNED, ARRIVAL)).isEmpty();
        assertThat(facts.driverStatus(OrderStatus.ON_THE_WAY, null)).isEmpty();
        verifyNoInteractions(driver);
    }

    private static DeliveryDto delivery(String photoFileId, String signatureFileId) {
        return new DeliveryDto("d-1", "s-1", "o-1", "VEH-X", DeliveryOutcome.DELIVERED, 7, null, "Test Receiver",
                photoFileId != null, signatureFileId != null, photoFileId, signatureFileId, AT, false, null, null);
    }
}
