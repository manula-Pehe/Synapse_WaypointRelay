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

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

@SpringBootTest
@Transactional
class IssueWorkflowTests {
    @Autowired IssueController issues;
    @Autowired JdbcTemplate jdbc;

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
        SignedInUser.asDispatcher("usr-t-dispatch");
        var answered = issues.reply(issue.id(), new IssueController.MessageRequest("Replacement arranged"));
        assertThat(answered.status()).isEqualTo("ANSWERED");
        SignedInUser.asStoreManager("usr-t-store", "OUT991");
        assertThat(issues.storeDetail(issue.id()).messages()).extracting(IssueController.IssueMessage::text)
                .contains("Replacement arranged");
        SignedInUser.asStoreManager("usr-t-other", "OUT992");
        assertThatThrownBy(() -> issues.storeDetail(issue.id())).isInstanceOf(NotFoundException.class);
    }
}
