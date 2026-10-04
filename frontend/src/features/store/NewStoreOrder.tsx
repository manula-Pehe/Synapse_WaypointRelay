import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { storeApi, type Order } from './api'
import { storeLocalDate, storeWindowLabel, useStoreLiveNow } from './storeLive'
import { Feedback, Loading } from './StoreShared'
import './new-store-order.css'

function shortDate(value: string) { return new Date(`${value}T12:00:00`).toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }) }

export function NewStoreOrder() {
  const navigate = useNavigate()
  const client = useQueryClient()
  const home = useQuery({ queryKey: ['store', 'home'], queryFn: storeApi.home })
  const orders = useQuery({ queryKey: ['store', 'orders'], queryFn: () => storeApi.orders() })
  const outlet = useQuery({ queryKey: ['store', 'outlet', home.data?.outlet], queryFn: () => storeApi.outlet(home.data!.outlet), enabled: !!home.data?.outlet })
  const now = useStoreLiveNow(home.data?.now, home.dataUpdatedAt)
  const [selectedDate, setSelectedDate] = useState('')
  const [temp, setTemp] = useState<'CHILLED' | 'AMBIENT'>('AMBIENT')
  const [units, setUnits] = useState(1)
  const [reason, setReason] = useState('')
  const [note, setNote] = useState('')
  const mutation = useMutation({ mutationFn: () => storeApi.create({ runDate: selectedDate || home.data!.runDate, temp, units, note: [reason, note.trim()].filter(Boolean).join(' · ') }), onSuccess: async (order: Order) => { await client.invalidateQueries({ queryKey: ['store'] }); navigate('/store/orders', { state: { placed: order } }) } })
  if (!home.data || !orders.data) return <Loading error={home.error ?? orders.error} retry={() => { void home.refetch(); void orders.refetch() }} />
  const today = now ? storeLocalDate(now) : ''
  const runDate = selectedDate || home.data.runDate
  const availableDates = [...new Set([home.data.runDate, ...orders.data.items.filter(order => order.runDate >= home.data!.runDate).map(order => order.runDate)])].sort().slice(0, 3)
  const currentRunClosed = home.data.ordersClosed && runDate === home.data.runDate
  const windowText = outlet.data ? storeWindowLabel(outlet.data.windowOpen, outlet.data.windowClose) : null
  return <div className="new-order-page">
    <div className="new-order-mobile-heading"><h1>New order</h1><p>Extra or one-off delivery</p></div>
    <div className="new-order-columns"><form className="new-order-form" onSubmit={event => { event.preventDefault(); if (!mutation.isPending && Number.isInteger(units) && units > 0 && !currentRunClosed) mutation.mutate() }}>
      <fieldset><legend>1 · What do you need?</legend><div className="new-order-options"><label className={temp === 'AMBIENT' ? 'selected' : ''}><input type="radio" name="temperature" checked={temp === 'AMBIENT'} onChange={() => setTemp('AMBIENT')} /><img src="/store-icons/box.svg" alt="" width="22" height="22" /><span><strong>Fresh dry goods</strong><small>Ambient · any vehicle</small></span></label><label className={temp === 'CHILLED' ? 'selected' : ''}><input type="radio" name="temperature" checked={temp === 'CHILLED'} onChange={() => setTemp('CHILLED')} /><img src="/store-icons/snow.svg" alt="" width="22" height="22" /><span><strong>Fresh chilled</strong><small>Needs a fridge truck</small></span></label></div></fieldset>
      <fieldset><legend>2 · Which delivery?</legend><div className="new-order-options new-order-days">{availableDates.map(date => <button key={date} type="button" className={runDate === date ? 'selected' : ''} onClick={() => setSelectedDate(date)}>{shortDate(date)}</button>)}<label className="new-order-date">Pick another date<input aria-label="Pick another delivery date" type="date" min={today || undefined} value={selectedDate && !availableDates.includes(selectedDate) ? selectedDate : ''} onChange={event => setSelectedDate(event.target.value)} /></label></div>{currentRunClosed && <p role="alert" className="new-order-alert">Orders for this run are closed. Choose a later date.</p>}</fieldset>
      <fieldset><legend>3 · How many cases?</legend><div className="new-order-amount"><button type="button" aria-label="Remove one case" disabled={units <= 1} onClick={() => setUnits(units - 1)}>−</button><input aria-label="Number of cases" type="number" min="1" step="1" required value={units} onChange={event => setUnits(Number(event.target.value))} /><button type="button" aria-label="Add one case" onClick={() => setUnits(units + 1)}>＋</button></div></fieldset>
      <fieldset><legend>4 · Why? <span>(optional — helps dispatch plan)</span></legend><div className="new-order-reasons">{['Festival / holiday', 'Promotion', 'Ran out early', 'Other'].map(option => <button type="button" key={option} className={reason === option ? 'selected' : ''} onClick={() => setReason(reason === option ? '' : option)}>{option}</button>)}</div><label className="new-order-note">Note for dispatch<textarea maxLength={450} value={note} onChange={event => setNote(event.target.value)} placeholder="Add any delivery details dispatch should know" /></label></fieldset>
      <Feedback error={mutation.error} /><div className="new-order-actions"><button type="submit" className="new-order-submit" disabled={mutation.isPending || !Number.isInteger(units) || units < 1 || currentRunClosed}>{mutation.isPending ? 'Placing order…' : '⊙ Place order'}</button><Link to="/store/orders">Cancel</Link></div>
    </form><aside className="new-order-checks"><h2>Checks</h2><p>⊙ <strong>Before the cut-off</strong><span>Dispatch checks whether this run is still open when you place the order.</span></p><p>⊙ <strong>Fits a vehicle</strong><span>Dispatch will plan capacity for the selected run.</span></p>{temp === 'CHILLED' && <p>❄ <strong>Needs a fridge truck</strong><span>Chilled cases travel only in fridge vehicles.</span></p>}{outlet.data?.parkingConstraint && <p>⚠ <strong>Outlet access</strong><span>{outlet.data.parkingConstraint.replaceAll('_', ' ')}</span></p>}<div className="new-order-summary"><small>SUMMARY</small><strong>{units} {temp === 'CHILLED' ? 'chilled' : 'dry'} cases · {shortDate(runDate)}{windowText ? ` · ${windowText} window` : ''}</strong></div></aside></div>
  </div>
}
