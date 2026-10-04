import { useQuery } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useFleet(runDate: string, depot: string) {
  return useQuery({ queryKey: dispatchKeys.fleet(runDate, depot), queryFn: () => dispatchApi.fleet(runDate, depot) })
}
