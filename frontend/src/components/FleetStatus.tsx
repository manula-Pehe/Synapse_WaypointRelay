// D2 · Fleet status  (D2v take off road opens from the "On road" switch)
import React, { useState } from 'react';
import { Badge, Button, EmptyState, Switch } from '../ui/components';
import TakeOffRoadDialog from './TakeOffRoadDialog';
import type { OffRoadReason } from './offRoadReasons';

export type VehicleStatus = 'Available' | 'Workshop' | 'Off road';

export interface VehicleItem {
  id: string;
  type: string;
  capacityKg: number;
  capacityM3: number;
  status: VehicleStatus;
  reason: string | null;
  onRoad: boolean;
  isFridge: boolean;
  weeklyFuelQuotaL: number;
}

export interface FleetSummary {
  total: number;
  available: number;
  workshop: number;
  offRoad: number;
  fridgeAvailable: number;
  fridgeTotal: number;
  fridgeAvailableIds: string[];
  vansAvailable: number;
  vansTotal: number;
}

export interface FleetStatusProps {
  vehicles: VehicleItem[];
  summary: FleetSummary;
  runDateLabel: string;
  depot: string;
  confirmedLabel: string | null;
  isConfirming: boolean;
  isUpdating: boolean;
  errorMessage: string | null;
  onConfirmFleet: () => void;
  onTakeOffRoad: (vehicleId: string, reason: OffRoadReason, details: string) => Promise<unknown>;
  onPutBack: (vehicleId: string) => void;
}

const card = 'flex flex-col justify-between rounded-xl border border-line bg-surface p-5';
const th = 'whitespace-nowrap px-5 py-3.5';
const td = 'whitespace-nowrap px-5 py-4';

function SummaryCard({ title, value, note, tone = 'text-ink' }: { title: string; value: string; note: string; tone?: string }) {
  return (
    <div className={card}>
      <div>
        <span className="text-[11px] font-bold uppercase tracking-wider text-muted">{title}</span>
        <div className={`mt-2 text-3xl font-extrabold tracking-tight ${tone}`}>{value}</div>
      </div>
      <p className="mt-3 text-xs text-muted">{note}</p>
    </div>
  );
}

function StatusBadge({ status }: { status: VehicleStatus }) {
  if (status === 'Available') return <Badge status="delivered" size="s" icon="✓">Available</Badge>;
  if (status === 'Workshop') return <Badge status="offline" size="s" icon="🔧">Workshop</Badge>;
  return <Badge status="failed" size="s" icon="⊘">Off road</Badge>;
}

export const FleetStatus: React.FC<FleetStatusProps> = ({
  vehicles,
  summary,
  runDateLabel,
  depot,
  confirmedLabel,
  isConfirming,
  isUpdating,
  errorMessage,
  onConfirmFleet,
  onTakeOffRoad,
  onPutBack,
}) => {
  const [vehicleToTake, setVehicleToTake] = useState<VehicleItem | null>(null);

  const handleToggle = (vehicle: VehicleItem) => {
    if (vehicle.onRoad) setVehicleToTake(vehicle);
    else onPutBack(vehicle.id);
  };

  const fridgeWarning = vehicleToTake?.isFridge
    ? `This is 1 of only ${summary.fridgeAvailable} available fridge vehicles at ${depot}. More chilled orders may have to wait.`
    : null;

  return (
    <div className="w-full space-y-6">
      {confirmedLabel ? (
        <div role="status" className="flex items-center gap-3.5 rounded-xl border border-status-delivered bg-status-delivered-soft p-5 text-status-delivered">
          <span aria-hidden="true">✓</span>
          <h2 className="text-base font-bold">Fleet for {runDateLabel} confirmed {confirmedLabel}</h2>
        </div>
      ) : null}
      <div className={`flex flex-col justify-between gap-4 rounded-xl border p-5 md:flex-row md:items-center ${confirmedLabel ? 'border-line bg-surface' : 'border-status-risk bg-status-risk-soft text-status-risk'}`}>
        <div className="flex items-start gap-3.5">
          <span aria-hidden="true">{confirmedLabel ? '↻' : '⚠'}</span>
          <div>
            <h2 className="text-base font-bold">{confirmedLabel ? 'Changed something?' : `Confirm ${runDateLabel}'s fleet before planning`}</h2>
            <p className="mt-1 text-xs sm:text-sm">Mark anything that won't run on {runDateLabel}, then confirm. Changes apply to that run only.</p>
          </div>
        </div>
        <Button size="s" disabled={isConfirming} onClick={onConfirmFleet}>
          {isConfirming ? 'Confirming…' : `${confirmedLabel ? 'Confirm again' : 'Confirm fleet'} for ${runDateLabel}`}
        </Button>
      </div>

      {errorMessage && <p role="alert" className="rounded-lg bg-status-failed-soft p-3 text-sm font-semibold text-status-failed">✕ {errorMessage}</p>}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard title="Available" value={`${summary.available} of ${summary.total}`} note={`${summary.workshop} in the workshop`} />
        <SummaryCard title="Fridge vehicles" tone="text-status-chilled" value={`${summary.fridgeAvailable} of ${summary.fridgeTotal}`} note={summary.fridgeAvailableIds.join(' · ') || 'None available'} />
        <SummaryCard title="Vans" value={`${summary.vansAvailable} of ${summary.vansTotal}`} note="Needed for van-only outlets" />
        <SummaryCard title="Off road" tone={summary.offRoad > 0 ? 'text-status-failed' : 'text-ink'} value={String(summary.offRoad)} note="Not used in any plan" />
      </div>

      {vehicles.length === 0 ? (
        <EmptyState title="No vehicles" description={`No vehicles are listed for ${depot}.`} />
      ) : (
        <div className="overflow-hidden rounded-xl border border-line bg-surface">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-inset text-xs font-semibold text-muted">
                  <th scope="col" className={th}>Vehicle</th>
                  <th scope="col" className={th}>Type</th>
                  <th scope="col" className={th}>Capacity</th>
                  <th scope="col" className={th}>Status</th>
                  <th scope="col" className={th}>On road</th>
                  <th scope="col" className={th}>Weekly fuel quota</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line text-ink">
                {vehicles.map((vehicle) => (
                  <tr key={vehicle.id} className="hover:bg-inset">
                    <td className={`${td} font-bold`}>{vehicle.id}</td>
                    <td className={`${td} text-muted`}>{vehicle.type}</td>
                    <td className={`${td} text-muted`}>{vehicle.capacityKg.toLocaleString()} kg · {vehicle.capacityM3.toFixed(1)} m³</td>
                    <td className={td}>
                      <StatusBadge status={vehicle.status} />
                      {vehicle.reason && <span className="ml-2 text-xs text-muted">{vehicle.reason}</span>}
                    </td>
                    <td className={td}>
                      <Switch label={vehicle.onRoad ? 'Yes' : 'No'} checked={vehicle.onRoad} disabled={isUpdating} onChange={() => handleToggle(vehicle)} />
                    </td>
                    <td className={`${td} tabular-nums text-muted`}>{vehicle.weeklyFuelQuotaL.toLocaleString()} L</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <footer className="text-xs text-muted">Showing {vehicles.length} of {summary.total} vehicles at {depot}</footer>

      {vehicleToTake && (
        <TakeOffRoadDialog
          vehicleId={vehicleToTake.id}
          vehicleDetails={`${vehicleToTake.type} · ${vehicleToTake.capacityKg.toLocaleString()} kg · ${vehicleToTake.capacityM3.toFixed(1)} m³`}
          runDateLabel={runDateLabel}
          fridgeWarning={fridgeWarning}
          onConfirm={(reason, details) => onTakeOffRoad(vehicleToTake.id, reason, details)}
          onClose={() => setVehicleToTake(null)}
        />
      )}
    </div>
  );
};

export default FleetStatus;
