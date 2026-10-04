import { useNavigate } from 'react-router-dom'
import { useAuth } from '../app/auth'

export interface AccountMenuProps {
  isOpen: boolean
  onClose: () => void
  onSignOut?: () => void
}

export default function AccountMenu({ isOpen, onClose, onSignOut }: AccountMenuProps) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!isOpen || !user) return null

  return <div className="absolute bottom-full left-0 z-50 mb-3 w-72 rounded-2xl border border-line bg-surface p-4 shadow-xl" role="dialog" aria-label="Dispatcher account">
    <div className="flex items-start justify-between gap-3 border-b border-line pb-3">
      <div><strong className="block text-sm text-ink">{user.name}</strong><span className="text-xs text-muted">Dispatcher · {user.depot ?? 'All depots'}</span></div>
      <button type="button" className="min-h-10 min-w-10 text-muted" aria-label="Close account menu" onClick={onClose}>×</button>
    </div>
    <div className="mt-3 border-t border-line pt-3"><button type="button" className="min-h-10 w-full rounded-lg px-3 text-left text-sm hover:bg-surface-2" onClick={() => { onClose(); navigate('/dispatch/notifications') }}>Notifications</button><button type="button" className="min-h-10 w-full rounded-lg px-3 text-left text-sm font-semibold text-danger hover:bg-danger-soft" onClick={() => { onClose(); (onSignOut ?? logout)() }}>Sign out</button></div>
  </div>
}
