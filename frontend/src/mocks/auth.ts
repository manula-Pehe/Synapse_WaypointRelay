import type { Credentials, Session, User } from '../app/auth'
import { ApiError } from '../lib/api'

// Fictional demo accounts. Never use these tokens against the real backend.
const accounts: { identifier: string; secret: string; user: User }[] = [
  {
    identifier: 'dilani@waypoint.lk',
    secret: 'Relay@2026',
    user: {
      id: 'demo-store',
      name: 'Dilani',
      role: 'STORE_MANAGER',
      outletId: 'OUT001',
      depot: null,
      vehicleId: null,
      language: 'en',
    },
  },
  {
    identifier: 'ruwan@waypoint.lk',
    secret: 'Relay@2026',
    user: {
      id: 'demo-dispatch',
      name: 'Ruwan',
      role: 'DISPATCHER',
      outletId: null,
      depot: null,
      vehicleId: null,
      language: 'en',
    },
  },
  {
    identifier: 'PELIYAGODA',
    secret: '1234',
    user: {
      id: 'demo-loader',
      name: 'Kasun',
      role: 'LOADER',
      outletId: null,
      depot: 'PELIYAGODA',
      vehicleId: null,
      language: 'en',
    },
  },
  {
    identifier: 'DRV-0036',
    secret: '3636',
    user: {
      id: 'demo-driver',
      name: 'Nuwan',
      role: 'DRIVER',
      outletId: null,
      depot: null,
      vehicleId: 'VEH036',
      language: 'en',
    },
  },
]
export async function mockLogin(credentials: Credentials): Promise<Session> {
  await new Promise((resolve) => setTimeout(resolve, 250))
  const account = accounts.find(
    (account) =>
      account.identifier.toLowerCase() === credentials.identifier.trim().toLowerCase() &&
      account.secret === credentials.secret,
  )
  if (!account) throw new ApiError(401, 'UNAUTHORIZED', 'Incorrect sign-in details.')
  return { token: `demo-${account.user.id}`, user: { ...account.user } }
}
