import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { storeApi } from './api'
import { Feedback } from './StoreShared'
import './new-issue-page.css'

const kinds = [['MISSING','Short'],['DAMAGED','Damaged'],['WRONG_ITEM','Wrong item'],['TEMPERATURE','Temperature'],['OTHER','Other']] as const
export function NewIssuePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const fromReceipt = location.state as { orderId?: string; units?: number; type?: string } | null
  const client = useQueryClient()
  const orders = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const [orderId, setOrderId] = useState(fromReceipt?.orderId ?? '')
  const [type, setType] = useState<string>(kinds.some(([value]) => value === fromReceipt?.type) ? fromReceipt!.type! : 'DAMAGED')
  const [units, setUnits] = useState(fromReceipt?.units && fromReceipt.units > 0 ? fromReceipt.units : 1)
  const [wants, setWants] = useState<'REPLACE' | 'NOTHING'>('REPLACE')
  const [note, setNote] = useState('')
  const [photos, setPhotos] = useState<File[]>([])
  const [photoError, setPhotoError] = useState('')
  const mutation = useMutation({ mutationFn: async () => { const issue = await storeApi.createIssue({ orderId: orderId || null, type, units: type === 'OTHER' ? null : units, wants, note }); const uploads = await Promise.allSettled(photos.map(photo => storeApi.uploadPhoto(issue.id, photo))); return { issue, photoFailed: uploads.some(result => result.status === 'rejected') } }, onSuccess: async ({ issue, photoFailed }) => { await client.invalidateQueries({ queryKey: ['store', 'issues'] }); navigate(`/store/issues/${issue.id}`, { state: { created: true, photoFailed } }) } })
  const valid = note.trim() && !photoError && (type === 'OTHER' || (Number.isInteger(units) && units > 0))
  return <div className="new-issue-page"><Link to="/store/issues" className="issue-back">← Issues</Link><h1>Report a problem</h1><p className="issue-intro">Tell dispatch what happened.</p><div className="new-issue-columns"><form onSubmit={event => { event.preventDefault(); if (valid && !mutation.isPending) mutation.mutate() }} className="new-issue-form"><fieldset><legend>1 · What’s wrong?</legend><div className="issue-options">{kinds.map(([value,label]) => <button key={value} type="button" className={type === value ? 'selected' : ''} onClick={() => setType(value)}>{label}</button>)}</div></fieldset><fieldset><legend>2 · How many cases?</legend><div className="new-order-amount"><button type="button" aria-label="Remove one case" disabled={units <= 1} onClick={() => setUnits(units - 1)}>−</button><input aria-label="Cases affected" type="number" min="1" step="1" value={units} onChange={event => setUnits(Number(event.target.value))} /><button type="button" aria-label="Add one case" onClick={() => setUnits(units + 1)}>＋</button></div></fieldset><fieldset><legend>3 · Add photos</legend><label className="issue-file">＋ Add photo<input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={event => { const files = Array.from(event.target.files ?? []); const invalid = files.some(file => !['image/jpeg','image/png','image/webp'].includes(file.type) || file.size > 1_000_000); setPhotoError(invalid ? 'Choose JPEG, PNG, or WebP photos under 1 MB each.' : ''); setPhotos(invalid ? [] : files) }} /></label>{photos.length > 0 && <span>{photos.length} selected</span>}{photoError && <p role="alert" className="text-danger">{photoError}</p>}</fieldset><fieldset><legend>4 · Describe it</legend><textarea required maxLength={2000} value={note} onChange={event => setNote(event.target.value)} placeholder="What happened?" /></fieldset><fieldset><legend>5 · What would you like?</legend><div className="issue-options"><button type="button" className={wants === 'REPLACE' ? 'selected' : ''} onClick={() => setWants('REPLACE')}>Replace on next delivery</button><button type="button" className={wants === 'NOTHING' ? 'selected' : ''} onClick={() => setWants('NOTHING')}>Just record it</button></div></fieldset><label>Related order<select value={orderId} onChange={event => setOrderId(event.target.value)}><option value="">No order</option>{orders.data?.items.map(order => <option key={order.id} value={order.id}>{order.ref} · {order.runDate}</option>)}</select></label><Feedback error={mutation.error} /><div className="issue-form-actions"><button type="submit" disabled={!valid || mutation.isPending}>{mutation.isPending ? 'Sending…' : '↗ Send report'}</button><Link to="/store/issues">Cancel</Link></div></form><aside className="issue-guidance"><h2>Good to know</h2><p>◷ Dispatch receives your report.</p><p>ⓘ Their reply appears in the issue thread.</p><p>▣ Each photo must be under 1 MB.</p></aside></div></div>
}
