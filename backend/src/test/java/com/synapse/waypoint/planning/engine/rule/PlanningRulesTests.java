package com.synapse.waypoint.planning.engine.rule;

import static com.synapse.waypoint.planning.engine.PlanningTestData.aStop;
import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.PlanningTestData.OrderBuilder;
import com.synapse.waypoint.planning.engine.PlanningTestData.OutletBuilder;
import com.synapse.waypoint.planning.engine.PlanningTestData.VehicleBuilder;
import com.synapse.waypoint.planning.engine.StopCandidate;
import com.synapse.waypoint.planning.engine.TripCalculator;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.TravelInput;

/** One nested group per rule: a day that satisfies it and a day that breaks it. */
class PlanningRulesTests {

    private static final PlanningInput DEFAULT_INPUT = anInput().build();
    private static final TripCalculator CALCULATOR = new TripCalculator(DEFAULT_INPUT);

    private static StopCandidate stop(OrderBuilder order, OutletBuilder outlet) {
        return aStop(order, outlet);
    }

    private static StopCandidate plainStop() {
        return stop(anOrder(), anOutlet());
    }

    private static VehicleDay day(VehicleBuilder vehicle, StopCandidate... stops) {
        return VehicleDay.idle(vehicle.build()).withTrip(new TripDraft(List.of(stops)));
    }

    private static RuleViolation violationOf(PlanningRule rule, VehicleDay day) {
        return rule.check(day).orElseThrow();
    }

    @Nested
    class BrandDistrict {
        private final PlanningRule rule = new BrandDistrictRule();

        @Test
        void shouldAcceptOneBrandInOneDistrict() {
            assertThat(rule.check(day(aVehicle(), plainStop(), stop(anOrder().ref("B"), anOutlet().id("O2"))))).isEmpty();
        }

        @Test
        void shouldRejectTwoBrandsOnOneTrip() {
            VehicleDay mixed = day(aVehicle(), plainStop(), stop(anOrder().ref("B").brand(Brand.STYLE), anOutlet().id("O2")));

            assertThat(violationOf(rule, mixed).rule()).isEqualTo(RuleCode.BRAND_DISTRICT);
        }

        @Test
        void shouldRejectTwoDistrictsOnOneTrip() {
            VehicleDay mixed = day(aVehicle(), plainStop(), stop(anOrder().ref("B"), anOutlet().id("O2").district("South")));

            assertThat(violationOf(rule, mixed).rule()).isEqualTo(RuleCode.BRAND_DISTRICT);
        }
    }

    @Nested
    class FridgeRequired {
        private final PlanningRule rule = new FridgeRequiredRule();

        @Test
        void shouldAcceptChilledOnAReefer() {
            assertThat(rule.check(day(aVehicle().reefer(), stop(anOrder().chilled(), anOutlet())))).isEmpty();
        }

        @Test
        void shouldAcceptAmbientOnAReefer() {
            assertThat(rule.check(day(aVehicle().reefer(), plainStop()))).isEmpty();
        }

        @Test
        void shouldRejectChilledOnAnAmbientVehicle() {
            assertThat(violationOf(rule, day(aVehicle(), stop(anOrder().chilled(), anOutlet()))).rule())
                    .isEqualTo(RuleCode.FRIDGE_REQUIRED);
        }
    }

    @Nested
    class VanOnly {
        private final PlanningRule rule = new VanOnlyRule();

        @Test
        void shouldAcceptAVanAtAVanOnlyOutlet() {
            assertThat(rule.check(day(aVehicle().van(), stop(anOrder(), anOutlet().vanOnly())))).isEmpty();
        }

        @Test
        void shouldAcceptATruckAtANormalOutlet() {
            assertThat(rule.check(day(aVehicle(), plainStop()))).isEmpty();
        }

        @Test
        void shouldRejectATruckAtAVanOnlyOutlet() {
            assertThat(violationOf(rule, day(aVehicle(), stop(anOrder(), anOutlet().vanOnly()))).rule())
                    .isEqualTo(RuleCode.VAN_ONLY);
        }
    }

    @Nested
    class WrongDepot {
        private final PlanningRule rule = new WrongDepotRule();

        @Test
        void shouldAcceptOutletsOfTheVehiclesDepotIgnoringCase() {
            assertThat(rule.check(day(aVehicle().depot("DEPOT-x"), plainStop()))).isEmpty();
        }

        @Test
        void shouldRejectAnOutletOfAnotherDepot() {
            assertThat(violationOf(rule, day(aVehicle().depot("Depot-Y"), plainStop())).rule())
                    .isEqualTo(RuleCode.WRONG_DEPOT);
        }
    }

    @Nested
    class Capacity {
        @Test
        void shouldAcceptAWeightExactlyAtTheLimit() {
            VehicleDay full = day(aVehicle().capacity("200", "10"),
                    stop(anOrder().weightKg("120"), anOutlet()), stop(anOrder().ref("B").weightKg("80"), anOutlet().id("O2")));

            assertThat(new OverWeightRule().check(full)).isEmpty();
        }

        @Test
        void shouldRejectATripOverTheWeightLimitEvenWhenEachOrderFits() {
            VehicleDay heavy = day(aVehicle().capacity("200", "10"),
                    stop(anOrder().weightKg("120"), anOutlet()), stop(anOrder().ref("B").weightKg("90"), anOutlet().id("O2")));

            assertThat(violationOf(new OverWeightRule(), heavy).rule()).isEqualTo(RuleCode.OVER_WEIGHT);
        }

        @Test
        void shouldAcceptAVolumeExactlyAtTheLimit() {
            VehicleDay full = day(aVehicle().capacity("1000", "2"), stop(anOrder().volumeM3("2"), anOutlet()));

            assertThat(new OverVolumeRule().check(full)).isEmpty();
        }

        @Test
        void shouldRejectATripOverTheVolumeLimit() {
            VehicleDay bulky = day(aVehicle().capacity("1000", "2"),
                    stop(anOrder().volumeM3("1.5"), anOutlet()), stop(anOrder().ref("B").volumeM3("0.6"), anOutlet().id("O2")));

            assertThat(violationOf(new OverVolumeRule(), bulky).rule()).isEqualTo(RuleCode.OVER_VOLUME);
        }
    }

    @Nested
    class MaxTrips {
        private final PlanningRule rule = new MaxTripsRule();
        private final TripDraft trip = new TripDraft(List.of(plainStop()));

        @Test
        void shouldAcceptTwoTrips() {
            assertThat(rule.check(VehicleDay.idle(aVehicle().build()).withTrip(trip).withTrip(trip))).isEmpty();
        }

        @Test
        void shouldRejectAThirdTrip() {
            VehicleDay three = VehicleDay.idle(aVehicle().build()).withTrip(trip).withTrip(trip).withTrip(trip);

            assertThat(violationOf(rule, three).rule()).isEqualTo(RuleCode.MAX_TRIPS);
        }
    }

    @Nested
    class TimeBudget {
        // 200 min outbound: one trip is 210 min, two trips are 420 min.
        private final TripCalculator slow = new TripCalculator(
                anInput().travel(new TravelInput("North", new BigDecimal("20"), 200, new BigDecimal("2"), 5)).build());
        private final PlanningRule rule = new TimeBudgetRule(slow);

        @Test
        void shouldAcceptOneFreshTripWithinTwoSeventy() {
            assertThat(rule.check(day(aVehicle(), plainStop()))).isEmpty();
        }

        @Test
        void shouldRejectFreshTripsTotallingOverTwoSeventy() {
            TripDraft trip = new TripDraft(List.of(plainStop()));
            VehicleDay twoTrips = VehicleDay.idle(aVehicle().build()).withTrip(trip).withTrip(trip);

            assertThat(violationOf(rule, twoTrips).rule()).isEqualTo(RuleCode.TIME_BUDGET);
        }

        @Test
        void shouldKeepFreshAndDaytimeBudgetsSeparate() {
            TripDraft fresh = new TripDraft(List.of(plainStop()));
            TripDraft style = new TripDraft(List.of(stop(anOrder().brand(Brand.STYLE), anOutlet())));

            assertThat(rule.check(VehicleDay.idle(aVehicle().build()).withTrip(fresh).withTrip(style))).isEmpty();
        }

        @Test
        void shouldRejectDaytimeTripsTotallingOverFourEighty() {
            TripDraft style = new TripDraft(List.of(stop(anOrder().brand(Brand.TECH), anOutlet())));
            TravelInput longRoad = new TravelInput("North", new BigDecimal("20"), 250, new BigDecimal("2"), 5);
            PlanningRule daytimeRule = new TimeBudgetRule(new TripCalculator(anInput().travel(longRoad).build()));

            assertThat(daytimeRule.check(VehicleDay.idle(aVehicle().build()).withTrip(style))).isEmpty();
            assertThat(violationOf(daytimeRule, VehicleDay.idle(aVehicle().build()).withTrip(style).withTrip(style)).rule())
                    .isEqualTo(RuleCode.TIME_BUDGET);
        }
    }

    @Nested
    class FuelQuota {
        // One stop is 40 km: 8 litres at 5 km/l.
        private final PlanningRule rule = new FuelQuotaRule(CALCULATOR);

        @Test
        void shouldAcceptFuelExactlyAtTheQuota() {
            assertThat(rule.check(day(aVehicle().weeklyQuotaLitres("8"), plainStop()))).isEmpty();
        }

        @Test
        void shouldRejectATripThatExceedsTheQuota() {
            assertThat(violationOf(rule, day(aVehicle().weeklyQuotaLitres("7.9"), plainStop())).rule())
                    .isEqualTo(RuleCode.FUEL_QUOTA);
        }

        @Test
        void shouldCountFuelAlreadyUsedThisWeek() {
            VehicleBuilder tired = aVehicle().weeklyQuotaLitres("10").fuelUsedLitres("3");

            assertThat(violationOf(rule, day(tired, plainStop())).rule()).isEqualTo(RuleCode.FUEL_QUOTA);
        }
    }

    @Nested
    class Window {
        // Default timing: the first stop is reached at 4:10.
        private final PlanningRule rule = new WindowRule(CALCULATOR);

        @Test
        void shouldAcceptArrivalInsideTheWindow() {
            assertThat(rule.check(day(aVehicle(), stop(anOrder(), anOutlet().window("04:00", "08:00"))))).isEmpty();
        }

        @Test
        void shouldAcceptArrivalExactlyAtWindowClose() {
            assertThat(rule.check(day(aVehicle(), stop(anOrder(), anOutlet().window("04:00", "04:10"))))).isEmpty();
        }

        @Test
        void shouldRejectArrivalAfterTheWindowCloses() {
            assertThat(violationOf(rule, day(aVehicle(), stop(anOrder(), anOutlet().window("04:00", "04:05")))).rule())
                    .isEqualTo(RuleCode.WINDOW);
        }

        @Test
        void shouldAcceptEarlyArrivalBecauseTheTruckWaitsForTheWindowToOpen() {
            assertThat(rule.check(day(aVehicle(), stop(anOrder(), anOutlet().window("05:00", "08:00"))))).isEmpty();
        }

        @Test
        void shouldRejectWhenWaitingForAnEarlyOutletMakesALaterStopLate() {
            // the first stop opens at 6:00, so the second stop is reached at 6:15, after its 6:10 close
            VehicleDay waiting = day(aVehicle(), stop(anOrder(), anOutlet().window("06:00", "06:05")),
                    stop(anOrder().ref("B"), anOutlet().id("O2").window("04:00", "06:10")));

            assertThat(violationOf(rule, waiting).rule()).isEqualTo(RuleCode.WINDOW);
        }

        @Test
        void shouldUseTheMallWindowInsteadOfTheNormalWindow() {
            OutletBuilder mall = anOutlet().window("06:00", "07:00").mallWindow("04:00", "05:00");

            assertThat(rule.check(day(aVehicle(), stop(anOrder(), mall)))).isEmpty();
        }

        @Test
        void shouldRejectArrivalOutsideTheMallWindowEvenInsideTheNormalWindow() {
            OutletBuilder mall = anOutlet().window("04:00", "08:00").mallWindow("03:00", "04:00");

            assertThat(violationOf(rule, day(aVehicle(), stop(anOrder(), mall))).rule()).isEqualTo(RuleCode.WINDOW);
        }

        @Test
        void shouldJudgeLaterStopsByTheirOwnLaterArrival() {
            // second stop is reached at 4:25
            VehicleDay twoStops = day(aVehicle(), stop(anOrder(), anOutlet().window("04:00", "08:00")),
                    stop(anOrder().ref("B"), anOutlet().id("O2").window("04:00", "04:20")));

            assertThat(violationOf(rule, twoStops).rule()).isEqualTo(RuleCode.WINDOW);
        }
    }

    @Nested
    class Checker {
        private final RuleChecker checker = RuleChecker.standard(CALCULATOR);

        @Test
        void shouldReportNothingForAFeasibleDay() {
            assertThat(checker.firstViolation(day(aVehicle(), plainStop()))).isEmpty();
            assertThat(checker.violations(day(aVehicle(), plainStop()))).isEmpty();
        }

        @Test
        void shouldReturnTheFirstViolationInRuleOrder() {
            VehicleDay broken = day(aVehicle().capacity("10", "10"), stop(anOrder().chilled().weightKg("500"), anOutlet()));

            assertThat(checker.firstViolation(broken)).get().extracting(RuleViolation::rule)
                    .isEqualTo(RuleCode.FRIDGE_REQUIRED);
        }

        @Test
        void shouldListEveryViolationForVerification() {
            VehicleDay broken = day(aVehicle().capacity("10", "10"), stop(anOrder().chilled().weightKg("500"), anOutlet()));

            assertThat(checker.violations(broken)).extracting(RuleViolation::rule)
                    .containsExactly(RuleCode.FRIDGE_REQUIRED, RuleCode.OVER_WEIGHT);
        }
    }
}
