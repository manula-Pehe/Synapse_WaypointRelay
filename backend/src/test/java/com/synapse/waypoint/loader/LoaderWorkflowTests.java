package com.synapse.waypoint.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;

/** Uses the real migration, plan facade, order lifecycle and notification service. */
@SpringBootTest(properties = {"app.seed.enabled=false", "app.jobs.enabled=false",
        "app.jwt.secret=loader-workflow-test-secret-with-more-than-32-characters"})
@Transactional
class LoaderWorkflowTests {
    private static final LocalDate RUN = LocalDate.of(2026, 10, 1);

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired LoaderService loader;
    @MockitoBean CurrentUser current;

    @BeforeEach
    void setUp() {
        given(current.id()).willReturn("loader-test");
        given(current.idIfSignedIn()).willReturn(Optional.of("loader-test"));
        given(current.role()).willReturn(Role.LOADER);
        given(current.depot()).willReturn(Optional.of("Peliyagoda"));
        jdbc.update("INSERT INTO outlets(id,brand,district,depot,dock_type,parking_constraint,window_open,window_close) VALUES ('L-OUT1','Fresh','Colombo','Peliyagoda','rear_dock','normal','05:00','07:30'),('L-OUT2','Fresh','Galle','Galle','street','normal','05:00','07:30')");
        jdbc.update("INSERT INTO vehicles(id,type,temp,weight_cap_kg,volume_cap_m3,fuel_type,km_per_l,weekly_fuel_quota_l,depot) VALUES ('L-VEH1','van','reefer',1000,7,'diesel',8,500,'Peliyagoda'),('L-VEH2','van','ambient',1000,7,'diesel',8,500,'Galle')");
        jdbc.update("INSERT INTO users(id,name,role,secret_hash,depot) VALUES ('loader-test','Test Loader','LOADER','x','Peliyagoda'),('dispatch-test','Test Dispatch','DISPATCHER','x','Peliyagoda')");
        jdbc.update("INSERT INTO users(id,name,role,secret_hash,outlet_id) VALUES ('store-test','Test Store','STORE_MANAGER','x','L-OUT1')");
        jdbc.update("INSERT INTO users(id,name,role,secret_hash,staff_id,vehicle_id) VALUES ('driver-test','Test Driver','DRIVER','x','DRV-TEST','L-VEH1')");
        jdbc.update("INSERT INTO orders(id,ref,outlet_id,brand,temp_requirement,units,weight_kg,volume_m3,run_date,status,source) VALUES ('L-O1','L-001','L-OUT1','Fresh','CHILLED',10,100,1,?,'PLANNED','SEED'),('L-O2','L-002','L-OUT1','Fresh','AMBIENT',5,50,0.5,?,'PLANNED','SEED'),('L-O3','L-003','L-OUT2','Fresh','AMBIENT',4,40,0.4,?,'PLANNED','SEED')", RUN, RUN, RUN);
        jdbc.update("INSERT INTO plans(id,run_date,depot,version,status,summary,created_at) VALUES ('L-P1',?,'Peliyagoda',1,'PUBLISHED','{}'::jsonb,now()),('L-P2',?,'Galle',1,'PUBLISHED','{}'::jsonb,now())", RUN, RUN);
        jdbc.update("INSERT INTO trips(id,plan_id,vehicle_id,trip_no,brand,district,window_type,depart_at,minutes,weight_kg,volume_m3,km) VALUES ('L-T1','L-P1','L-VEH1',1,'Fresh','Colombo','FRESH','2026-10-01 04:30:00+05:30',90,150,1.5,20),('L-T2','L-P2','L-VEH2',1,'Fresh','Galle','FRESH','2026-10-01 04:40:00+05:30',90,40,0.4,20)");
        jdbc.update("INSERT INTO stops(id,trip_id,order_id,seq,load_seq,arrive_from,arrive_to,late_risk) VALUES ('L-S1','L-T1','L-O1',1,2,'2026-10-01 05:00:00+05:30','2026-10-01 07:30:00+05:30',0),('L-S2','L-T1','L-O2',2,1,'2026-10-01 05:00:00+05:30','2026-10-01 07:30:00+05:30',0),('L-S3','L-T2','L-O3',1,1,'2026-10-01 05:00:00+05:30','2026-10-01 07:30:00+05:30',0)");
    }

    @Test
    void listsOnlyPublishedTripsInTheLoadersDepot() {
        assertThat(loader.trips(RUN).items()).extracting(LoaderService.TripSummary::tripId).containsExactly("L-T1");
        assertThatThrownBy(() -> loader.trip("L-T2")).isInstanceOf(DomainException.class)
                .satisfies(error -> assertThat(((DomainException) error).code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void secondTripWaitsForTheSameVehicleToLeave() {
        jdbc.update("INSERT INTO orders(id,ref,outlet_id,brand,temp_requirement,units,weight_kg,volume_m3,run_date,status,source) VALUES ('L-O4','L-004','L-OUT1','Fresh','AMBIENT',3,30,0.3,?,'PLANNED','SEED')", RUN);
        jdbc.update("INSERT INTO trips(id,plan_id,vehicle_id,trip_no,brand,district,window_type,depart_at,minutes,weight_kg,volume_m3,km) VALUES ('L-T3','L-P1','L-VEH1',2,'Fresh','Colombo','DAYTIME','2026-10-01 07:00:00+05:30',90,30,0.3,20)");
        jdbc.update("INSERT INTO stops(id,trip_id,order_id,seq,load_seq,arrive_from,arrive_to,late_risk) VALUES ('L-S4','L-T3','L-O4',1,1,'2026-10-01 08:00:00+05:30','2026-10-01 10:00:00+05:30',0)");
        assertThat(loader.trips(RUN).items()).extracting(LoaderService.TripSummary::vehicleAvailable)
                .containsExactly(true, false);
        assertThatThrownBy(() -> loader.tick("L-S4")).isInstanceOf(DomainException.class)
                .satisfies(error -> assertThat(((DomainException) error).code()).isEqualTo(ErrorCode.RULE_VIOLATION));
    }

    @Test
    void failedFridgeCheckAlertsDispatchAndBlocksLoading() {
        assertThat(loader.fridgeCheck("L-T1", true, new BigDecimal("11"), true).passed()).isFalse();
        entityManager.flush();
        assertThatThrownBy(() -> loader.tick("L-S1")).isInstanceOf(DomainException.class)
                .satisfies(error -> assertThat(((DomainException) error).code()).isEqualTo(ErrorCode.RULE_VIOLATION));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id='dispatch-test' AND type='FRIDGE_CHECK_FAILED'", Integer.class)).isEqualTo(1);
        assertThat(loader.fridgeCheck("L-T1", true, new BigDecimal("3"), true).passed()).isTrue();
        assertThat(loader.tick("L-S1").trip().ticked()).isEqualTo(1);
        assertThat(loader.fridgeCheck("L-T1", true, new BigDecimal("11"), true).passed()).isFalse();
        assertThat(loader.trip("L-T1").fridgeCheck().passed()).isFalse();
    }

    @Test
    void shortfallBooksRemainderAndKeepsTheOriginalLoadReduced() {
        loader.fridgeCheck("L-T1", true, new BigDecimal("3"), true);
        loader.tick("L-S1");
        LoaderService.ShortfallResult result = loader.shortfall("L-S1", 2, "MISSING", "Two absent");
        assertThat(result.remainderOrderRef()).startsWith("L-001-R");
        assertThat(jdbc.queryForObject("SELECT units FROM orders WHERE id=?", Integer.class, result.remainderOrderId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM order_events WHERE order_id=?", Integer.class, result.remainderOrderId())).isEqualTo(1);
        assertThat(loader.trip("L-T1").stops().stream().filter(stop -> stop.stopId().equals("L-S1")).findFirst().orElseThrow().units()).isEqualTo(8);
        assertThat(loader.shortfallForOrder("L-O1")).isPresent();
    }

    @Test
    void fullStopShortfallGoesToDispatchWithoutCreatingARemainder() {
        assertThatThrownBy(() -> loader.shortfall("L-S1", 10, "MISSING", null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Contact dispatch");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shortfalls WHERE stop_id='L-S1'", Integer.class)).isZero();
    }

    @Test
    void missingShortfallReasonReturnsValidationError() {
        assertThatThrownBy(() -> loader.shortfall("L-S1", 2, null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(error -> assertThat(((DomainException) error).code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void handoverMarksEveryTripOrderLoaded() {
        loader.fridgeCheck("L-T1", true, new BigDecimal("3"), true);
        loader.tick("L-S1");
        loader.tick("L-S2");
        assertThat(loader.handover("L-T1", "DRV-TEST").status()).isEqualTo("LOADED");
        assertThat(jdbc.queryForList("SELECT status FROM orders WHERE id IN ('L-O1','L-O2') ORDER BY id", String.class))
                .containsExactly("LOADED", "LOADED");
        assertThat(jdbc.queryForObject("SELECT driver_id FROM handovers WHERE trip_id='L-T1'", String.class)).isEqualTo("driver-test");
    }
}
