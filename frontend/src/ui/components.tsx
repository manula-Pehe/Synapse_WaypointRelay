import { useEffect, useState, type ButtonHTMLAttributes, type HTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Icon } from './icons'

type Tone = 'primary' | 'secondary' | 'neutral' | 'outline' | 'tonal' | 'critical'
type Size = 's' | 'm' | 'l' | 'xl'
const sizes: Record<Size, string> = { s: 'min-h-10 px-3 text-xs', m: 'min-h-12 px-4 text-sm', l: 'min-h-14 px-5 text-base', xl: 'min-h-16 px-6 text-xl' }
const tones: Record<Tone, string> = {
  primary: 'bg-brand text-on-brand',
  secondary: 'border border-brand bg-surface text-brand',
  neutral: 'bg-inset text-ink',
  outline: 'border border-line bg-transparent text-ink',
  tonal: 'bg-brand-soft text-brand',
  critical: 'bg-danger text-white',
}

export function Button({ tone = 'primary', size = 'm', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { tone?: Tone; size?: Size }) {
  return <button {...props} className={`inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-50 ${sizes[size]} ${tones[tone]} ${className}`} />
}
export function IconButton({ label, children, size = 'm', tone = 'outline', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string; size?: Size; tone?: 'neutral' | 'outline' | 'primary' }) {
  return <Button {...props} aria-label={label} size={size} tone={tone} className="aspect-square !px-0">{children}</Button>
}
export type BadgeStatus = 'way' | 'delivered' | 'risk' | 'failed' | 'deferred' | 'chilled' | 'offline'
const statusStyles: Record<BadgeStatus, string> = {
  way: 'bg-status-way-soft text-status-way',
  delivered: 'bg-status-delivered-soft text-status-delivered',
  risk: 'bg-status-risk-soft text-status-risk',
  failed: 'bg-status-failed-soft text-status-failed',
  deferred: 'bg-status-deferred-soft text-status-deferred',
  chilled: 'bg-status-chilled-soft text-status-chilled',
  offline: 'bg-status-offline-soft text-status-offline',
}
export function Badge({ children, status, size = 'm', icon }: { children: ReactNode; status: BadgeStatus; size?: 's' | 'm' | 'l'; icon: ReactNode }) {
  return <span className={`inline-flex items-center gap-1.5 rounded-full font-semibold ${size === 's' ? 'px-2 py-0.5 text-xs' : size === 'l' ? 'px-3 py-1.5 text-base' : 'px-3 py-1 text-sm'} ${statusStyles[status]}`}><span aria-hidden="true">{icon}</span>{children}</span>
}
export function Card({ children, className = '', ...props }: HTMLAttributes<HTMLElement> & { children: ReactNode }) { return <section {...props} className={`rounded-2xl border border-line bg-surface p-5 ${className}`}>{children}</section> }
export function Input({ label, id, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; id: string }) { return <label htmlFor={id} className="block text-sm font-semibold text-ink">{label}<input {...props} id={id} className={`mt-1 block min-h-10 w-full rounded-lg border border-line bg-surface px-3 text-ink ${props.className ?? ''}`} /></label> }
export function Select({ label, id, children, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label: string; id: string; children: ReactNode }) { return <label htmlFor={id} className="block text-sm font-semibold text-ink">{label}<select {...props} id={id} className={`mt-1 block min-h-10 w-full rounded-lg border border-line bg-surface px-3 text-ink ${props.className ?? ''}`}>{children}</select></label> }
export function Checkbox({ label, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string }) { return <label className="inline-flex min-h-10 items-center gap-2 text-sm"><input {...props} type="checkbox" className="size-5 accent-brand" />{label}</label> }
export function Switch({ label, checked, onChange, disabled }: { label: string; checked: boolean; onChange: (checked: boolean) => void; disabled?: boolean }) { return <button type="button" role="switch" aria-checked={checked} disabled={disabled} onClick={() => onChange(!checked)} className="inline-flex min-h-10 items-center gap-2 text-sm"><span className={`relative inline-flex h-6 w-11 rounded-full ${checked ? 'bg-brand' : 'bg-inset'}`}><span className={`absolute top-1 size-4 rounded-full bg-surface transition-transform ${checked ? 'translate-x-6' : 'translate-x-1'}`} /></span>{label}</button> }
export function Stepper({ value, min = 0, max = 100, onChange, label }: { value: number; min?: number; max?: number; onChange: (value: number) => void; label: string }) { return <div className="inline-flex items-center gap-2"><span className="text-sm font-semibold">{label}</span><IconButton label={`Decrease ${label}`} disabled={value <= min} onClick={() => onChange(value - 1)}>−</IconButton><output className="min-w-8 text-center">{value}</output><IconButton label={`Increase ${label}`} disabled={value >= max} onClick={() => onChange(value + 1)}>+</IconButton></div> }
export function Tabs({ items, active, onChange, label }: { items: { id: string; label: string }[]; active: string; onChange: (id: string) => void; label: string }) { return <div role="tablist" aria-label={label} className="inline-flex rounded-lg bg-inset p-1">{items.map(item => <button key={item.id} type="button" role="tab" aria-selected={active === item.id} onClick={() => onChange(item.id)} className={`min-h-10 rounded-md px-4 text-sm font-semibold ${active === item.id ? 'bg-surface text-brand shadow-sm' : 'text-muted'}`}>{item.label}</button>)}</div> }
export const Segmented = Tabs
export function Sheet({ open, title, onClose, children, side = 'right' }: { open: boolean; title: string; onClose: () => void; children: ReactNode; side?: 'right' | 'bottom' }) { if (!open) return null; return <div className="fixed inset-0 z-50 bg-overlay" role="dialog" aria-modal="true" aria-label={title}><button className="absolute inset-0" aria-label="Close sheet" onClick={onClose} /><div className={`absolute overflow-auto bg-surface p-6 shadow-xl ${side === 'right' ? 'inset-y-0 right-0 w-full max-w-md' : 'inset-x-0 bottom-0 max-h-[80vh] rounded-t-2xl'}`}><div className="flex items-center justify-between"><h2 className="text-lg font-bold">{title}</h2><IconButton label="Close sheet" onClick={onClose}>×</IconButton></div><div className="mt-4">{children}</div></div></div> }
export function Dialog({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) { if (!open) return null; return <div className="fixed inset-0 z-50 flex items-center justify-center bg-overlay p-4" role="dialog" aria-modal="true" aria-label={title}><button className="absolute inset-0" aria-label="Close dialog" onClick={onClose} /><div className="relative w-full max-w-md rounded-2xl bg-surface p-6 shadow-xl"><div className="flex items-center justify-between"><h2 className="text-lg font-bold">{title}</h2><IconButton label="Close dialog" onClick={onClose}>×</IconButton></div><div className="mt-4">{children}</div></div></div> }
export function Toast({ message, tone = 'info', onDismiss }: { message: string; tone?: 'info' | 'success' | 'danger'; onDismiss: () => void }) { return <div role="status" className={`fixed bottom-4 right-4 z-50 flex max-w-sm items-center gap-3 rounded-xl border p-4 shadow-lg ${tone === 'success' ? 'border-success bg-success-soft text-success' : tone === 'danger' ? 'border-danger bg-danger-soft text-danger' : 'border-brand bg-brand-soft text-brand'}`}>{message}<IconButton label="Dismiss" onClick={onDismiss}>×</IconButton></div> }
export function AppBar({ title, action }: { title: string; action?: ReactNode }) { return <header className="flex min-h-16 items-center justify-between border-b border-line bg-surface px-5"><h1 className="text-xl font-bold">{title}</h1>{action}</header> }
export function SideNav({ items, active, onChange }: { items: { id: string; label: string }[]; active: string; onChange: (id: string) => void }) { return <nav aria-label="Sections" className="space-y-1 bg-surface p-3">{items.map(item => <button key={item.id} className={`block min-h-10 w-full rounded-lg px-4 text-left text-sm ${active === item.id ? 'bg-brand-soft font-semibold text-brand' : 'text-muted hover:bg-inset'}`} onClick={() => onChange(item.id)}>{item.label}</button>)}</nav> }
export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) { return <div className="rounded-2xl border border-dashed border-line bg-surface p-8 text-center"><h2 className="font-bold">{title}</h2><p className="mt-2 text-sm text-muted">{description}</p>{action && <div className="mt-4">{action}</div>}</div> }
export function Spinner({ label = 'Loading' }: { label?: string }) { return <span role="status" className="inline-flex items-center gap-2 text-sm text-muted"><span aria-hidden="true" className="size-5 animate-spin rounded-full border-2 border-line border-t-brand" />{label}</span> }
export function ThemeSwitch({ defaultDark = false }: { defaultDark?: boolean }) { const [dark, setDark] = useState(() => { const saved = localStorage.getItem('waypoint.theme'); return saved ? saved === 'dark' : defaultDark }); useEffect(() => { document.documentElement.dataset.theme = dark ? 'dark' : 'light' }, [dark]); return <IconButton label={dark ? 'Switch to light theme' : 'Switch to dark theme'} title={dark ? 'Light theme' : 'Dark theme'} tone="outline" size="s" onClick={() => { const next = !dark; setDark(next); localStorage.setItem('waypoint.theme', next ? 'dark' : 'light') }}><Icon name={dark ? 'sun' : 'moon'} /></IconButton> }
