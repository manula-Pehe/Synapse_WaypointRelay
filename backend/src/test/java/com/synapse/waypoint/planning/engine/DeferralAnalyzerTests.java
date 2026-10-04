package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.RUN_DATE;
import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.PlanningTestData.InputBuilder;
import com.synapse.waypoint.planning.engine.PlanningTestData.OrderBuilder;

class DeferralAnalyzerTests {

    private static InputBuilder baseInput() {
        return anInput().outlet(anOutlet().id("O1"));
    }

    private static List<PlannedDeferral> deferralsOf(InputBuilder builder) {
        return new PlanningEngine().plan(builder.build()).deferrals();
    }

    private static InputBuilder oneReeferFor(OrderBuilder... orders) {
        InputBuilder builder = baseInput().vehicle(aVehicle().reefer().capacity("150", "10").weeklyQuotaLitres("12"));
        for (OrderBuilder order : orders) {
            builder.order(order);
        }
        return builder;
    }

    @Test
    void shouldCallItUnavoidableWithTheSharedRuleWhenEveryVehicleIsTooSmall() {
        PlannedDeferral deferral = deferralsOf(baseInput()
                .vehicle(aVehicle().id("V1").capacity("100", "10")).vehicle(aVehicle().id("V2").capacity("200", "10"))
                .order(anOrder().ref("HUGE").outlet("O1").weightKg("5000"))).get(0);

        assertThat(deferral.kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
        assertThat(deferral.rule()).isEqualTo(RuleCode.OVER_WEIGHT);
        assertThat(deferral.reason()).contains("heavier");
    }

    @Test
    void shouldUseNoVehicleFitsWhenVehiclesAreBlockedByDifferentRules() {
        // the dry vehicle fails FRIDGE_REQUIRED, the fridge vehicle fails OVER_WEIGHT
        PlannedDeferral deferral = deferralsOf(baseInput()
                .vehicle(aVehicle().id("DRY")).vehicle(aVehicle().id("COLD").reefer().capacity("100", "10"))
                .order(anOrder().ref("BIG").outlet("O1").chilled().weightKg("500"))).get(0);

        assertThat(deferral.kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
        assertThat(deferral.rule()).isEqualTo(RuleCode.NO_VEHICLE_FITS);
    }

    @Test
    void shouldCallItUnavoidableWhenThereAreNoVehicles() {
        PlannedDeferral deferral = deferralsOf(baseInput().order(anOrder().ref("A").outlet("O1"))).get(0);

        assertThat(deferral.kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
        assertThat(deferral.rule()).isEqualTo(RuleCode.NO_VEHICLE_FITS);
    }

    @Test
    void shouldCallItChosenWithFridgeCapacityWhenTheFridgeVehicleIsFull() {
        PlannedDeferral deferral = deferralsOf(oneReeferFor(
                anOrder().ref("FIRST").outlet("O1").chilled().weightKg("100").daysSinceLastServed(2),
                anOrder().ref("SECOND").outlet("O1").chilled().weightKg("100"))).get(0);

        assertThat(deferral.order().ref()).isEqualTo("SECOND");
        assertThat(deferral.kind()).isEqualTo(DeferralKind.CHOSEN);
        assertThat(deferral.rule()).isEqualTo(RuleCode.FRIDGE_CAPACITY);
        assertThat(deferral.reason()).isEqualTo("The only fridge vehicle is full before 8 AM.");
    }

    @Test
    void shouldCountFridgeVehiclesInTheReason() {
        InputBuilder builder = baseInput()
                .vehicle(aVehicle().id("C1").reefer().capacity("150", "10").weeklyQuotaLitres("12"))
                .vehicle(aVehicle().id("C2").reefer().capacity("150", "10").weeklyQuotaLitres("12"))
                .order(anOrder().ref("A").outlet("O1").chilled().weightKg("100"))
                .order(anOrder().ref("B").outlet("O1").chilled().weightKg("100"))
                .order(anOrder().ref("C").outlet("O1").chilled().weightKg("100"));

        assertThat(deferralsOf(builder)).extracting(PlannedDeferral::reason)
                .containsExactly("All 2 fridge vehicles are full before 8 AM.");
    }

    @Test
    void shouldUseTheLastBlockingRuleForOtherChosenDeferrals() {
        // one vehicle, 12 litre quota: the second order cannot join (weight) or open a second trip (fuel)
        PlannedDeferral deferral = deferralsOf(baseInput()
                .vehicle(aVehicle().capacity("150", "10").weeklyQuotaLitres("12"))
                .order(anOrder().ref("A").outlet("O1").weightKg("100").daysSinceLastServed(2))
                .order(anOrder().ref("B").outlet("O1").weightKg("100"))).get(0);

        assertThat(deferral.kind()).isEqualTo(DeferralKind.CHOSEN);
        assertThat(deferral.rule()).isEqualTo(RuleCode.FUEL_QUOTA);
    }

    @Test
    void shouldReportScoreDaysWaitedAndNextRunDate() {
        PlannedDeferral deferral = deferralsOf(baseInput()
                .vehicle(aVehicle().capacity("100", "10"))
                .order(anOrder().ref("HUGE").outlet("O1").weightKg("5000").chilled().daysSinceLastServed(3))).get(0);

        assertThat(deferral.priorityScore()).isEqualTo(3 + 4);
        assertThat(deferral.daysWaited()).isEqualTo(3);
        assertThat(deferral.newDate()).isEqualTo(RUN_DATE.plusDays(1));
    }

    @Test
    void shouldAskForADecisionOnlyWhenTheOrderWasAlreadyDeferredYesterday() {
        InputBuilder builder = baseInput().vehicle(aVehicle().capacity("100", "10"))
                .order(anOrder().ref("AGAIN").outlet("O1").weightKg("5000").deferredYesterday())
                .order(anOrder().ref("FIRST").outlet("O1").weightKg("5000"));

        assertThat(deferralsOf(builder)).extracting(deferral -> deferral.order().ref(), PlannedDeferral::needsDecision)
                .containsExactlyInAnyOrder(
                        tuple("AGAIN", true),
                        tuple("FIRST", false));
    }
}
