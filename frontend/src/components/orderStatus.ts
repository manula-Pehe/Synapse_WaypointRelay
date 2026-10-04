import type { BadgeStatus } from '../ui/components'

export interface StatusStyle { label: string; status: BadgeStatus; icon: string }

export const STATUS_STYLES: Record<string, StatusStyle> = {
  PREPARED: { label: 'Prepared', status: 'risk', icon: '◔' },
  CONFIRMED: { label: 'Confirmed', status: 'delivered', icon: '✓' },
  PLANNED: { label: 'Planned', status: 'way', icon: '◷' },
  LOADED: { label: 'Loaded', status: 'way', icon: '▣' },
  ON_THE_WAY: { label: 'On the way', status: 'way', icon: '➜' },
  DELIVERED: { label: 'Delivered', status: 'delivered', icon: '✓' },
  PARTIAL: { label: 'Partly delivered', status: 'risk', icon: '◐' },
  FAILED: { label: 'Failed', status: 'failed', icon: '✕' },
  MOVED: { label: 'Moved', status: 'deferred', icon: '↷' },
  CANCELLED: { label: 'Cancelled', status: 'offline', icon: '⊘' },
}

export function orderStatusLabel(status: string): string {
  return STATUS_STYLES[status]?.label ?? (status === 'EDITED' ? 'Quantity edited' : status)
}
