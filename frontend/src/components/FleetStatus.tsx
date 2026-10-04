import React, { useState } from 'react';
import TakeOffRoadDialog from './TakeOffRoadDialog';

export interface VehicleItem {
  id: string;
  type: string;
  capacityKg: number;
  capacityM3: number;
  status: 'Workshop' | 'Available';
  onRoad: boolean;
  fuelPct: number;
  highlighted?: boolean;
}

// Sample values only - invented, not taken from the competition dataset.
const DEFAULT_FLEET_DATA: VehicleItem[] = [
  {
    id: 'VEH001',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Workshop',
    onRoad: false,
    fuelPct: 15,
  },
  {
    id: 'VEH002',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Workshop',
    onRoad: false,
    fuelPct: 10,
  },
  {
    id: 'VEH003',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Available',
    onRoad: true,
    fuelPct: 60,
  },
  {
    id: 'VEH004',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Workshop',
    onRoad: false,
    fuelPct: 20,
  },
  {
    id: 'VEH005',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Workshop',
    onRoad: false,
    fuelPct: 10,
  },
  {
    id: 'VEH006',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Available',
    onRoad: true,
    fuelPct: 65,
  },
  {
    id: 'VEH007',
    type: 'Truck · fridge',
    capacityKg: 4000,
    capacityM3: 20,
    status: 'Available',
    onRoad: true,
    fuelPct: 45,
  },
  {
    id: 'VEH035',
    type: 'Van · fridge',
    capacityKg: 1000,
    capacityM3: 6,
    status: 'Workshop',
    onRoad: false,
    fuelPct: 5,
  },
  {
    id: 'VEH036',
    type: 'Van · fridge',
    capacityKg: 1000,
    capacityM3: 6,
    status: 'Available',
    onRoad: true,
    fuelPct: 30,
    highlighted: true,
  },
  {
    id: 'VEH037',
    type: 'Van · ambient',
    capacityKg: 1200,
    capacityM3: 9,
    status: 'Available',
    onRoad: true,
    fuelPct: 40,
  },
  {
    id: 'VEH011',
    type: 'Truck · ambient',
    capacityKg: 8000,
    capacityM3: 40,
    status: 'Available',
    onRoad: true,
    fuelPct: 50,
  },
];

export interface FleetStatusProps {
  fleet?: VehicleItem[];
  onConfirmFleet?: () => void;
  onToggleOnRoad?: (vehicleId: string, currentVal: boolean) => void;
}

export const FleetStatus: React.FC<FleetStatusProps> = ({
  fleet: initialFleet = DEFAULT_FLEET_DATA,
  onConfirmFleet,
  onToggleOnRoad,
}) => {
  const [vehicles, setVehicles] = useState<VehicleItem[]>(initialFleet);
  const [selectedVehicleForOffRoad, setSelectedVehicleForOffRoad] = useState<VehicleItem | null>(null);

  const handleToggle = (id: string) => {
    const target = vehicles.find((v) => v.id === id);
    if (target && target.onRoad) {
      // Prompt confirmation dialog when taking an active vehicle off the road
      setSelectedVehicleForOffRoad(target);
      return;
    }

    setVehicles((prev) =>
      prev.map((v) => {
        if (v.id === id) {
          const nextOnRoad = !v.onRoad;
          const nextStatus = nextOnRoad ? 'Available' : 'Workshop';
          onToggleOnRoad?.(id, v.onRoad);
          return {
            ...v,
            onRoad: nextOnRoad,
            status: nextStatus,
          };
        }
        return v;
      })
    );
  };

  return (
    <div className="w-full space-y-6 font-sans">
      {/* 1. Amber Warning Banner */}
      <div className="flex flex-col justify-between gap-4 rounded-xl border border-amber-300 bg-amber-50/80 p-5 shadow-2xs md:flex-row md:items-center">
        <div className="flex items-start gap-3.5">
          <div className="mt-0.5 flex-shrink-0 text-amber-700">
            {/* Warning Triangle Icon */}
            <svg
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
              />
            </svg>
          </div>
          <div>
            <h2 className="text-base font-bold text-amber-900">
              Confirm Thursday's fleet before planning
            </h2>
            <p className="mt-1 text-xs text-amber-800/90 sm:text-sm">
              Mark anything that won't run tomorrow. 3 vehicles still show “status unknown”.
            </p>
          </div>
        </div>

        {/* Action Button: Confirm fleet for Thu 1 Oct */}
        <button
          type="button"
          onClick={onConfirmFleet}
          className="inline-flex min-h-[40px] flex-shrink-0 items-center gap-2 rounded-lg bg-[#0e2a47] px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-[#14365b]"
        >
          <svg
            className="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
          >
            <circle cx="12" cy="12" r="9" />
            <path strokeLinecap="round" strokeLinejoin="round" d="m9 12 2 2 4-4" />
          </svg>
          <span>Confirm fleet for Thu 1 Oct</span>
        </button>
      </div>

      {/* 2. Summary Cards (Grid of 4) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: AVAILABLE */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              AVAILABLE
            </span>
            <div className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
              28 of 38
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">10 in the workshop</p>
        </div>

        {/* Card 2: FRIDGE VEHICLES (Amber Text) */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              FRIDGE VEHICLES
            </span>
            <div className="mt-2 text-3xl font-extrabold tracking-tight text-amber-800">
              4 of 9
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">
            VEH003 · VEH006 · VEH007 · VEH036
          </p>
        </div>

        {/* Card 3: VANS */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              VANS
            </span>
            <div className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">
              3 of 4
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">1 fridge van (VEH036)</p>
        </div>

        {/* Card 4: FUEL (Green Text) */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-200/90 bg-white p-5 shadow-2xs">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-slate-500 uppercase">
              FUEL
            </span>
            <div className="mt-2 text-3xl font-extrabold tracking-tight text-emerald-700">
              All OK
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">
            Highest use: 68% of weekly quota
          </p>
        </div>
      </div>

      {/* 3. Fleet Data Table */}
      <div className="overflow-hidden rounded-xl border border-slate-200/90 bg-white shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            {/* Headers */}
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/80 text-xs font-semibold text-slate-500">
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Vehicle</th>
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Type</th>
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Capacity</th>
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Status</th>
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">On road</th>
                <th scope="col" className="px-5 py-3.5 whitespace-nowrap">Weekly fuel</th>
              </tr>
            </thead>

            {/* Rows */}
            <tbody className="divide-y divide-slate-100 text-slate-800">
              {vehicles.map((v) => {
                const isAvailable = v.status === 'Available';
                return (
                  <tr
                    key={v.id}
                    className={`transition-colors hover:bg-slate-50/70 ${
                      v.highlighted ? 'bg-sky-50/30' : ''
                    }`}
                  >
                    {/* Vehicle */}
                    <td className="px-5 py-4 font-bold text-slate-900 whitespace-nowrap">
                      {v.id}
                    </td>

                    {/* Type */}
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {v.type}
                    </td>

                    {/* Capacity */}
                    <td className="px-5 py-4 text-slate-600 whitespace-nowrap">
                      {v.capacityKg.toLocaleString()} kg · {v.capacityM3.toFixed(1)} m³
                    </td>

                    {/* Status Badge */}
                    <td className="px-5 py-4 whitespace-nowrap">
                      {isAvailable ? (
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
                          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <polyline points="20 6 9 17 4 12" />
                          </svg>
                          <span>Available</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-300 bg-slate-100/90 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                          <svg className="h-3.5 w-3.5 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
                          </svg>
                          <span>Workshop</span>
                        </span>
                      )}
                    </td>

                    {/* On road Toggle Switch */}
                    <td className="px-5 py-4 whitespace-nowrap">
                      <button
                        type="button"
                        role="switch"
                        aria-checked={v.onRoad}
                        onClick={() => handleToggle(v.id)}
                        className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border transition-colors focus:outline-none ${
                          v.onRoad
                            ? 'border-teal-600 bg-teal-600'
                            : 'border-slate-300 bg-slate-200'
                        }`}
                      >
                        <span
                          className={`pointer-events-none inline-block h-3.5 w-3.5 transform rounded-full bg-white shadow-xs transition-transform ${
                            v.onRoad ? 'translate-x-4.5' : 'translate-x-0.5'
                          }`}
                        />
                      </button>
                    </td>

                    {/* Weekly Fuel Progress Bar */}
                    <td className="px-5 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <div className="h-1.5 w-24 rounded-full bg-slate-100 overflow-hidden">
                          <div
                            className="h-full rounded-full bg-[#1e40af]"
                            style={{ width: `${v.fuelPct}%` }}
                          />
                        </div>
                        <span className="text-xs text-slate-500 tabular-nums">
                          {v.fuelPct}% used
                        </span>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. Footer */}
      <footer className="text-xs text-slate-500">
        Showing 11 of 38 · fuel percentages are illustrative for the demo week
      </footer>

      {/* 5. Take Off Road Confirmation Dialog */}
      <TakeOffRoadDialog
        isOpen={!!selectedVehicleForOffRoad}
        onClose={() => setSelectedVehicleForOffRoad(null)}
        vehicleId={selectedVehicleForOffRoad?.id}
        vehicleDetails={`${selectedVehicleForOffRoad?.type} · ${selectedVehicleForOffRoad?.capacityKg.toLocaleString()} kg · ${selectedVehicleForOffRoad?.capacityM3.toFixed(1)} m³`}
        onConfirm={() => {
          if (selectedVehicleForOffRoad) {
            setVehicles((prev) =>
              prev.map((v) =>
                v.id === selectedVehicleForOffRoad.id
                  ? { ...v, onRoad: false, status: 'Workshop' }
                  : v
              )
            );
            onToggleOnRoad?.(selectedVehicleForOffRoad.id, true);
          }
        }}
      />
    </div>
  );
};

export default FleetStatus;
