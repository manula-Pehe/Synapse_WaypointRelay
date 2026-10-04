/**
 * X1m-off - signing in at a depot with no signal.
 *
 * The PIN is checked against a hash stored on the phone at the last successful online sign-in, so
 * the driver can start a run without a network. Nothing here bypasses the server: an offline
 * session cannot call a write endpoint, it can only queue work in the outbox until the sync runner
 * gets a signal and the server sees the same actions with the same clientIds.
 *
 * The stored value is salted and stretched with PBKDF2 rather than a bare digest. That does not make
 * a 4-digit PIN strong on its own - it only means the hash is not a rainbow-table lookup away - but
 * it is the honest ceiling for client-side verification, and the reason the session is marked
 * offline: the server has not confirmed this sign-in.
 */

const STORAGE_KEY = 'waypoint.driver.pin'
const SALT_KEY = 'waypoint.driver.pin.salt'
const ITERATIONS = 210_000

/** A session minted offline, so the app knows to queue rather than to trust the network. */
export interface OfflineCredential {
  staffId: string
  /** PBKDF2-SHA256 of the PIN, hex. Never the PIN itself. */
  hash: string
  /** Who that staff id was, so the offline session knows which driver it is. */
  userId: string
  name: string
  vehicleId: string | null
  savedAt: string
}

function randomSalt(): Uint8Array {
  const salt = new Uint8Array(16)
  crypto.getRandomValues(salt)
  return salt
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
}

async function derive(pin: string, salt: Uint8Array): Promise<string> {
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(pin),
    'PBKDF2',
    false,
    ['deriveBits'],
  )
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: salt as BufferSource, iterations: ITERATIONS, hash: 'SHA-256' },
    key,
    256,
  )
  return toHex(new Uint8Array(bits))
}

/** A constant-time compare, so a wrong PIN cannot be discovered a character at a time. */
function sameHash(a: string, b: string): boolean {
  if (a.length !== b.length) return false
  let diff = 0
  for (let index = 0; index < a.length; index += 1) diff |= a.charCodeAt(index) ^ b.charCodeAt(index)
  return diff === 0
}

function readJson(key: string): unknown {
  try {
    return JSON.parse(localStorage.getItem(key) ?? 'null')
  } catch {
    return null
  }
}

/**
 * Called after a successful *online* sign-in. Nothing is stored if the phone cannot do the crypto -
 * the driver simply gets the online behaviour with no offline fallback.
 */
export async function rememberPin(
  credential: Omit<OfflineCredential, 'hash' | 'savedAt'>,
  pin: string,
): Promise<void> {
  try {
    const salt = randomSalt()
    const hash = await derive(pin, salt)
    localStorage.setItem(SALT_KEY, toHex(salt))
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ ...credential, hash, savedAt: new Date().toISOString() } satisfies OfflineCredential),
    )
  } catch {
    /* A phone without WebCrypto signs in online only, which is the safe default. */
  }
}

/** Forgets the stored PIN - on sign-out, and whenever the server rejects the credentials. */
export function forgetPin(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(SALT_KEY)
  } catch {
    /* Nothing to forget. */
  }
}

export interface OfflineCheck {
  ok: boolean
  /** Why it failed, so the screen can say something useful rather than just "wrong PIN". */
  reason: 'ok' | 'no-stored-pin' | 'wrong-pin'
  credential: OfflineCredential | null
}

/** Checks a PIN against the hash stored at the last online sign-in. */
export async function checkPinOffline(staffId: string, pin: string): Promise<OfflineCheck> {
  const stored = readJson(STORAGE_KEY) as OfflineCredential | null
  const saltHex = localStorage.getItem(SALT_KEY)

  if (!stored || typeof stored.hash !== 'string' || !saltHex) {
    return { ok: false, reason: 'no-stored-pin', credential: null }
  }
  // A different driver cannot use this phone's stored PIN, so the staff id has to match too.
  if (stored.staffId !== staffId) {
    return { ok: false, reason: 'wrong-pin', credential: null }
  }

  const salt = new Uint8Array(saltHex.match(/.{2}/g)?.map((byte) => parseInt(byte, 16)) ?? [])
  const hash = await derive(pin, salt)
  return sameHash(hash, stored.hash)
    ? { ok: true, reason: 'ok', credential: stored }
    : { ok: false, reason: 'wrong-pin', credential: null }
}
