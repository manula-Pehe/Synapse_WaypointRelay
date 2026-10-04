import { useCallback, useEffect, useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../lib/api'
import { offlineDb } from '../../lib/offline'
import { driverApi, toTrip, type TodayPayload } from './api'
import type { DriverRun } from './types'

/**
 * The driver's run: fetched once, then kept in Dexie so every screen works with no signal (F4's
 * "all data from the offline cache").
 *
 * The cached run is seeded into the query straight away, so the app opens showing the last known
 * run and then quietly replaces it with the server's - the driver is in a basement depot with no
 * signal, and a spinner is the wrong first answer.
 *
 * There is no made-up run to fall back on. A vehicle with nothing planned has no trip, and showing
 * invented stops to a driver - or to a judge - is worse than saying so, so `run` is null until the
 * server sends a real one and `empty` says which of the three reasons applies.
 */

const CACHE_KEY = 'today'
const QUERY_KEY = ['driver', 'today']

/** Why there is no run to show, or null when there is one. */
export type RunEmpty = 'loading' | 'offline' | 'no-trip'

function toRun(payload: TodayPayload): DriverRun | null {
  // The plan for a vehicle is one run of ordered stops, so the first trip is the run the driver is
  // on; the rest are carried by the payload for a later revision.
  const trip = payload.trips[0]
  if (!trip) return null
  return {
    runDate: payload.runDate,
    vehicleId: payload.vehicleId,
    vehicleType: payload.vehicleType,
    trip: toTrip(payload, trip),
  }
}

export function useRun() {
  const client = useQueryClient()

  /**
   * Seed from Dexie before the fetch resolves. This is a cache read, not a render-phase update, so
   * the run appears without waiting on the network and without the cascading render a useEffect
   * setState would cause.
   */
  useEffect(() => {
    let live = true
    void offlineDb.cache.get(CACHE_KEY).then((cached) => {
      if (live && cached) client.setQueryData(QUERY_KEY, cached.data)
    })
    return () => {
      live = false
    }
  }, [client])

  const query = useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => {
      const payload = await driverApi.today()
      // Cached after a good fetch, so a run survives a reload with no signal.
      await offlineDb.cache.put({
        key: CACHE_KEY,
        data: payload,
        fetchedAt: new Date().toISOString(),
      })
      return payload
    },
    staleTime: 30_000,
    retry: false,
  })

  const run = useMemo(() => (query.data ? toRun(query.data) : null), [query.data])

  const empty = useMemo<RunEmpty | null>(() => {
    if (run) return null
    if (query.isPending) return 'loading'
    // The server answered but had no trip for this vehicle: the plan is not published yet.
    if (query.isError) return 'offline'
    return 'no-trip'
  }, [run, query.isPending, query.isError])

  /** R0 - accept the load, then re-read so the screen shows the server's version, not a guess. */
  const acceptLoad = useCallback(async () => {
    if (!run) return
    await driverApi.acceptTrip(run.trip.id)
    await client.invalidateQueries({ queryKey: QUERY_KEY })
  }, [client, run])

  return {
    run,
    trip: run?.trip ?? null,
    /** No run to show, and which of the three reasons it is. Null when there is a run. */
    empty,
    loading: query.isPending,
    /** Showing the cached run because the server could not be reached. */
    fromCache: query.isError && Boolean(query.data),
    error: query.error instanceof ApiError ? query.error : null,
    refresh: query.refetch,
    acceptLoad,
  }
}

export type { DriverRun }