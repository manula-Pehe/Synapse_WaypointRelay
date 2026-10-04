import { useMutation, useQueryClient } from '@tanstack/react-query'
import { dispatchApi } from './api'
import { dispatchKeys } from './queryKeys'

export interface PhoneInLine { temp: 'CHILLED' | 'AMBIENT'; units: number }
export interface PhoneInDraft { outletId: string; note: string; lines: PhoneInLine[] }

/** Thrown when some lines were created before one failed, so the form can keep only the rest. */
export class PhoneInPartialError extends Error {
  constructor(readonly createdLines: number, message: string) { super(message) }
}

export function usePhoneIn(runDate: string, depot: string) {
  const client = useQueryClient()
  const invalidate = () => client.invalidateQueries({ queryKey: dispatchKeys.orders(runDate, depot) })
  return useMutation({
    mutationFn: async (draft: PhoneInDraft) => {
      let created = 0
      for (const line of draft.lines) {
        try {
          await dispatchApi.phoneIn({ outletId: draft.outletId, runDate, temp: line.temp, units: line.units, note: draft.note })
          created++
        } catch (error) {
          await invalidate()
          const reason = error instanceof Error ? error.message : 'Request failed'
          throw new PhoneInPartialError(created, created > 0 ? `${created} of ${draft.lines.length} lines were created. ${reason}` : reason)
        }
      }
      return created
    },
    onSuccess: invalidate,
  })
}
