import { Spinner } from '../../../ui/components'
import { errorMessage } from './errorMessage'

export function LoadingNote({ label }: { label: string }) {
  return <div className="rounded-xl border border-line bg-surface p-6"><Spinner label={label} /></div>
}

export function ErrorNote({ title, error }: { title: string; error: unknown }) {
  return <div role="alert" className="rounded-xl border border-status-failed bg-status-failed-soft p-5 text-status-failed">
    <p className="font-bold">✕ {title}</p>
    <p className="mt-1 text-sm">{errorMessage(error)}</p>
  </div>
}
