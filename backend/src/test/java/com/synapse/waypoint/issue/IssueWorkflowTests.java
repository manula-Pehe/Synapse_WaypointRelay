package com.synapse.waypoint.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

@SpringBootTest
@Transactional
class IssueWorkflowTests {
    @Autowired IssueController issues;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @BeforeEach void setup() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT992", "Fresh", "Otherdistrict");
        OrderFixtures.insertUser(jdbc, "usr-t-store", "STORE_MANAGER", "OUT991");
        OrderFixtures.insertUser(jdbc, "usr-t-other", "STORE_MANAGER", "OUT992");
        OrderFixtures.insertUser(jdbc, "usr-t-dispatch", "DISPATCHER", null);
        SignedInUser.asStoreManager("usr-t-store", "OUT991");
    }
    @AfterEach void signOut() { SignedInUser.signOut(); }

    @Test void replyIsVisibleToStoreAndAnotherOutletCannotReadIt() {
        var issue = issues.create(new IssueController.NewIssue(null, "DAMAGED", 2, "REPLACE", "Two cases damaged"));
        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT user_id, link FROM notifications WHERE type = 'ISSUE_RECORDED'"))
                .containsEntry("user_id", "usr-t-store")
                .containsEntry("link", "/store/issues/" + issue.id());
        SignedInUser.asDispatcher("usr-t-dispatch");
        var answered = issues.reply(issue.id(), new IssueController.MessageRequest("Replacement arranged"));
        assertThat(answered.status()).isEqualTo("ANSWERED");
        entityManager.flush();
        assertThat(jdbc.queryForList("SELECT user_id, type, link FROM notifications WHERE type = 'ISSUE_REPLY'"))
                .singleElement().satisfies(row -> {
                    assertThat(row.get("user_id")).isEqualTo("usr-t-store");
                    assertThat(row.get("link")).isEqualTo("/store/issues/" + issue.id());
                });
        SignedInUser.asStoreManager("usr-t-store", "OUT991");
        assertThat(issues.storeDetail(issue.id()).messages()).extracting(IssueController.IssueMessage::text)
                .contains("Replacement arranged");
        SignedInUser.asStoreManager("usr-t-other", "OUT992");
        assertThatThrownBy(() -> issues.storeDetail(issue.id())).isInstanceOf(NotFoundException.class);
    }

    @Test void resolutionAlertsOnlyTheIssueOutletOnce() {
        var issue = issues.create(new IssueController.NewIssue(null, "MISSING", 1, "CREDIT", "One case missing"));
        SignedInUser.asDispatcher("usr-t-dispatch");
        issues.resolve(issue.id());
        entityManager.flush();
        assertThat(jdbc.queryForList("SELECT user_id FROM notifications WHERE type = 'ISSUE_RESOLVED'", String.class))
                .containsExactly("usr-t-store");
        assertThatThrownBy(() -> issues.resolve(issue.id())).isInstanceOf(com.synapse.waypoint.common.error.DomainException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE type = 'ISSUE_RESOLVED'", Integer.class))
                .isEqualTo(1);
    }
}
