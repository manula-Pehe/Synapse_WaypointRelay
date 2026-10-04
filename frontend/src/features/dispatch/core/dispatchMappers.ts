import type { Fleet, Order, OrderEvent, Outlet, UnconfirmedOutlet, Vehicle } from './api'
import type { FleetSummary, VehicleItem } from '../../../components/FleetStatus'
import type { OffRoadReason } from '../../../components/offRoadReasons'
import type { OutletItem } from '../../../components/OutletsReference'
import type { OrderItem, UnconfirmedStore } from '../../../components/OrderQueue'
import type { OutletOption } from '../../../components/AddOrderDrawer'
import type { TimelineEvent, TimelineTone } from '../../../components/OrderHistoryDrawer'
import { orderStatusLabel } from '../../../components/orderStatus'

const SRI_LANKA_TZ = 'Asia/Colombo'
const VAN_ONLY_CONSTRAINT = 'van_only'
const WAITED_FLAG_MIN_DAYS = 1
const UNPLANNABLE_STATUSES = new Set(['PREPARED', 'CANCELLED'])
const SUCCESS_STATUSES = new Set(['CONFIRMED', 'DELIVERED'])
const WARNING_STATUSES = new Set(['EDITED', 'PARTIAL', 'FAILED', 'MOVED', 'CANCELLED'])

/** "05:15" → "5:15 AM" */
export function formatClock(hhmm: string): string {
  const [hours, minutes] = hhmm.split(':').map(Number)
  const suffix = hours >= 12 ? 'PM' : 'AM'
  return `${hours % 12 === 0 ? 12 : hours % 12}:${String(minutes).padStart(2, '0')} ${suffix}`
}

export function formatWindow(outlet: Pick<Outlet, 'windowOpen' | 'windowClose' | 'mallWindowOpen' | 'mallWindowClose'>): string {
  const [open, close] = outlet.mallWindowOpen && outlet.mallWindowClose
    ? [outlet.mallWindowOpen, outlet.mallWindowClose]
    : [outlet.windowOpen, outlet.windowClose]
  return `${formatClock(open)} – ${formatClock(close)}`
}

/** "2026-10-01" → "Thu 1 Oct" (formatting only, never used for business decisions) */
export function formatRunDate(isoDate: string): string {
  return new Intl.DateTimeFormat('en-GB', { timeZone: 'UTC', weekday: 'short', day: 'numeric', month: 'short' }).format(new Date(`${isoDate}T00:00:00Z`))
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('en-US', { timeZone: SRI_LANKA_TZ, weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit', hour12: true }).format(new Date(iso))
}

export interface VehicleCapacity { maxWeightKg: number; maxVolumeM3: number }

export function largestVehicle(vehicles: Vehicle[]): VehicleCapacity {
  return {
    maxWeightKg: Math.max(0, ...vehicles.map(vehicle => vehicle.weightCapKg)),
    maxVolumeM3: Math.max(0, ...vehicles.map(vehicle => vehicle.volumeCapM3)),
  }
}

export function toOrderItems(orders: Order[], outlets: Outlet[], largest: VehicleCapacity): OrderItem[] {
  const outletsById = new Map(outlets.map(outlet => [outlet.id, outlet]))
  return orders
    .filter(order => !UNPLANNABLE_STATUSES.has(order.status))
    .map(order => {
      const outlet = outletsById.get(order.outletId)
      return {
        id: order.id,
        ref: order.ref,
        outletId: order.outletId,
        outlet: order.outletName,
        brand: order.brand,
        cases: order.units,
        kg: order.weightKg,
        volumeM3: order.volumeM3,
        window: outlet ? formatWindow(outlet) : 'Not listed',
        flags: {
          chilled: order.temp === 'CHILLED',
          vanOnly: outlet?.parkingConstraint === VAN_ONLY_CONSTRAINT,
          largerThanVehicle: largest.maxWeightKg > 0 && (order.weightKg > largest.maxWeightKg || order.volumeM3 > largest.maxVolumeM3),
          waitedDays: order.daysSinceLastServed >= WAITED_FLAG_MIN_DAYS ? order.daysSinceLastServed : null,
          skippedYesterday: order.deferredYesterday,
        },
      }
    })
}

export function toUnconfirmedStores(stores: UnconfirmedOutlet[]): UnconfirmedStore[] {
  return stores.map(store => ({
    id: store.outletId,
    outlet: store.outletName,
    phone: store.phone,
    preparedOrder: store.orders.map(order => `${order.temp === 'CHILLED' ? 'Chilled' : 'Ambient'} · ${order.units} cases`).join(', '),
  }))
}

export function toOutletOptions(outlets: Outlet[]): OutletOption[] {
  return outlets.map(outlet => ({ id: outlet.id, label: `${outlet.name} · ${outlet.brand}` }))
}

function timelineTone(type: string): TimelineTone {
  if (SUCCESS_STATUSES.has(type)) return 'success'
  return WARNING_STATUSES.has(type) ? 'warning' : 'brand'
}

export function toTimelineEvents(history: OrderEvent[]): TimelineEvent[] {
  return history.map((event, index) => ({
    id: `${event.at}-${index}`,
    title: orderStatusLabel(event.type),
    timestamp: `${formatDateTime(event.at)} · ${event.actor ?? 'System'}`,
    tone: timelineTone(event.type),
  }))
}

const REEFER = 'reefer'
const VAN = 'van'

const sentenceCase = (value: string) => value.charAt(0).toUpperCase() + value.slice(1)

export function toVehicleItems(vehicles: Vehicle[]): VehicleItem[] {
  return vehicles.map(vehicle => ({
    id: vehicle.id,
    type: `${sentenceCase(vehicle.type)} · ${vehicle.temp === REEFER ? 'fridge' : vehicle.temp}`,
    capacityKg: vehicle.weightCapKg,
    capacityM3: vehicle.volumeCapM3,
    status: vehicle.availability === 'AVAILABLE' ? 'Available' : vehicle.availability === 'IN_WORKSHOP' ? 'Workshop' : 'Off road',
    reason: vehicle.availabilityReason,
    onRoad: vehicle.availability === 'AVAILABLE',
    isFridge: vehicle.temp === REEFER,
    weeklyFuelQuotaL: vehicle.weeklyFuelQuotaL,
  }))
}

export function toFleetSummary(fleet: Fleet): FleetSummary {
  const fridges = fleet.items.filter(vehicle => vehicle.temp === REEFER)
  const vans = fleet.items.filter(vehicle => vehicle.type === VAN)
  const isAvailable = (vehicle: Vehicle) => vehicle.availability === 'AVAILABLE'
  return {
    total: fleet.items.length,
    available: fleet.counts.available,
    workshop: fleet.counts.inWorkshop,
    offRoad: fleet.counts.offRoad,
    fridgeAvailable: fleet.counts.reeferAvailable,
    fridgeTotal: fridges.length,
    fridgeAvailableIds: fridges.filter(isAvailable).map(vehicle => vehicle.id),
    vansAvailable: vans.filter(isAvailable).length,
    vansTotal: vans.length,
  }
}

/** Workshop is its own availability status; every other reason takes the vehicle off the road. */
export function offRoadChange(reason: OffRoadReason, details: string): { status: Vehicle['availability']; reason: string } {
  return {
    status: reason === 'Workshop' ? 'IN_WORKSHOP' : 'OFF_ROAD',
    reason: details ? `${reason}: ${details}` : reason,
  }
}

const humanise = (value: string) => sentenceCase(value.replaceAll('_', ' '))

export function toOutletItems(outlets: Outlet[]): OutletItem[] {
  return outlets.map(outlet => {
    const dock = humanise(outlet.dockType)
    const vanOnly = outlet.parkingConstraint === VAN_ONLY_CONSTRAINT
    return {
      id: outlet.id,
      name: outlet.name,
      district: outlet.district,
      brand: outlet.brand,
      line: `${outlet.brand} · ${dock.toLowerCase()}${vanOnly ? ' · van only' : ''}`,
      dock,
      access: vanOnly ? 'Van only' : 'Trucks and vans',
      depot: outlet.depot,
      deliveryWindow: formatWindow(outlet),
    }
  })
}
