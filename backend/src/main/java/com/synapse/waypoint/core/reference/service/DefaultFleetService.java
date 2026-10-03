package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.core.order.entity.OrderRun;
import com.synapse.waypoint.core.order.repository.OrderRunRepository;
import com.synapse.waypoint.core.reference.dto.FleetCountsDto;
import com.synapse.waypoint.core.reference.dto.FleetDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;

@Service
@Transactional
class DefaultFleetService implements FleetService {

    private final ReferenceService reference;
    private final OrderRunRepository runs;
    private final DispatcherDepotScope depotScope;

    DefaultFleetService(ReferenceService reference, OrderRunRepository runs, DispatcherDepotScope depotScope) {
        this.reference = reference;
        this.runs = runs;
        this.depotScope = depotScope;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDto view(LocalDate runDate, String depot) {
        String depotName = depotScope.resolve(depot);
        List<VehicleDto> vehicles = reference.vehicles(runDate, depotName);
        OrderRun run = runs.findByRunDateAndDepot(runDate, depotName).orElse(null);
        return new FleetDto(vehicles, run == null ? null : ApiTimestamp.of(run.getFleetConfirmedAt()),
                run == null ? null : run.getFleetConfirmedBy(), FleetCountsDto.of(vehicles));
    }
}
