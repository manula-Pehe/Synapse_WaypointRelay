import { useQuery } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useOrderDetail(runDate: string, depot: string, orderId: string | null) {
  return useQuery({
    queryKey: [...dispatchKeys.orders(runDate, depot), 'detail', orderId],
    queryFn: () => dispatchApi.order(orderId!),
    enabled: orderId !== null,
  })
}
