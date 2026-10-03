package com.synapse.waypoint.core.reference.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.TravelDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;

/**
 * Read-only reference data for other modules (planning, store, dispatch): outlets, vehicles and
 * their availability, travel times and service times. A blank depot means every depot; depots are
 * matched ignoring case. Lookups that must succeed throw {@code NotFoundException}.
 */
public interface ReferenceService {

    OutletDto outlet(String id);

    List<OutletDto> outlets(String depot);

    /** Outlets of a depot ({@code null} depot = all) and brand ({@code null} brand = all), by id. */
    List<OutletDto> outlets(String depot, String brand);

    /** The vehicle with its availability on the current run date. */
    VehicleDto vehicle(String id);

    /** Every vehicle of the depot with its availability on the run date, by id. */
    List<VehicleDto> vehicles(LocalDate runDate, String depot);

    /** Only the vehicles that can be planned on the run date. */
    List<VehicleDto> availableVehicles(LocalDate runDate, String depot);

    TravelDto travel(String district, String depot);

    /** Minutes spent at a stop of this brand and dock type. */
    int serviceMinutes(String brand, String dockType);

    /** Litres the vehicle has used in the ISO week; zero when nothing is recorded. */
    BigDecimal fuelUsed(String vehicleId, int isoYear, int isoWeek);
}
