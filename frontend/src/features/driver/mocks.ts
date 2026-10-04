// Sample driver data. Every value here is MADE UP 
// 

import type { DriverStop, DriverTrip } from './types'

function stop(
  id: string,
  sequence: number,
  outletId: string,
  outletName: string,
  orderRef: string,
  cases: number,
  chilled: boolean,
  windowOpen: string,
  windowClose: string,
  predictedArrival: string,
  status: DriverStop['status'] = 'PENDING',
  earlyByMinutes = 0,
  dockType: DriverStop['outlet']['dockType'] = 'street',
  note?: string,
): DriverStop {
  return {
    id,
    sequence,
    outlet: {
      id: outletId,
      name: outletName,
      brand: 'Fresh',
      district: 'Colombo',
      dockType,
      windowOpen,
      windowClose,
      note,
    },
    orderRef,
    cases,
    chilled,
    earlyByMinutes,
    predictedArrival,
    status,
  }
}

export const mockTrip: DriverTrip = {
  id: 'trp-1',
  no: 'TR-104',
  vehicleId: 'VEH036',
  depot: 'Peliyagoda',
  fridgeTempC: 3.4,
  loadedCases: 229,
  loadAccepted: false,
  stops: [
    stop('stp-1', 1, 'OUT001', 'Colombo Fort Grocer', 'S1-000', 10, false, '05:00', '07:30', '06:10', 'DONE'),
    stop('stp-2', 2, 'OUT003', 'Nawinna Fresh Mart', 'S1-001', 60, true, '05:00', '07:30', '06:25', 'DONE'),
    stop('stp-3', 3, 'OUT008', 'Dehiwala City Super', 'S1-014', 90, true, '05:00', '07:30', '06:50', 'ARRIVED'),
    stop('stp-4', 4, 'OUT017', 'Battaramulla Mall Store', 'S1-021', 24, false, '09:00', '18:00', '08:10', 'PENDING', 50, 'mall_bay', 'Ask at the mall desk for the bay pass'),
    stop('stp-5', 5, 'OUT021', 'Moratuwa Cold Store', 'S1-033', 45, true, '05:00', '07:30', '09:05', 'PENDING', 0, 'rear_dock'),
  ],
}

export const mockTrips: DriverTrip[] = [mockTrip]