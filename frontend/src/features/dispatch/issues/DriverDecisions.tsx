import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  dispatchApi,
  type ConflictPayload,
  type FailedDeliveryPayload,
  type VehicleProblemPayload,
} from '../../driver/api'

/**
 * The dispatcher's decisions on driver data — D8 (sync conflicts), D6f (failed deliveries) and the
 * vehicle problems from F10.
 *
 * They are on one screen on purpose. Each is the same shape of question — a driver has already
 * moved, and dispatch has to say what happens next — so putting them together means a dispatcher
 * works one queue rather than three.
 */

const KEEP_REASONS: Record<string, string> = {
  STOP_ALREADY_DELIVERED: 'the driver already delivered this stop',
  ORDER_MOVED: 'the order was moved to another vehicle',
  STOP_CANCELLED: 'the stop was cancelled after the driver set off',
}

export function DriverDecisions() {
  const client = useQueryClient()
  const [replyTo, setReplyTo] = useState<string | null>(null)
  const [reply, setReply] = useState('')

  const conflicts = useQuery({ queryKey: ['dispatch', 'conflicts'], queryFn: () => dispatchApi.conflicts() })
  const failed = useQuery({ queryKey: ['dispatch', 'failed'], queryFn: () => dispatchApi.failed() })
  const problems = useQuery({ queryKey: ['dispatch', 'problems'], queryFn: () => dispatchApi.problems() })

  const resolve = useMutation({
    mutationFn: ({ id, keepField }: { id: string; keepField: 'KEEP_FIELD' | 'OVERRIDE' }) =>
      dispatchApi.resolveConflict(id, keepField),
    onSuccess: () => client.invalidateQueries({ queryKey: ['dispatch', 'conflicts'] }),
  })

  const decide = useMutation({
    mutationFn: ({ id, decision }: { id: string; decision: FailedDecision }) =>
      dispatchApi.decide(id, decision),
    onSuccess: () => client.invalidateQueries({ queryKey: ['dispatch', 'failed'] }),
  })

  const sendReply = useMutation({
    mutationFn: ({ id, text }: { id: string; text: string }) => dispatchApi.replyProblem(id, text),
    onSuccess: () => {
      setReplyTo(null)
      setReply('')
      return client.invalidateQueries({ queryKey: ['dispatch', 'problems'] })
    },
  })

  const busy = resolve.isPending || decide.isPending || sendReply.isPending

  return (
    <div className="space-y-8">
      <Section
        title="Sync conflicts"
        hint="A driver's offline delivery clashed with a board change. The delivery stands — decide what the board should say."
        count={conflicts.data?.total}
        loading={conflicts.isPending}
        error={conflicts.error}
      >
        {(conflicts.data?.items ?? []).map((conflict) => (
          <ConflictCard
            key={conflict.id}
            conflict={conflict}
            busy={busy}
            onResolve={(keepField) => resolve.mutate({ id: conflict.id, keepField })}
          />
        ))}
        {conflicts.data?.total === 0 && <Empty>No conflicts waiting.</Empty>}
      </Section>

      <Section
        title="Failed deliveries"
        hint="Every failed stop needs a decision before the 2 PM re-plan."
        count={failed.data?.total}
        loading={failed.isPending}
        error={failed.error}
      >
        {(failed.data?.items ?? []).map((delivery) => (
          <FailedCard
            key={delivery.id}
            delivery={delivery}
            busy={busy}
            onDecide={(decision) => decide.mutate({ id: delivery.id, decision })}
          />
        ))}
        {failed.data?.total === 0 && <Empty>No failed deliveries waiting.</Empty>}
      </Section>

      <Section
        title="Vehicle problems"
        hint="Answer on the same thread, so the driver reads it instead of phoning."
        count={problems.data?.total}
        loading={problems.isPending}
        error={problems.error}
      >
        {(problems.data?.items ?? []).map((problem) => (
          <ProblemCard
            key={problem.id}
            problem={problem}
            busy={busy}
            replying={replyTo === problem.id}
            reply={reply}
            onReplyChange={setReply}
            onStartReply={() => setReplyTo(problem.id)}
            onCancelReply={() => setReplyTo(null)}
            onSend={() => sendReply.mutate({ id: problem.id, text: reply })}
          />
        ))}
        {problems.data?.total === 0 && <Empty>No open problems.</Empty>}
      </Section>
    </div>
  )
}

type FailedDecision = 'REPLAN_TOMORROW' | 'TRY_LATER_TODAY' | 'CANCEL'

const DECISIONS: { value: FailedDecision; label: string }[] = [
  { value: 'REPLAN_TOMORROW', label: 'Re-plan tomorrow' },
  { value: 'TRY_LATER_TODAY', label: 'Try later today' },
  { value: 'CANCEL', label: 'Cancel' },
]

function ConflictCard({
  conflict,
  busy,
  onResolve,
}: {
  conflict: ConflictPayload
  busy: boolean
  onResolve: (keepField: 'KEEP_FIELD' | 'OVERRIDE') => void
}) {
  const reason = String(conflict.details?.reason ?? 'UNKNOWN')
  const detail = KEEP_REASONS[reason] ?? reason.replaceAll('_', ' ').toLowerCase()

  return (
    <article className="rounded-xl border border-line bg-surface p-4" data-testid="conflict-card">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <strong className="font-mono text-sm">{conflict.orderId}</strong>
        <span className="text-xs text-muted">{when(conflict.createdAt)}</span>
      </div>
      <p className="mt-2 text-sm">
        Delivered offline, but {detail}. The driver's proof stands; the board needs to catch up.
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          disabled={busy}
          onClick={() => onResolve('KEEP_FIELD')}
          className="min-h-11 rounded-full bg-brand px-4 text-sm font-medium text-on-brand disabled:opacity-50"
        >
          Keep the delivery
        </button>
        <button
          disabled={busy}
          onClick={() => onResolve('OVERRIDE')}
          className="min-h-11 rounded-full border border-line px-4 text-sm disabled:opacity-50"
        >
          Override with the board
        </button>
      </div>
    </article>
  )
}

function FailedCard({
  delivery,
  busy,
  onDecide,
}: {
  delivery: FailedDeliveryPayload
  busy: boolean
  onDecide: (decision: FailedDecision) => void
}) {
  const decided = Boolean(delivery.decision)
  const storeAnswered = Boolean(delivery.storeChoice)

  return (
    <article className="rounded-xl border border-line bg-surface p-4" data-testid="failed-card">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <strong className="font-mono text-sm">{delivery.orderId}</strong>
        <span className="text-xs text-muted">
          {delivery.vehicleId} · {when(delivery.completedAt)}
        </span>
      </div>
      <p className="mt-2 text-sm">
        {delivery.units} cases not delivered
        {delivery.reason ? ` — ${delivery.reason.replaceAll('_', ' ').toLowerCase()}` : ''}.
        {storeAnswered ? ' The store has answered.' : ' The store has not answered yet.'}
      </p>
      {decided ? (
        <p className="mt-3 text-sm text-muted">
          Decided: {String(delivery.decision).replaceAll('_', ' ').toLowerCase()}
        </p>
      ) : (
        <div className="mt-4 flex flex-wrap gap-2">
          {DECISIONS.map((option) => (
            <button
              key={option.value}
              disabled={busy}
              onClick={() => onDecide(option.value)}
              className="min-h-11 rounded-full border border-line px-4 text-sm disabled:opacity-50"
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </article>
  )
}

function ProblemCard({
  problem,
  busy,
  replying,
  reply,
  onReplyChange,
  onStartReply,
  onCancelReply,
  onSend,
}: {
  problem: VehicleProblemPayload
  busy: boolean
  replying: boolean
  reply: string
  onReplyChange: (text: string) => void
  onStartReply: () => void
  onCancelReply: () => void
  onSend: () => void
}) {
  return (
    <article className="rounded-xl border border-line bg-surface p-4" data-testid="problem-card">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <strong className="text-sm">
          {problem.kind.replaceAll('_', ' ').toLowerCase()}
          {problem.fridgeTempC !== null && ` · ${problem.fridgeTempC}°C`}
          {/* The stranded stock is what the re-plan moves, so it reads on the card (D6b). */}
          {problem.unitsOnBoard !== null && ` · ${problem.unitsOnBoard} cases on board`}
        </strong>
        <span className="text-xs text-muted">{when(problem.reportedAt)}</span>
      </div>
      <p className="mt-2 text-sm">
        {problem.canDrive ? 'Driver can still drive.' : 'Driver cannot drive.'}
        {problem.note ? ` ${problem.note}` : ''}
      </p>

      {problem.reply ? (
        <div className="mt-3 rounded-lg bg-canvas p-3">
          <small className="block text-muted">Your instruction</small>
          <p className="mt-1 text-sm">{problem.reply}</p>
        </div>
      ) : replying ? (
        <div className="mt-3">
          <textarea
            value={reply}
            onChange={(event) => onReplyChange(event.target.value)}
            placeholder="What should the driver do?"
            className="min-h-20 w-full rounded-lg border border-line bg-canvas p-3 text-sm"
          />
          <div className="mt-2 flex gap-2">
            <button
              disabled={busy || reply.trim().length === 0}
              onClick={onSend}
              className="min-h-11 rounded-full bg-brand px-4 text-sm font-medium text-on-brand disabled:opacity-50"
            >
              Send
            </button>
            <button
              disabled={busy}
              onClick={onCancelReply}
              className="min-h-11 rounded-full border border-line px-4 text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          disabled={busy}
          onClick={onStartReply}
          className="mt-3 min-h-11 rounded-full border border-line px-4 text-sm disabled:opacity-50"
        >
          Reply
        </button>
      )}
    </article>
  )
}

function Section({
  title,
  hint,
  count,
  loading,
  error,
  children,
}: {
  title: string
  hint: string
  count?: number
  loading: boolean
  error: unknown
  children: React.ReactNode
}) {
  return (
    <section>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-xl font-bold">{title}</h2>
        {count !== undefined && <span className="text-sm text-muted">{count} waiting</span>}
      </div>
      <p className="mt-1 text-sm text-muted">{hint}</p>
      <div className="mt-4 space-y-3">
        {loading ? <Empty>Loading…</Empty> : error ? <Empty>Could not load this list.</Empty> : children}
      </div>
    </section>
  )
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="rounded-xl border border-line bg-surface p-4 text-sm text-muted">{children}</p>
}

function when(iso: string): string {
  return new Date(iso).toLocaleString()
}
