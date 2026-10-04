import { useSyncPillLabel, useSyncStatus } from '../../lib/offline'
import { useDriverTheme } from './theme'
import { t, type Language } from './i18n'

/**
 * The sync badge that sits on every driver screen.
 *
 * It reads from the outbox rather than the network, so it is correct with no signal - which is the
 * whole point: the driver must be able to trust that their work is safe on the phone.
 */
export default function SyncPill({ language }: { language: Language }) {
  const { state, waiting, failed } = useSyncStatus()
  const { key, values } = useSyncPillLabel()
  const { colors } = useDriverTheme()

  const tone =
    state === 'offline' || failed > 0
      ? colors.warn
      : state === 'syncing'
        ? colors.brand
        : waiting > 0
          ? colors.ink2
          : colors.ok

  const icon = state === 'offline' ? '⊘' : state === 'syncing' ? '↻' : waiting > 0 ? '↑' : '✓'

  return (
    <span
      data-testid="sync-pill"
      data-state={state}
      data-waiting={waiting}
      className="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-sm"
      style={{ color: tone, borderColor: tone }}
    >
      <span aria-hidden>{icon}</span>
      {t(language, key as Parameters<typeof t>[1], values)}
    </span>
  )
}