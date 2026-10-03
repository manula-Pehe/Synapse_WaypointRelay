package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.service.FleetAvailabilityPolicy.Availability;

/** Vehicle entity to DTO, with the availability for one run date. */
@Component
class VehicleMapper {

    private final FleetAvailabilityPolicy availabilityPolicy;

    VehicleMapper(FleetAvailabilityPolicy availabilityPolicy) {
        this.availabilityPolicy = availabilityPolicy;
    }

    List<VehicleDto> toDtos(List<Vehicle> vehicles, LocalDate runDate) {
        Map<String, Availability> availability = availabilityPolicy.forVehicles(runDate, vehicles);
        return vehicles.stream().map(vehicle -> toDto(vehicle, availability.get(vehicle.getId()))).toList();
    }

    private static VehicleDto toDto(Vehicle vehicle, Availability availability) {
        return new VehicleDto(vehicle.getId(), vehicle.getType(), vehicle.getTemp(), vehicle.getWeightCapKg(),
                vehicle.getVolumeCapM3(), vehicle.getFuelType(), vehicle.getKmPerLitre(),
                vehicle.getWeeklyFuelQuotaLitres(), vehicle.getDepot(), availability.status(), availability.reason());
    }
}
