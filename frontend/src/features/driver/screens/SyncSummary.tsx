import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { history, syncNow, useSyncStatus } from '../../../lib/offline'
import type { OutboxRow } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Badge, Button, Card, Label } from '../components'
import { t, type Language } from '../i18n'
import { useDriverTheme } from '../theme'

export interface SyncSummaryProps {
  language: Language
  onToggleTheme: () => void
}

/**
 * the offline screen and the back-online summary.
 *
 * They are the same screen for a reason: US-7.1 is "show me my proof is safe" and US-7.2 is "show me
 * what went". A driver who is offline and a driver who just came back should both see the same list
 * of their own actions, only the headline differs.
 */
export default function SyncSummary({ language, onToggleTheme }: SyncSummaryProps) {
  const snapshot = useSyncStatus()
  const [rows, setRows] = useState<OutboxRow[]>([])
  const navigate = useNavigate()
  const { colors } = useDriverTheme()

  useEffect(() => {
    history().then(setRows)
  }, [snapshot.done, snapshot.waiting, snapshot.state])

  const done = rows.filter((row) => row.status === 'done')
  const waiting = rows.filter((row) => row.status === 'pending' || row.status === 'sent')
  const conflicted = rows.filter((row) => row.result === 'CONFLICT')

  return (
    <DriverLayout
      title={snapshot.state === 'offline' ? t(language, 'driver.sync.title') : t(language, 'driver.sync.backOnline')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate('/driver/stops')}
      action={
        snapshot.state !== 'offline' && (
          <Button full onClick={() => syncNow()} testId="sync-now">
            {t(language, 'driver.sync.syncing')}
          </Button>
        )
      }
    >
      <Card>
        <Label>{t(language, 'driver.sync.summary')}</Label>
        <div className="mt-2 flex flex-wrap gap-2">
          <Badge tone="ok">{done.length} {t(language, 'driver.sync.synced')}</Badge>
          <Badge tone="neutral">{waiting.length} {t(language, 'driver.sync.waiting', { count: waiting.length })}</Badge>
          {conflicted.length > 0 && <Badge tone="danger">{conflicted.length}</Badge>}
        </div>
        {snapshot.lastSyncAt && (
          <p className="mt-3 text-sm" style={{ color: colors.ink2 }}>
            {new Date(snapshot.lastSyncAt).toLocaleTimeString()}
          </p>
        )}
      </Card>

      {rows.length === 0 && <Card>{t(language, 'driver.sync.nothing')}</Card>}

      <ul className="space-y-2">
        {rows.map((row) => (
          <li key={row.clientId}>
            <Card className="flex items-center justify-between gap-3">
              <span className="min-w-0 truncate">{row.type}</span>
              <Badge
                tone={
                  row.result === 'CONFLICT'
                    ? 'danger'
                    : row.status === 'done'
                      ? 'ok'
                      : row.status === 'failed'
                        ? 'warn'
                        : 'neutral'
                }
              >
                {row.result ?? row.status}
              </Badge>
            </Card>
          </li>
        ))}
      </ul>
    </DriverLayout>
  )
}