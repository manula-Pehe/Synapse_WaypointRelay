import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { storeApi, type Issue } from './api'
import { Button, Card, Feedback, Heading, Loading } from './StoreShared'

export function IssueStatus({ status }: { status: Issue['status'] }) { return <span className={`rounded-full px-3 py-1 text-sm font-semibold ${status === 'RESOLVED' ? 'bg-success-soft text-success' : status === 'ANSWERED' ? 'bg-brand-soft text-brand' : 'bg-warning-soft text-warning'}`}>{status === 'RESOLVED' ? '✓' : status === 'ANSWERED' ? '↩' : '◷'} {status.toLowerCase()}</span> }
function IssuePhoto({ issueId, photoId }: { issueId: string; photoId: string }) {
  const [error, setError] = useState('')
  async function open() {
    const tab = window.open('', '_blank', 'noopener,noreferrer')
    try {
      const blob = await storeApi.issuePhoto(issueId, photoId)
      const url = URL.createObjectURL(blob)
      if (tab) tab.location.href = url
      else { const anchor = document.createElement('a'); anchor.href = url; anchor.target = '_blank'; anchor.click() }
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch { tab?.close(); setError('Could not open photo.') }
  }
  return <><button onClick={() => void open()} className="flex min-h-12 items-center text-brand underline">View photo</button>{error && <span role="alert" className="text-danger">{error}</span>}</>
}
export function StoreIssues() {
  const query = useQuery({ queryKey: ['store', 'issues'], queryFn: storeApi.issues })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  return <><Heading title="Issues" subtitle="Reports and replies from dispatch" action={<Link to="/store/issues/new" className="flex min-h-12 items-center rounded-lg bg-brand px-5 font-semibold text-on-brand">+ Report problem</Link>} />
    <div className="space-y-3">{query.data.items.length ? query.data.items.map(issue => <Link key={issue.id} to={`/store/issues/${issue.id}`} className="block rounded-xl border border-line bg-surface p-5 hover:border-brand"><div className="flex flex-wrap justify-between gap-2"><div><p className="font-bold">{issue.ref} · {issue.type.replaceAll('_', ' ').toLowerCase()}</p><p className="mt-1 text-sm text-muted">{issue.orderId ? `Order ${issue.orderId.slice(0, 8)} · ` : ''}{new Date(issue.createdAt).toLocaleString()}</p></div><IssueStatus status={issue.status} /></div></Link>) : <Card>No issues reported.</Card>}</div>
  </>
}
export function NewIssue() {
  const navigate = useNavigate()
  const client = useQueryClient()
  const orders = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const [orderId, setOrderId] = useState('')
  const [type, setType] = useState('DAMAGED')
  const [units, setUnits] = useState(1)
  const [wants, setWants] = useState('REPLACE')
  const [note, setNote] = useState('')
  const [photos, setPhotos] = useState<File[]>([])
  const mutation = useMutation({ mutationFn: async () => {
    const issue = await storeApi.createIssue({ orderId: orderId || null, type, units: type === 'LATE' || type === 'OTHER' ? null : units, wants, note })
    const uploads = await Promise.allSettled(photos.map(photo => storeApi.uploadPhoto(issue.id, photo)))
    return { issue, photoFailed: uploads.some(result => result.status === 'rejected') }
  }, onSuccess: async ({ issue, photoFailed }) => { await client.invalidateQueries({ queryKey: ['store', 'issues'] }); navigate(`/store/issues/${issue.id}`, { state: { photoFailed } }) } })
  return <><Link to="/store/issues" className="mb-4 inline-flex min-h-12 items-center font-semibold text-brand">← Issues</Link><Heading title="Report a problem" subtitle="Tell dispatch what happened" /><Feedback error={mutation.error} /><Card><form onSubmit={e => { e.preventDefault(); mutation.mutate() }} className="grid max-w-xl gap-5"><label className="font-semibold">Order<select value={orderId} onChange={e => setOrderId(e.target.value)} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3"><option value="">No order</option>{orders.data?.items.map(o => <option key={o.id} value={o.id}>{o.ref} · {o.runDate}</option>)}</select></label><label className="font-semibold">Problem<select value={type} onChange={e => setType(e.target.value)} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3">{['DAMAGED','MISSING','WRONG_ITEM','LATE','OTHER'].map(x => <option key={x} value={x}>{x.replaceAll('_',' ').toLowerCase()}</option>)}</select></label>{type !== 'LATE' && type !== 'OTHER' && <label className="font-semibold">Cases affected<input type="number" min="1" value={units} onChange={e => setUnits(Number(e.target.value))} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3" /></label>}<label className="font-semibold">What would help?<select value={wants} onChange={e => setWants(e.target.value)} className="mt-2 block min-h-12 w-full rounded-lg border border-line px-3"><option value="REPLACE">Replace cases</option><option value="CREDIT">Credit</option><option value="NOTHING">No action needed</option></select></label><label className="font-semibold">Details<textarea required maxLength={2000} value={note} onChange={e => setNote(e.target.value)} className="mt-2 block min-h-28 w-full rounded-lg border border-line p-3" /></label><label className="font-semibold">Photos (JPEG, PNG, WebP; up to 1 MB each)<input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={e => setPhotos([...e.target.files ?? []])} className="mt-2 block min-h-12 w-full rounded-lg border border-line p-2" /></label><Button type="submit" disabled={mutation.isPending || !note.trim() || (type !== 'LATE' && type !== 'OTHER' && units < 1)}>{mutation.isPending ? 'Sending…' : 'Send report'}</Button></form></Card></>
}
export function StoreIssueDetail() {
  const { id = '' } = useParams()
  const location = useLocation()
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['store', 'issue', id], queryFn: () => storeApi.issue(id) })
  const [text, setText] = useState('')
  const mutation = useMutation({ mutationFn: () => storeApi.issueMessage(id, text), onSuccess: async () => { setText(''); await client.invalidateQueries({ queryKey: ['store', 'issue', id] }); await client.invalidateQueries({ queryKey: ['store', 'issues'] }) } })
  if (!query.data) return <Loading error={query.error} retry={() => void query.refetch()} />
  const issue = query.data
  return <><Link to="/store/issues" className="mb-4 inline-flex min-h-12 items-center font-semibold text-brand">← Issues</Link><Heading title={issue.ref} subtitle={issue.type.replaceAll('_',' ').toLowerCase()} action={<IssueStatus status={issue.status} />} />{Boolean(location.state?.photoFailed) && <p role="alert" className="mb-4 rounded-lg bg-warning-soft p-3 text-warning">Issue saved, but one or more photos did not upload.</p>}<Card><p><strong>Requested:</strong> {issue.wants.toLowerCase()}</p>{issue.units && <p className="mt-2"><strong>Cases:</strong> {issue.units}</p>}{issue.photoIds.length > 0 && <div className="mt-4 flex flex-wrap gap-3">{issue.photoIds.map(photo => <IssuePhoto key={photo} issueId={id} photoId={photo} />)}</div>}</Card><h2 className="mb-3 mt-8 text-xl font-bold">Conversation</h2><div className="space-y-3">{issue.messages.map(message => <Card key={message.id}><p className="text-sm text-muted">{message.authorName} · {new Date(message.createdAt).toLocaleString()}</p><p className="mt-2 whitespace-pre-wrap">{message.text}</p></Card>)}</div><Feedback error={mutation.error} />{issue.status !== 'RESOLVED' && <Card className="mt-5"><form onSubmit={e => { e.preventDefault(); mutation.mutate() }}><label className="font-semibold">Reply<textarea required value={text} onChange={e => setText(e.target.value)} maxLength={2000} className="mt-2 block min-h-24 w-full rounded-lg border border-line p-3" /></label><div className="mt-3"><Button type="submit" disabled={mutation.isPending || !text.trim()}>Send reply</Button></div></form></Card>}</>
}
