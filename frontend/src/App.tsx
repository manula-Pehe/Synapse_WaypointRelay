import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './app/AuthProvider'
import { rolePaths, useAuth, type Role } from './app/auth'
import { RoleGuard, RoleLayout, WorkspacePlaceholder } from './app/RoleLayout'
import { LoginPage } from './features/auth/LoginPage'
import { NotificationsPage } from './features/notifications/Notifications'
import { ApiError } from './lib/api'
import { useState } from 'react'
import DispatcherLayout from './components/DispatcherLayout'
import OrderQueue from './components/OrderQueue'
import FleetStatus from './components/FleetStatus'
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
import { StoreMore, StoreSettings } from './features/store/StoreSettings'
import { StoreHistory } from './features/store/StoreHistory'
import OutletsReference from './components/OutletsReference'
import RunReport from './components/RunReport'
import CapacityOutlook from './components/CapacityOutlook'
import IssuesInbox from './components/IssuesInbox'
import IssueDetail from './components/IssueDetail'

function DispatcherWorkspace() {
  const { logout } = useAuth()
  const [activeNav, setActiveNav] = useState('issues')
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>('ISS-0142')

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

  const content = activeNav === 'fleet' ? <FleetStatus />
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
    : activeNav === 'outlets' ? <OutletsReference />
    : activeNav === 'reports' ? <RunReport />
    : <OrderQueue />

  return (
    <DispatcherLayout
      activeNav={activeNav}
      onSignOut={logout}
      onNavChange={(nav) => {
        setActiveNav(nav)
        if (nav !== 'issues') {
          setSelectedIssueId(null)
        }
      }}
      title={currentMeta.title}
      subtitle={currentMeta.subtitle}
      planStatus={currentMeta.planStatus}
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
