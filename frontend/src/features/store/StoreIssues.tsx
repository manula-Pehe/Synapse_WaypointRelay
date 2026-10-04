import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { storeApi, type Issue } from './api'
import { Button, Card, Feedback, Heading, Loading } from './StoreShared'

export function IssueStatus({ status }: { status: Issue['status'] }) { return <span className={`rounded-full px-3 py-1 text-sm font-semibold ${status === 'RESOLVED' ? 'bg-success-soft text-success' : status === 'ANSWERED' ? 'bg-brand-soft text-brand' : 'bg-warning-soft text-warning'}`}>{status === 'RESOLVED' ? '✓' : status === 'ANSWERED' ? '↩' : '◷'} {status.toLowerCase()}</span> }
export function IssuePhoto({ issueId, photoId, dispatch = false }: { issueId: string; photoId: string; dispatch?: boolean }) {
  const [error, setError] = useState('')
  async function open() {
    const tab = window.open('', '_blank', 'noopener,noreferrer')
    try {
      const blob = await (dispatch ? storeApi.dispatchIssuePhoto(issueId, photoId) : storeApi.issuePhoto(issueId, photoId))
      const url = URL.createObjectURL(blob)
      if (tab) tab.location.href = url
      else { const anchor = document.createElement('a'); anchor.href = url; anchor.target = '_blank'; anchor.click() }
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch { tab?.close(); setError('Could not open photo.') }
  }
  return <><button onClick={() => void open()} className="flex min-h-12 items-center text-brand underline">View photo</button>{error && <span role="alert" className="text-danger">{error}</span>}</>
}
export function StoreIssues() {
  const [filter, setFilter] = useState<'OPEN' | 'RESOLVED' | 'ALL'>('OPEN')
  const query = useQuery({ queryKey: ['store', 'issues'], queryFn: storeApi.issues, refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const all = query.data.items
  const shown = all.filter(issue => filter === 'ALL' || (filter === 'OPEN' ? issue.status !== 'RESOLVED' : issue.status === 'RESOLVED'))
  return <><Heading title="Issues" subtitle="Problems you reported and replies from dispatch" action={<Link to="/store/issues/new" className="flex min-h-12 items-center rounded-lg bg-brand px-5 font-semibold text-on-brand">+ Report problem</Link>} /><div className="mb-4 flex gap-2">{(['OPEN','RESOLVED','ALL'] as const).map(value => <button key={value} onClick={() => setFilter(value)} className={`min-h-12 rounded-full px-4 text-sm font-semibold ${filter === value ? 'bg-brand text-white' : 'border border-line bg-white'}`}>{value === 'OPEN' ? `Open ${all.filter(issue => issue.status !== 'RESOLVED').length}` : value === 'RESOLVED' ? `Resolved ${all.filter(issue => issue.status === 'RESOLVED').length}` : 'All'}</button>)}</div>
    <div className="space-y-3">{shown.length ? shown.map(issue => <Link key={issue.id} to={`/store/issues/${issue.id}`} className="block rounded-xl border border-line bg-surface p-5 hover:border-brand"><div className="flex flex-wrap justify-between gap-2"><div><p className="font-bold">{issue.ref} · {issue.type.replaceAll('_', ' ').toLowerCase()}</p><p className="mt-1 text-sm text-muted">{issue.orderId ? `Order ${issue.orderId.slice(0, 8)} · ` : ''}{new Date(issue.createdAt).toLocaleString()}</p></div><IssueStatus status={issue.status} /></div></Link>) : <Card>No issues in this section.</Card>}</div>
  </>
}
export function StoreIssueDetail() {
  const { id = '' } = useParams()
  const location = useLocation()
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['store', 'issue', id], queryFn: () => storeApi.issue(id), refetchInterval: 15_000, refetchOnWindowFocus: 'always', refetchOnReconnect: 'always' })
  const [text, setText] = useState('')
  const mutation = useMutation({ mutationFn: () => storeApi.issueMessage(id, text), onSuccess: async () => { setText(''); await client.invalidateQueries({ queryKey: ['store', 'issue', id] }); await client.invalidateQueries({ queryKey: ['store', 'issues'] }) } })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const issue = query.data
  return <>{Boolean(location.state?.created) && <div role="status" className="review-success"><strong>✓ Thanks — dispatch has your report</strong><span>{issue.ref} · You will see replies in this conversation.</span></div>}<Link to="/store/issues" className="mb-4 inline-flex min-h-12 items-center font-semibold text-brand">← Issues</Link><Heading title={issue.ref} subtitle={issue.type.replaceAll('_',' ').toLowerCase()} action={<IssueStatus status={issue.status} />} />{Boolean(location.state?.photoFailed) && <p role="alert" className="mb-4 rounded-lg bg-warning-soft p-3 text-warning">Issue saved, but one or more photos did not upload.</p>}<Card><p><strong>Requested:</strong> {issue.wants.toLowerCase()}</p>{issue.units && <p className="mt-2"><strong>Cases:</strong> {issue.units}</p>}{issue.photoIds.length > 0 && <div className="mt-4 flex flex-wrap gap-3">{issue.photoIds.map(photo => <IssuePhoto key={photo} issueId={id} photoId={photo} />)}</div>}</Card><h2 className="mb-3 mt-8 text-xl font-bold">Conversation</h2><div className="space-y-3">{issue.messages.map(message => <Card key={message.id}><p className="text-sm text-muted">{message.authorName} · {new Date(message.createdAt).toLocaleString()}</p><p className="mt-2 whitespace-pre-wrap">{message.text}</p></Card>)}</div><Feedback error={mutation.error} />{issue.status !== 'RESOLVED' && <Card className="mt-5"><form onSubmit={e => { e.preventDefault(); mutation.mutate() }}><label className="font-semibold">Reply<textarea required value={text} onChange={e => setText(e.target.value)} maxLength={2000} className="mt-2 block min-h-24 w-full rounded-lg border border-line p-3" /></label><div className="mt-3"><Button type="submit" disabled={mutation.isPending || !text.trim()}>Send reply</Button></div></form></Card>}</>
}
