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


function DispatcherWorkspace() {
  const [activeNav, setActiveNav] = useState('orders')
  const pageMeta = activeNav === 'fleet'
    ? { title: 'Fleet · Peliyagoda · Thu 1 Oct', subtitle: 'Mark workshop vehicles before planning · weekly fuel shown per vehicle' }
    : { title: 'Order queue · Thu 1 Oct run', subtitle: 'Orders closed Wed 4:00 PM · 85 confirmed orders' }
  return (
    <DispatcherLayout activeNav={activeNav} onNavChange={setActiveNav} title={pageMeta.title} subtitle={pageMeta.subtitle}>
      {activeNav === 'fleet' ? <FleetStatus /> : <OrderQueue />}
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
                  <Route index element={role === 'DISPATCHER' ? <DispatcherWorkspace /> : <WorkspacePlaceholder />} />
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
