// D2 · Fleet status - wires D2v (take off road) and Confirm fleet to the API
import FleetStatus from '../../../components/FleetStatus'
import { useConfirmFleet } from './useConfirmFleet'
import { useFleet } from './useFleet'
import { useUpdateVehicle } from './useUpdateVehicle'
import { ErrorNote, LoadingNote } from './QueryNotes'
import { errorMessage } from './errorMessage'
import { formatDateTime, formatRunDate, offRoadChange, toFleetSummary, toVehicleItems } from './dispatchMappers'

interface Props { runDate: string; depot: string }

export function FleetScreen({ runDate, depot }: Props) {
  const fleet = useFleet(runDate, depot)
  const confirmFleet = useConfirmFleet(runDate, depot)
  const updateVehicle = useUpdateVehicle(runDate, depot)

  if (fleet.isError) return <ErrorNote title="Could not load the fleet" error={fleet.error} />
  if (fleet.isPending) return <LoadingNote label="Loading fleet…" />

  const actionError = confirmFleet.error ?? (updateVehicle.isError ? updateVehicle.error : null)
  return <FleetStatus
    vehicles={toVehicleItems(fleet.data.items)}
    summary={toFleetSummary(fleet.data)}
    runDateLabel={formatRunDate(runDate)}
    depot={depot}
    confirmedLabel={fleet.data.confirmedAt ? formatDateTime(fleet.data.confirmedAt) : null}
    isConfirming={confirmFleet.isPending}
    isUpdating={updateVehicle.isPending}
    errorMessage={actionError ? errorMessage(actionError) : null}
    onConfirmFleet={() => confirmFleet.mutate()}
    onTakeOffRoad={(vehicleId, reason, details) => updateVehicle.mutateAsync({ vehicleId, ...offRoadChange(reason, details) })}
    onPutBack={vehicleId => updateVehicle.mutate({ vehicleId, status: 'AVAILABLE', reason: null })}
  />
}
