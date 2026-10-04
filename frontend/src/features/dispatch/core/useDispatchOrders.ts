import { useQuery } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useDispatchOrders(runDate: string, depot: string) {
  return useQuery({ queryKey: [...dispatchKeys.orders(runDate, depot), 'list'], queryFn: () => dispatchApi.orders(runDate, depot) })
}

export function useUnconfirmedOutlets(runDate: string, depot: string) {
  return useQuery({ queryKey: [...dispatchKeys.orders(runDate, depot), 'unconfirmed'], queryFn: () => dispatchApi.unconfirmed(runDate, depot) })
}

export function useCloseStatus(runDate: string, depot: string) {
  return useQuery({ queryKey: [...dispatchKeys.orders(runDate, depot), 'close'], queryFn: () => dispatchApi.closeStatus(runDate, depot) })
}
