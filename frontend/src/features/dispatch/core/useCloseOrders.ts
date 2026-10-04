import { useMutation, useQueryClient } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useCloseOrders(runDate: string, depot: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => dispatchApi.closeOrders(runDate, depot),
    onSuccess: () => client.invalidateQueries({ queryKey: dispatchKeys.orders(runDate, depot) }),
  })
}
