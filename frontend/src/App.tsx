import { QueryClient, QueryClientProvider, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './app/AuthProvider'
import { rolePaths, useAuth, type Role } from './app/auth'
import { RoleGuard, RoleLayout, WorkspacePlaceholder } from './app/RoleLayout'
import { LoginPage } from './features/auth/LoginPage'
import { NotificationsPage } from './features/notifications/Notifications'
import DriverApp from './features/driver/DriverApp'
import { ApiError } from './lib/api'
import { useState } from 'react'
import DispatcherLayout from './components/DispatcherLayout'
import LiveBoardPage from './components/LiveBoardPage'
import { StoreLayout } from './features/store/StoreLayout'
import { StoreHome, StoreOrders, StoreOrderDetail } from './features/store/StoreOrders'
import { StoreReview } from './features/store/StoreReview'
import { NewStoreOrder } from './features/store/NewStoreOrder'
import { StoreDeliveries } from './features/store/StoreDeliveries'
import { StoreArrival } from './features/store/StoreArrival'
import { StoreIssues, StoreIssueDetail } from './features/store/StoreIssues'
import { NewIssuePage } from './features/store/NewIssuePage'
import { StoreMoved } from './features/store/StoreMoved'
import { StoreDeliveryProblem } from './features/store/StoreDeliveryProblem'
import { StoreReceipt } from './features/store/StoreReceipt'
import { DispatchIssues } from './features/dispatch/issues/DispatchIssues'
import { DriverDecisions } from './features/dispatch/issues/DriverDecisions'
import { StoreMore, StoreSettings } from './features/store/StoreSettings'
import { StoreHistory } from './features/store/StoreHistory'
import { OrderQueueScreen } from './features/dispatch/core/OrderQueueScreen'
import { FleetScreen } from './features/dispatch/core/FleetScreen'
import { OutletsScreen } from './features/dispatch/core/OutletsScreen'
import { dispatchApi } from './features/dispatch/core/api'
import NetworkMap from './features/dispatch/core/NetworkMap'
import UIShowcase from './ui/UIShowcase'
import RunReport from './components/RunReport'
import CapacityOutlook from './components/CapacityOutlook'
import { LoaderSignIn } from './features/loader/LoaderSignIn'
import { LoaderHome } from './features/loader/LoaderHome'

function DispatcherWorkspace() {
  const { user, logout } = useAuth()
  const client = useQueryClient()
  const [activeNav, setActiveNav] = useState('orders')
  const [activeDepot, setActiveDepot] = useState(user?.depot ?? 'Peliyagoda')
  const [clockInput, setClockInput] = useState('')
  const settings = useQuery({ queryKey: ['dispatch-settings'], queryFn: dispatchApi.settings, refetchInterval: 30_000 })
  const moveClock = useMutation({ mutationFn: dispatchApi.moveClock, onSuccess: () => client.invalidateQueries() })
  const runDate = settings.data?.runDate
  const depot = activeDepot === 'All depots' ? '' : activeDepot
  const plan = useQuery({ queryKey: ['dispatch-plan-status', runDate, depot], queryFn: () => dispatchApi.latestPlan(runDate!, depot), enabled: !!runDate && !!depot })
  const planStatus = !depot ? undefined : plan.data ? `Plan v${plan.data.version} · ${plan.data.status.toLowerCase()}` : plan.error instanceof ApiError && plan.error.status === 404 ? 'No plan' : plan.isPending ? 'Checking plan…' : 'Plan unavailable'

  const metadata: Record<string, { title: string; subtitle: string }> = {
    orders: { title: 'Order queue', subtitle: 'Orders and confirmation status for the selected run' },
    fleet: { title: 'Fleet', subtitle: 'Mark unavailable vehicles before planning' },
    plan: { title: 'Plan', subtitle: 'Planning screens are owned by Chethiya' },
    'live-board': { title: 'Live board', subtitle: 'Recorded delivery progress and attention items' },
    'driver-decisions': { title: 'Driver decisions', subtitle: 'Sync conflicts, failed deliveries and vehicle problems, from driver data' },
    issues: { title: 'Issues', subtitle: 'Store issues and replies' },
    'network-map': { title: 'Network map', subtitle: 'District trips from the selected plan' },
    capacity: { title: 'Capacity outlook · sample preview', subtitle: 'Forecast backend is not available yet' },
    outlets: { title: 'Outlets', subtitle: 'Delivery rules by outlet' },
    reports: { title: 'Run report', subtitle: 'Recorded outcomes and exceptions for the selected run' },
  }
  const meta = metadata[activeNav] ?? { title: 'Waypoint Relay', subtitle: 'Dispatch & fleet operations' }
  const content = settings.error ? <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-800">Could not load run settings: {settings.error.message}</p>
    : activeNav === 'fleet' ? (runDate && depot ? <FleetScreen runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : activeNav === 'live-board' ? (runDate && depot ? <LiveBoardPage runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : activeNav === 'driver-decisions' ? <DriverDecisions />
    : activeNav === 'issues' ? <DispatchIssues />
    : activeNav === 'capacity' ? <CapacityOutlook />
    : activeNav === 'outlets' ? <OutletsScreen depot={depot} />
    : activeNav === 'network-map' ? (runDate && depot ? <NetworkMap runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : activeNav === 'reports' ? (runDate && depot ? <RunReport runDate={runDate} depot={depot} /> : <p>Select a depot and wait for the run date.</p>)
    : activeNav === 'orders' ? (runDate && depot ? <OrderQueueScreen runDate={runDate} depot={depot} onCreatePlan={() => setActiveNav('plan')} /> : <p>Select a depot and wait for the run date.</p>)
    : <p>This screen is being developed by its owner.</p>

  return <DispatcherLayout
    activeNav={activeNav}
    onSignOut={logout}
    onNavChange={setActiveNav}
    title={meta.title}
    subtitle={`${meta.subtitle}${runDate ? ` · Run ${runDate}` : ''}`}
    planStatus={planStatus}
    activeDepot={activeDepot}
    onDepotChange={setActiveDepot}
    runDate={runDate ? `Run: ${runDate}` : 'Loading run date…'}
    user={user ? { name: user.name, role: 'Dispatcher', depots: user.depot ?? 'All depots' } : undefined}
    clockControl={<form className="flex items-center gap-2" onSubmit={event => { event.preventDefault(); if (clockInput) moveClock.mutate(`${clockInput}:00+05:30`) }}><label className="text-xs font-medium">Demo clock (Sri Lanka) <input className="ml-1 min-h-10 rounded border border-slate-300 px-2" type="datetime-local" value={clockInput} onChange={event => setClockInput(event.target.value)} /></label><button className="min-h-10 rounded bg-[#0e2a47] px-3 text-xs font-semibold text-white" disabled={!clockInput || moveClock.isPending}>Set</button>{moveClock.error && <span role="alert" className="text-xs text-red-700">{moveClock.error.message}</span>}</form>}
  >{content}</DispatcherLayout>
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
            <Route path="/loader/sign-in" element={<LoaderSignIn />} />
            <Route path="/ui" element={<UIShowcase />} />
            {(Object.entries(rolePaths) as [Role, string][]).map(([role, path]) => (
              <Route key={role} element={<RoleGuard role={role} />}>
                <Route path={path} element={<RoleLayout />}>
                  {role === 'STORE_MANAGER' ? <>
                    <Route element={<StoreLayout />}>
                      <Route index element={<StoreHome />} />
                      <Route path="orders" element={<StoreOrders />} />
                      <Route path="orders/new" element={<NewStoreOrder />} />
                      <Route path="orders/review" element={<StoreReview />} />
                      <Route path="orders/:id" element={<StoreOrderDetail />} />
                      <Route path="deliveries" element={<StoreDeliveries />} />
                      <Route path="deliveries/:orderId" element={<StoreArrival />} />
                      <Route path="deliveries/:orderId/moved" element={<StoreMoved />} />
                      <Route path="deliveries/:orderId/problem" element={<StoreDeliveryProblem />} />
                      <Route path="deliveries/:orderId/receipt" element={<StoreReceipt />} />
                      <Route path="issues" element={<StoreIssues />} />
                      <Route path="issues/new" element={<NewIssuePage />} />
                      <Route path="issues/:id" element={<StoreIssueDetail />} />
                      <Route path="settings" element={<StoreSettings />} />
                      <Route path="more" element={<StoreMore />} />
                      <Route path="history" element={<StoreHistory />} />
                    </Route>
</> : role === 'DRIVER' ? (
                    // The driver app is a phone-width app in a cab, so it takes the whole route
                    // rather than an index page inside the desktop shell.
                    <Route path="*" element={<DriverApp />} />
                  ) : <Route index element={role === 'DISPATCHER' ? <DispatcherWorkspace /> : role === 'LOADER' ? <LoaderHome /> : <WorkspacePlaceholder />} />}
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
