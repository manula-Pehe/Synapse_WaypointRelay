package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;
import com.synapse.waypoint.planning.domain.ParkingConstraint;
import com.synapse.waypoint.planning.domain.Temperature;
import com.synapse.waypoint.planning.domain.VehicleTemperature;
import com.synapse.waypoint.planning.domain.VehicleType;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.OutletInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.ServiceKey;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;

/**
 * Builders for invented planning data. Defaults: one depot "Depot-X", one district "North" with
 * 40 min outbound, 5 min and 2 km between stops, 20 km to the district and 10 min service per stop.
 */
public final class PlanningTestData {

    public static final String DEPOT = "Depot-X";
    public static final String DISTRICT = "North";
    public static final LocalDate RUN_DATE = LocalDate.of(2030, 1, 10);
    public static final int SERVICE_MINUTES = 10;

    private PlanningTestData() {
    }

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public static OutletBuilder anOutlet() {
        return new OutletBuilder();
    }

    public static VehicleBuilder aVehicle() {
        return new VehicleBuilder();
    }

    public static InputBuilder anInput() {
        return new InputBuilder();
    }

    public static StopCandidate aStop(OrderBuilder order, OutletBuilder outlet) {
        return new StopCandidate(order.outlet(outlet.id).build(), outlet.build());
    }

    public static TravelInput northTravel() {
        return new TravelInput(DISTRICT, new BigDecimal("20"), 40, new BigDecimal("2"), 5);
    }

    public static final class OrderBuilder {
        private String ref = "ORD-1";
        private String outletId = "OUT-A";
        private Brand brand = Brand.FRESH;
        private Temperature temperature = Temperature.AMBIENT;
        private BigDecimal weightKg = new BigDecimal("100");
        private BigDecimal volumeM3 = BigDecimal.ONE;
        private int daysSinceLastServed = 1;
        private boolean deferredYesterday;

        public OrderBuilder ref(String value) {
            ref = value;
            return this;
        }

        public OrderBuilder outlet(String value) {
            outletId = value;
            return this;
        }

        public OrderBuilder brand(Brand value) {
            brand = value;
            return this;
        }

        public OrderBuilder chilled() {
            temperature = Temperature.CHILLED;
            return this;
        }

        public OrderBuilder weightKg(String value) {
            weightKg = new BigDecimal(value);
            return this;
        }

        public OrderBuilder volumeM3(String value) {
            volumeM3 = new BigDecimal(value);
            return this;
        }

        public OrderBuilder daysSinceLastServed(int value) {
            daysSinceLastServed = value;
            return this;
        }

        public OrderBuilder deferredYesterday() {
            deferredYesterday = true;
            return this;
        }

        public OrderInput build() {
            return new OrderInput("id-" + ref, ref, outletId, brand, temperature, weightKg, volumeM3,
                    daysSinceLastServed, deferredYesterday);
        }
    }

    public static final class OutletBuilder {
        private String id = "OUT-A";
        private String depot = DEPOT;
        private String district = DISTRICT;
        private DockType dockType = DockType.REAR_DOCK;
        private ParkingConstraint parking = ParkingConstraint.NORMAL;
        private LocalTime open = LocalTime.of(4, 0);
        private LocalTime close = LocalTime.of(8, 0);
        private LocalTime mallOpen;
        private LocalTime mallClose;

        public OutletBuilder id(String value) {
            id = value;
            return this;
        }

        public OutletBuilder depot(String value) {
            depot = value;
            return this;
        }

        public OutletBuilder district(String value) {
            district = value;
            return this;
        }

        public OutletBuilder dockType(DockType value) {
            dockType = value;
            return this;
        }

        public OutletBuilder vanOnly() {
            parking = ParkingConstraint.VAN_ONLY;
            return this;
        }

        public OutletBuilder window(String openTime, String closeTime) {
            open = LocalTime.parse(openTime);
            close = LocalTime.parse(closeTime);
            return this;
        }

        public OutletBuilder mallWindow(String openTime, String closeTime) {
            mallOpen = LocalTime.parse(openTime);
            mallClose = LocalTime.parse(closeTime);
            return this;
        }

        public OutletInput build() {
            return new OutletInput(id, depot, district, dockType, parking, open, close, mallOpen, mallClose);
        }
    }

    public static final class VehicleBuilder {
        private String id = "VEH-1";
        private String depot = DEPOT;
        private VehicleType type = VehicleType.TRUCK;
        private VehicleTemperature temperature = VehicleTemperature.AMBIENT;
        private BigDecimal weightCapKg = new BigDecimal("1000");
        private BigDecimal volumeCapM3 = new BigDecimal("10");
        private BigDecimal kmPerLitre = new BigDecimal("5");
        private BigDecimal weeklyQuotaLitres = new BigDecimal("500");
        private BigDecimal fuelUsedLitres = BigDecimal.ZERO;

        public VehicleBuilder id(String value) {
            id = value;
            return this;
        }

        public VehicleBuilder depot(String value) {
            depot = value;
            return this;
        }

        public VehicleBuilder van() {
            type = VehicleType.VAN;
            return this;
        }

        public VehicleBuilder reefer() {
            temperature = VehicleTemperature.REEFER;
            return this;
        }

        public VehicleBuilder capacity(String weightKg, String volumeM3) {
            weightCapKg = new BigDecimal(weightKg);
            volumeCapM3 = new BigDecimal(volumeM3);
            return this;
        }

        public VehicleBuilder kmPerLitre(String value) {
            kmPerLitre = new BigDecimal(value);
            return this;
        }

        public VehicleBuilder weeklyQuotaLitres(String value) {
            weeklyQuotaLitres = new BigDecimal(value);
            return this;
        }

        public VehicleBuilder fuelUsedLitres(String value) {
            fuelUsedLitres = new BigDecimal(value);
            return this;
        }

        public VehicleInput build() {
            return new VehicleInput(id, depot, type, temperature, weightCapKg, volumeCapM3, kmPerLitre,
                    weeklyQuotaLitres, fuelUsedLitres);
        }
    }

    public static final class InputBuilder {
        private final List<OrderInput> orders = new ArrayList<>();
        private final Map<String, OutletInput> outlets = new LinkedHashMap<>();
        private final List<VehicleInput> vehicles = new ArrayList<>();
        private final Map<String, TravelInput> travel = new HashMap<>(Map.of(DISTRICT, northTravel()));
        private final Map<ServiceKey, Integer> service = new HashMap<>();

        private InputBuilder() {
            for (Brand brand : EnumSet.allOf(Brand.class)) {
                for (DockType dock : EnumSet.allOf(DockType.class)) {
                    service.put(new ServiceKey(brand, dock), SERVICE_MINUTES);
                }
            }
        }

        public InputBuilder order(OrderBuilder order) {
            orders.add(order.build());
            return this;
        }

        public InputBuilder outlet(OutletBuilder outlet) {
            OutletInput built = outlet.build();
            outlets.put(built.id(), built);
            return this;
        }

        public InputBuilder vehicle(VehicleBuilder vehicle) {
            vehicles.add(vehicle.build());
            return this;
        }

        public InputBuilder travel(TravelInput value) {
            travel.put(value.district(), value);
            return this;
        }

        public InputBuilder serviceMinutes(Brand brand, DockType dock, int minutes) {
            service.put(new ServiceKey(brand, dock), minutes);
            return this;
        }

        public PlanningInput build() {
            return new PlanningInput(RUN_DATE, DEPOT, orders, outlets, vehicles, travel, service);
        }
    }
}
