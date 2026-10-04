import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useAuth, type Language } from '../app/auth'
import { api } from '../lib/api'

export interface AccountMenuProps {
  isOpen: boolean
  onClose: () => void
}

const languages: [Language, string][] = [['en', 'English'], ['si', 'සිංහල'], ['ta', 'தமிழ்']]

export default function AccountMenu({ isOpen, onClose }: AccountMenuProps) {
  const { user, token, language, setLanguage, logout } = useAuth()
  const navigate = useNavigate()
  const saveLanguage = useMutation({
    mutationFn: (next: Language) => token?.startsWith('demo-')
      ? Promise.resolve()
      : api('auth/me', { method: 'PATCH', body: JSON.stringify({ language: next }) }),
    onSuccess: (_, next) => setLanguage(next),
  })
  if (!isOpen || !user) return null

  return <div className="absolute bottom-full left-0 z-50 mb-3 w-72 rounded-2xl border border-slate-200 bg-white p-4 shadow-xl" role="dialog" aria-label="Dispatcher account">
    <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-3">
      <div><strong className="block text-sm text-slate-900">{user.name}</strong><span className="text-xs text-slate-500">Dispatcher · {user.depot ?? 'All depots'}</span></div>
      <button type="button" className="min-h-10 min-w-10 text-slate-600" aria-label="Close account menu" onClick={onClose}>×</button>
    </div>
    <fieldset className="mt-3 space-y-1"><legend className="mb-2 text-xs font-semibold text-slate-700">Language</legend>{languages.map(([code, label]) => <label key={code} className="flex min-h-10 items-center gap-2 rounded-lg px-2 text-sm hover:bg-slate-50"><input type="radio" name="dispatch-language" checked={language === code} disabled={saveLanguage.isPending} onChange={() => saveLanguage.mutate(code)} />{label}</label>)}</fieldset>
    {saveLanguage.error && <p role="alert" className="mt-2 text-xs text-red-700">{saveLanguage.error.message}</p>}
    <div className="mt-3 border-t border-slate-100 pt-3"><button type="button" className="min-h-10 w-full rounded-lg px-3 text-left text-sm hover:bg-slate-50" onClick={() => { onClose(); navigate('/dispatch/notifications') }}>Notifications</button><button type="button" className="min-h-10 w-full rounded-lg px-3 text-left text-sm font-semibold text-red-700 hover:bg-red-50" onClick={() => { onClose(); logout() }}>Sign out</button></div>
  </div>
}
