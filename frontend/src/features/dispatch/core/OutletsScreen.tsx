// D13 · Outlets — delivery rules by outlet
import OutletsReference from '../../../components/OutletsReference'
import { useOutlets } from './useOutlets'
import { ErrorNote, LoadingNote } from './QueryNotes'
import { toOutletItems } from './dispatchMappers'

export function OutletsScreen({ depot }: { depot: string }) {
  const outlets = useOutlets(depot)
  if (outlets.isError) return <ErrorNote title="Could not load the outlets" error={outlets.error} />
  if (outlets.isPending) return <LoadingNote label="Loading outlets…" />
  return <OutletsReference outlets={toOutletItems(outlets.data.items)} />
}
