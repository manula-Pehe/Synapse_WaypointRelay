// Sample data in the exact shapes of docs/api.md (§2–§4).
// All numbers are MADE UP - never copy values from the competition dataset.
// Replace with real API calls once the endpoints are merged.

export const settings = {
  runDate: '2026-10-01',
  now: '2026-09-30T14:00:00+05:30',
  timezone: 'Asia/Colombo',
}

export const outlets = [
  { id: 'OUT001', brand: 'Fresh', district: 'Colombo', depot: 'Peliyagoda', dockType: 'rear_dock', parkingConstraint: 'normal', windowOpen: '05:15', windowClose: '07:45', mallWindowOpen: null, mallWindowClose: null },
  { id: 'OUT003', brand: 'Fresh', district: 'Gampaha', depot: 'Peliyagoda', dockType: 'rear_dock', parkingConstraint: 'van_only', windowOpen: '04:45', windowClose: '07:15', mallWindowOpen: null, mallWindowClose: null },
  { id: 'OUT008', brand: 'Fresh', district: 'Kalutara', depot: 'Peliyagoda', dockType: 'rear_dock', parkingConstraint: 'normal', windowOpen: '05:30', windowClose: '07:50', mallWindowOpen: null, mallWindowClose: null },
  { id: 'OUT017', brand: 'Style', district: 'Colombo', depot: 'Peliyagoda', dockType: 'rear_dock', parkingConstraint: 'normal', windowOpen: '09:00', windowClose: '18:00', mallWindowOpen: '07:00', mallWindowClose: '09:00' },
]

export const vehicles = [
  { id: 'VEH036', type: 'van', temp: 'reefer', weightCapKg: 1000, volumeCapM3: 6, fuelType: 'diesel', kmPerL: 9, weeklyFuelQuotaL: 300, depot: 'Peliyagoda', availability: 'AVAILABLE', availabilityReason: null },
  { id: 'VEH003', type: 'truck', temp: 'reefer', weightCapKg: 5000, volumeCapM3: 25, fuelType: 'diesel', kmPerL: 5, weeklyFuelQuotaL: 500, depot: 'Peliyagoda', availability: 'AVAILABLE', availabilityReason: null },
  { id: 'VEH001', type: 'truck', temp: 'reefer', weightCapKg: 5000, volumeCapM3: 25, fuelType: 'diesel', kmPerL: 5, weeklyFuelQuotaL: 500, depot: 'Peliyagoda', availability: 'IN_WORKSHOP', availabilityReason: 'Scheduled service' },
]

export const orders = [
  { id: 'ord-1', ref: 'S1-000', outletId: 'OUT001', outletName: 'OUT001 · Colombo', brand: 'Fresh', temp: 'AMBIENT', units: 18, weightKg: 90, volumeM3: 0.4, runDate: '2026-10-01', status: 'PREPARED', source: 'SEED', autoConfirm: true, daysSinceLastServed: 1, deferredYesterday: false, parentOrderId: null, storeChecked: true, confirmedAt: null, updatedAt: '2026-09-30T06:00:00+05:30' },
  { id: 'ord-2', ref: 'S1-001', outletId: 'OUT001', outletName: 'OUT001 · Colombo', brand: 'Fresh', temp: 'CHILLED', units: 46, weightKg: 330, volumeM3: 1.8, runDate: '2026-10-01', status: 'PREPARED', source: 'SEED', autoConfirm: false, daysSinceLastServed: 3, deferredYesterday: false, parentOrderId: null, storeChecked: true, confirmedAt: null, updatedAt: '2026-09-30T06:00:00+05:30' },
  { id: 'ord-3', ref: 'S1-014', outletId: 'OUT008', outletName: 'OUT008 · Kalutara', brand: 'Fresh', temp: 'CHILLED', units: 86, weightKg: 600, volumeM3: 3.5, runDate: '2026-10-01', status: 'CONFIRMED', source: 'SEED', autoConfirm: false, daysSinceLastServed: 1, deferredYesterday: false, parentOrderId: null, storeChecked: true, confirmedAt: '2026-09-30T11:10:00+05:30', updatedAt: '2026-09-30T11:10:00+05:30' },
]

export const orderHistory = [
  { at: '2026-09-30T06:00:00+05:30', actor: null, type: 'PREPARED', fromStatus: null, toStatus: 'PREPARED', details: {} },
  { at: '2026-09-30T14:10:00+05:30', actor: 'Dilani J.', type: 'EDITED', fromStatus: 'PREPARED', toStatus: 'PREPARED', details: { units: { from: 15, to: 60 } } },
]

export const closeStatus = { closed: false, closedAt: null, closedBy: null }
