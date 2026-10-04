import { useMutation, useQueryClient } from '@tanstack/react-query'
import { dispatchApi, type Vehicle } from './api'
import { dispatchKeys } from './queryKeys'

export interface VehicleAvailabilityChange { vehicleId: string; status: Vehicle['availability']; reason: string | null }

export function useUpdateVehicle(runDate: string, depot: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (change: VehicleAvailabilityChange) => dispatchApi.setAvailability(change.vehicleId, runDate, change.status, change.reason),
    onSuccess: () => client.invalidateQueries({ queryKey: dispatchKeys.fleet(runDate, depot) }),
  })
}
