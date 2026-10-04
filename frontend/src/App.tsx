import { QueryClient, QueryClientProvider, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './app/AuthProvider'
import { rolePaths, useAuth, type Role } from './app/auth'
import { RoleGuard, RoleLayout, WorkspacePlaceholder } from './app/RoleLayout'
import { LoginPage } from './features/auth/LoginPage'
import { NotificationsPage } from './features/notifications/Notifications'
import { ApiError } from './lib/api'
import { useState } from 'react'
import DispatcherLayout from './components/DispatcherLayout'
import LiveBoardPage from './components/LiveBoardPage'
import { StoreLayout } from './features/store/StoreLayout'
import { StoreHome, StoreOrders, StoreOrderDetail, NewStoreOrder } from './features/store/StoreOrders'
import { StoreDeliveries } from './features/store/StoreDeliveries'
import { StoreIssues, NewIssue, StoreIssueDetail } from './features/store/StoreIssues'
import { DispatchIssues } from './features/dispatch/issues/DispatchIssues'
import { StoreSettings } from './features/store/StoreSettings'
import { StoreHistory } from './features/store/StoreHistory'
import { DispatchOrders, DispatchFleet, DispatchOutlets } from './features/dispatch/core/DispatchDataPages'
import { dispatchApi } from './features/dispatch/core/api'
import RunReport from './components/RunReport'
import CapacityOutlook from './components/CapacityOutlook'
import IssuesInbox from './components/IssuesInbox'
import IssueDetail from './components/IssueDetail'

function DispatcherWorkspace() {
  const { user } = useAuth()
  const client = useQueryClient()
  const [activeNav, setActiveNav] = useState('orders')
  const [activeDepot, setActiveDepot] = useState(user?.depot ?? 'Peliyagoda')
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null)
  const [clockInput, setClockInput] = useState('')
  const settings = useQuery({ queryKey: ['dispatch-settings'], queryFn: dispatchApi.settings, refetchInterval: 30_000 })
  const moveClock = useMutation({ mutationFn: dispatchApi.moveClock, onSuccess: () => client.invalidateQueries() })
  const runDate = settings.data?.runDate
  const depot = activeDepot === 'All depots' ? '' : activeDepot

  const pageMeta: Record<string, { title: string; subtitle: string; planStatus?: string }> = {
    orders: {
      title: 'Order queue · Thu 1 Oct run',
      subtitle: 'Orders closed Wed 4:00 PM · 85 confirmed orders',
      planStatus: 'Plan v1 · not started',
    },
    fleet: {
      title: 'Fleet · Peliyagoda · Thu 1 Oct',
      subtitle: 'Mark workshop vehicles before planning · weekly fuel shown per vehicle',
      planStatus: 'Plan v1 · not started',
    },
    'live-board': {
      title: 'Live board · Thu 1 Oct · 6:45 AM',
      subtitle: 'Exceptions first · updates arrive as drivers sync',
      planStatus: 'Plan v1 · published',
    },
    issues: {
      title: 'Issues',
      subtitle: 'Thu 1 Oct · 7:50 AM · from stores, drivers and loaders',
      planStatus: '4 open',
    },
    capacity: {
      title: 'Capacity outlook · next 10 weeks',
      subtitle: 'Demand forecast vs fleet · Peliyagoda · plan fridge trucks before peaks',
      planStatus: 'Estimated · rule-based',
    },
    outlets: {
      title: 'Outlets',
      subtitle: '120 outlets · 2 depots',
      planStatus: 'Plan v1 · not started',
    },
    reports: {
      title: 'Run report · Thu 1 Oct',
      subtitle: 'All depots · final at 2:00 PM',
      planStatus: 'Plan v1 · published',
    },
  }

  const currentMeta = pageMeta[activeNav] || {
    title: 'Waypoint Relay',
    subtitle: 'Dispatch & Fleet Operations',
    planStatus: 'Plan v1 · not started',
  }

  const isIssueDetailActive = activeNav === 'issues' && selectedIssueId !== null

  const content = settings.error ? <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-800">Could not load run settings: {settings.error.message}</p>
    : activeNav === 'fleet' ? (runDate && depot ? <DispatchFleet runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : activeNav === 'live-board' ? <LiveBoardPage />
    : activeNav === 'issues' ? (
        selectedIssueId ? (
          <IssueDetail
            issueId={selectedIssueId}
            onBack={() => setSelectedIssueId(null)}
          />
        ) : (
          <IssuesInbox
            onIssueSelect={(issue) => setSelectedIssueId(issue.id)}
          />
        )
      )
    : activeNav === 'capacity' ? <CapacityOutlook />
    : activeNav === 'outlets' ? <DispatchOutlets depot={depot} />
    : activeNav === 'reports' ? <RunReport />
    : activeNav === 'orders' ? (runDate && depot ? <DispatchOrders runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : <p>This screen is awaiting its backend integration.</p>

  return (
    <DispatcherLayout
      activeNav={activeNav}
      onNavChange={(nav) => {
        setActiveNav(nav)
        if (nav !== 'issues') {
          setSelectedIssueId(null)
        }
      }}
      title={['orders', 'fleet', 'outlets'].includes(activeNav) ? `${activeNav[0].toUpperCase()}${activeNav.slice(1)} · ${depot || 'All depots'}` : currentMeta.title}
      subtitle={['orders', 'fleet', 'outlets'].includes(activeNav) ? (runDate ? `Run ${runDate} · demo clock ${settings.data?.now ?? ''}` : 'Loading run settings…') : currentMeta.subtitle}
      planStatus={['orders', 'fleet', 'outlets'].includes(activeNav) ? undefined : currentMeta.planStatus}
      activeDepot={activeDepot}
      onDepotChange={setActiveDepot}
      runDate={runDate ? `Run: ${runDate}` : 'Loading run date…'}
      user={user ? { name: user.name, role: 'Dispatcher', depots: user.depot ?? 'All depots' } : undefined}
      clockControl={<form className="flex items-center gap-2" onSubmit={event => { event.preventDefault(); if (clockInput) moveClock.mutate(`${clockInput}:00+05:30`) }}><label className="text-xs font-medium">Demo clock (Sri Lanka) <input className="ml-1 min-h-10 rounded border border-slate-300 px-2" type="datetime-local" value={clockInput} onChange={event => setClockInput(event.target.value)} /></label><button className="min-h-10 rounded bg-[#0e2a47] px-3 text-xs font-semibold text-white" disabled={!clockInput || moveClock.isPending}>Set</button>{moveClock.error && <span role="alert" className="text-xs text-red-700">{moveClock.error.message}</span>}</form>}
      hideTopBar={activeNav === 'capacity' || isIssueDetailActive}
    >
      {content}
    </DispatcherLayout>
  )
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (count, error) =>
        !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 2,
    },
  },
})
function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? rolePaths[user.role] : '/login'} replace />
}
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            {(Object.entries(rolePaths) as [Role, string][]).map(([role, path]) => (
              <Route key={role} element={<RoleGuard role={role} />}>
                <Route path={path} element={<RoleLayout />}>
                  {role === 'STORE_MANAGER' ? <>
                    <Route element={<StoreLayout />}>
                      <Route index element={<StoreHome />} />
                      <Route path="orders" element={<StoreOrders />} />
                      <Route path="orders/new" element={<NewStoreOrder />} />
                      <Route path="orders/:id" element={<StoreOrderDetail />} />
                      <Route path="deliveries" element={<StoreDeliveries />} />
                      <Route path="issues" element={<StoreIssues />} />
                      <Route path="issues/new" element={<NewIssue />} />
                      <Route path="issues/:id" element={<StoreIssueDetail />} />
                      <Route path="settings" element={<StoreSettings />} />
                      <Route path="history" element={<StoreHistory />} />
                    </Route>
                  </> : <Route index element={role === 'DISPATCHER' ? <DispatcherWorkspace /> : <WorkspacePlaceholder />} />}
                  {role === 'DISPATCHER' && <Route path="issues" element={<DispatchIssues />} />}
                  <Route path="notifications" element={<NotificationsPage />} />
                  <Route path="*" element={<Navigate to={path} replace />} />
                </Route>
              </Route>
            ))}
            <Route path="*" element={<HomeRedirect />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
