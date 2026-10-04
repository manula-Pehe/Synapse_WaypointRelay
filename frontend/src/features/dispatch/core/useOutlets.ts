import { useQuery } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useOutlets(depot: string) {
  return useQuery({ queryKey: dispatchKeys.outlets(depot), queryFn: () => dispatchApi.outlets(depot) })
}
