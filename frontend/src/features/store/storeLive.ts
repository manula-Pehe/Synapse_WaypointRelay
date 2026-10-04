import { useEffect, useState } from 'react'

const STORE_ZONE = 'Asia/Colombo'

/** Keep display time moving between server responses without trusting the device's date. */
export function useStoreLiveNow(serverNow?: string, updatedAt = 0) {
  const [tick, setTick] = useState(0)
  useEffect(() => {
    const interval = window.setInterval(() => setTick(Date.now()), 1_000)
    return () => window.clearInterval(interval)
  }, [])
  if (!serverNow) return null
  return new Date(Date.parse(serverNow) + Math.max(0, tick - updatedAt))
}

export function storeDateLabel(date: Date) {
  return new Intl.DateTimeFormat('en-GB', { timeZone: STORE_ZONE, weekday: 'short', day: 'numeric', month: 'short' }).format(date).replace('Sept', 'Sep')
}

export function storeLocalDate(date: Date) {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: STORE_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date)
  const value = (type: string) => parts.find(part => part.type === type)?.value ?? ''
  return `${value('year')}-${value('month')}-${value('day')}`
}

export function storeTimeLabel(date: Date) {
  return new Intl.DateTimeFormat('en-US', { timeZone: STORE_ZONE, hour: 'numeric', minute: '2-digit' }).format(date)
}

export function storeWallTimeLabel(value: string) {
  const hour = Number(value.slice(0, 2))
  return `${hour % 12 || 12}:${value.slice(3, 5)} ${hour < 12 ? 'AM' : 'PM'}`
}

export function storeWindowLabel(open: string, close: string) {
  return `${storeWallTimeLabel(open)} – ${storeWallTimeLabel(close)}`
}

export function storeGreeting(date: Date) {
  const hour = Number(new Intl.DateTimeFormat('en-GB', { timeZone: STORE_ZONE, hour: 'numeric', hourCycle: 'h23' }).format(date))
  return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'
}

export function storeCutoffLabel(now: Date, cutOffAt: string, closed: boolean) {
  const time = storeTimeLabel(new Date(cutOffAt))
  if (closed || now.getTime() >= Date.parse(cutOffAt)) return `Orders closed · ${time}`
  const minutes = Math.max(0, Math.ceil((Date.parse(cutOffAt) - now.getTime()) / 60_000))
  return `Orders close in ${Math.floor(minutes / 60)} h ${minutes % 60} min · ${time}`
}
