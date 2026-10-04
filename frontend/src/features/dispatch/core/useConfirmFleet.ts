import { useMutation, useQueryClient } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export function useConfirmFleet(runDate: string, depot: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => dispatchApi.confirmFleet(runDate, depot),
    onSuccess: () => client.invalidateQueries({ queryKey: dispatchKeys.fleet(runDate, depot) }),
  })
}
