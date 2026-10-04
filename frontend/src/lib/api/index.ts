export class ApiError extends Error {
  readonly status: number
  readonly code: string
  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

const apiBase = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

/** Built apps call VITE_API_URL; empty means same origin (Vite proxy or nginx). */
function apiUrl(path: string): string {
  return `${apiBase}/api/${path.replace(/^\//, '')}`
}

let accessToken: string | null = null
let onUnauthorized = () => {}
export function configureApi(token: string | null, unauthorized: () => void) {
  accessToken = token
  onUnauthorized = unauthorized
}

type ApiOptions = RequestInit & { authenticated?: boolean }
/** Paths are relative to /api. Use authenticated: false for sign-in. */
export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { authenticated = true, ...init } = options
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const requestToken = authenticated ? accessToken : null
  if (requestToken) headers.set('Authorization', `Bearer ${requestToken}`)
  const response = await fetch(apiUrl(path), { ...init, headers })
  if (response.status === 401 && requestToken && requestToken === accessToken) onUnauthorized()
  const body = await response.text()
  let data: unknown
  try {
    data = body ? JSON.parse(body) : undefined
  } catch {
    if (response.ok)
      throw new ApiError(
        response.status,
        'INVALID_RESPONSE',
        'The server returned an invalid response.',
      )
  }
  if (!response.ok) {
    const error = data as { code?: string; message?: string } | undefined
    throw new ApiError(
      response.status,
      typeof error?.code === 'string' ? error.code : `HTTP_${response.status}`,
      response.status === 403
        ? 'No access'
        : typeof error?.message === 'string'
          ? error.message
          : 'Something went wrong. Please try again.',
    )
  }
  return data as T
}

export async function apiBlob(path: string): Promise<Blob> {
  const requestToken = accessToken
  const response = await fetch(apiUrl(path), {
    headers: requestToken ? { Authorization: `Bearer ${requestToken}` } : {},
  })
  if (response.status === 401 && requestToken && requestToken === accessToken) onUnauthorized()
  if (!response.ok) throw new ApiError(response.status, `HTTP_${response.status}`, 'Could not load the file.')
  return response.blob()
}
