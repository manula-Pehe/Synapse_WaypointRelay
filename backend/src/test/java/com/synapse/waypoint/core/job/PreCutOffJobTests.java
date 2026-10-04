package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** The 3 PM store reminder and the 3:30 PM dispatcher alert. Outlets, users and orders are invented. */
@SpringBootTest
@Transactional
class PreCutOffJobTests {

    private static final String DEPOT = OrderFixtures.DEPOT;
    private static final JobRun RUN = new JobRun(OrderFixtures.RUN_DATE, DEPOT);

    @Autowired StoreReminderJob reminderJob;
    @Autowired DispatcherAlertJob alertJob;
    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void createOutletsAndUsers() {
        OrderFixtures.insertOutlet(jdbc, "OUT971", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT972", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT973", "Style", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT974", "Fresh", "Testdistrict", "Otherdepot");
        JobTestData.insertUser(jdbc, "usr-j-store-1", "STORE_MANAGER", "OUT971", null);
        JobTestData.insertUser(jdbc, "usr-j-store-2", "STORE_MANAGER", "OUT972", null);
        JobTestData.insertUser(jdbc, "usr-j-store-3", "STORE_MANAGER", "OUT973", null);
        JobTestData.insertUser(jdbc, "usr-j-store-4", "STORE_MANAGER", "OUT974", null);
        JobTestData.insertUser(jdbc, "usr-j-disp-here", "DISPATCHER", null, DEPOT);
        JobTestData.insertUser(jdbc, "usr-j-disp-other", "DISPATCHER", null, "Otherdepot");
    }

    @Test
    void shouldBeDueAt3PmAnd330PmOnTheDayBeforeTheRun() {
        assertThat(reminderJob.triggerAt(RUN.runDate())).isEqualTo(Instant.parse("2026-09-30T09:30:00Z"));
        assertThat(alertJob.triggerAt(RUN.runDate())).isEqualTo(Instant.parse("2026-09-30T10:00:00Z"));
    }

    @Test
    void shouldRemindOnlyStoresWithUnconfirmedChilledStyleOrTechOrders() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);
        OrderFixtures.save(orders, "OUT972", "Fresh", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
        OrderFixtures.save(orders, "OUT973", "Style", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
        OrderFixtures.save(orders, "OUT974", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);

        JobOutcome outcome = reminderJob.run(RUN);

        assertThat(outcome.isSkipped()).isFalse();
        assertThat(recipientsOf(StoreReminderJob.NOTIFICATION_TYPE))
                .containsExactly("usr-j-store-1", "usr-j-store-3");
    }

    @Test
    void shouldWarnWithTheConfirmByFourPmMessageAndLinkToTheOrders() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);

        reminderJob.run(RUN);

        entityManager.flush();
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT severity, title, link FROM notifications WHERE user_id = 'usr-j-store-1'");
        assertThat(row).containsEntry("severity", "WARNING").containsEntry("title", "Confirm by 4 PM")
                .containsEntry("link", StoreReminderJob.STORE_ORDERS_LINK);
    }

    @Test
    void shouldNotRemindStoresWhoHaveConfirmedEverything() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.CONFIRMED);

        JobOutcome outcome = reminderJob.run(RUN);

        assertThat(outcome.isSkipped()).isTrue();
        assertThat(recipientsOf(StoreReminderJob.NOTIFICATION_TYPE)).isEmpty();
    }

    @Test
    void shouldAlertOnlyTheDispatchersOfTheDepotAndNameTheDepotInTheTitle() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);
        OrderFixtures.save(orders, "OUT972", "Fresh", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
        OrderFixtures.save(orders, "OUT974", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);

        alertJob.run(RUN);

        assertThat(recipientsOf(DispatcherAlertJob.NOTIFICATION_TYPE))
                .contains("usr-j-disp-here").doesNotContain("usr-j-disp-other");
        entityManager.flush();
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT severity, title, body, link FROM notifications WHERE user_id = 'usr-j-disp-here'");
        assertThat(row).containsEntry("severity", "WARNING").containsEntry("link", "/dispatch");
        assertThat((String) row.get("title")).isEqualTo(DEPOT + ": 2 stores have not confirmed");
        assertThat((String) row.get("body")).contains("OUT971").contains("OUT972").doesNotContain("OUT974");
    }

    @Test
    void shouldSendNothingWhenEveryoneHasConfirmed() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.CONFIRMED);

        JobOutcome outcome = alertJob.run(RUN);

        assertThat(outcome.isSkipped()).isTrue();
        assertThat(recipientsOf(DispatcherAlertJob.NOTIFICATION_TYPE)).isEmpty();
    }

    @Test
    void shouldSkipBothJobsOnceTheOrdersAreClosed() {
        OrderFixtures.save(orders, "OUT971", "Fresh", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);
        closeOrders.close(LocalDate.parse("2026-10-01"), DEPOT);

        List<JobOutcome> outcomes = List.of(reminderJob.run(RUN), alertJob.run(RUN));

        assertThat(outcomes).allMatch(JobOutcome::isSkipped);
        assertThat(recipientsOf(StoreReminderJob.NOTIFICATION_TYPE)).isEmpty();
        assertThat(recipientsOf(DispatcherAlertJob.NOTIFICATION_TYPE)).isEmpty();
    }

    private List<String> recipientsOf(String type) {
        entityManager.flush();
        return JobTestData.recipientsOf(jdbc, type);
    }
}
