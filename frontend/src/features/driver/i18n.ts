// Every piece of driver text goes through here. English, Sinhala and Tamil for the stop list;
// keys everywhere so a screen never hard-codes a word.

export type Language = 'en' | 'si' | 'ta'

const STRINGS = {
  en: {
    'driver.app.title': 'Driver',
    'driver.signin.staffId': 'Staff ID',
    'driver.signin.pin': 'PIN',
    'driver.signin.continue': 'Continue',
    'driver.signin.offline': 'No signal. Sign in with the PIN you used last time.',
    'driver.today.title': 'Today',
    'driver.today.vehicle': 'Vehicle',
    'driver.today.stops': 'stops',
    'driver.today.accept': 'Check the load and accept',
    'driver.today.eta': 'Back by',
    'driver.stops.title': 'Stops',
    'driver.stops.next': 'Next stop',
    'driver.stops.done': 'Done',
    'driver.stops.window': 'Window',
    'driver.stops.arrive': 'I have arrived',
    'driver.stops.cases': 'cases',
    'driver.stop.detail': 'Stop',
    'driver.stop.access': 'Access',
    'driver.stop.call': 'Call store',
    'driver.delivery.deliver': 'Record delivery',
    'driver.delivery.photo': 'Photo',
    'driver.delivery.signature': 'Signature',
    'driver.delivery.receivedBy': 'Received by',
    'driver.delivery.undo': 'Undo',
    'driver.delivery.undone': 'Undone',
    'driver.delivery.saved': 'Saved on this phone',
    'driver.failed.title': 'What happened?',
    'driver.failed.short': 'Some cases did not go',
    'driver.failed.none': 'Nothing was delivered',
    'driver.failed.reason.storeClosed': 'Store was closed',
    'driver.failed.reason.noAccess': 'Could not get in',
    'driver.failed.reason.refused': 'Store refused',
    'driver.failed.reason.damaged': 'Cases damaged',
    'driver.failed.units': 'Cases delivered',
    'driver.wait.title': 'Store is closed',
    'driver.wait.start': 'Start waiting',
    'driver.wait.end': 'Done waiting',
    'driver.wait.waiting': 'Waiting',
    'driver.sync.synced': 'Synced',
    'driver.sync.syncing': 'Syncing',
    'driver.sync.offline': 'Offline',
    'driver.sync.waiting': '{count} waiting',
    'driver.sync.title': 'Your work is saved on this phone',
    'driver.sync.backOnline': 'Back online',
    'driver.sync.summary': 'What synced',
    'driver.sync.nothing': 'Nothing is waiting to send.',
    'driver.menu.title': 'Menu',
    'driver.menu.theme': 'Light or dark',
    'driver.menu.language': 'Language',
    'driver.menu.depot': 'Call depot',
    'driver.menu.problem': 'Report a problem',
    'driver.menu.signout': 'Sign out',
    'driver.tripEnd.title': 'Trip complete',
    'driver.tripEnd.next': 'Back at the depot',
    'driver.tripEnd.done': 'Deliveries recorded',
    'driver.problem.title': 'Report a problem',
    'driver.problem.canDrive': 'I can keep driving',
    'driver.problem.cannot': 'I cannot drive',
    'driver.problem.send': 'Send to dispatch',
    'driver.problem.sent': 'Sent. Dispatch will reply here.',
    'driver.problem.reply': 'Dispatch says',
    'driver.problem.fridge': 'Fridge reading',
  },
  si: {
    'driver.app.title': 'රියරුවා',
    'driver.stops.title': 'නවත්වීම්',
    'driver.stops.next': 'ඊළඟ නවත්වීම',
    'driver.stops.done': 'අවසන්',
    'driver.stops.window': 'කාලය',
    'driver.stops.arrive': 'මම පැමිණිලා',
    'driver.stops.cases': 'කේස',
    'driver.delivery.deliver': 'බාර දීම සලකුණු කරන්න',
    'driver.failed.title': 'කුමක් සිදු කළද?',
    'driver.sync.synced': 'සම්පූර්ණයි',
    'driver.sync.syncing': 'යැමුණි',
    'driver.sync.offline': 'සංයුක්තයි',
    'driver.sync.waiting': '{count} බලාපොරොත්තු',
  },
  ta: {
    'driver.app.title': 'ஓட்டுநர்',
    'driver.stops.title': 'நிறுத்தங்கள்',
    'driver.stops.next': 'அடுத்த நிறுத்தம்',
    'driver.stops.done': 'முடிந்தது',
    'driver.stops.window': 'நேரம்',
    'driver.stops.arrive': 'நான் வந்துவிட்டேன்',
    'driver.stops.cases': 'பெட்டிகள்',
    'driver.delivery.deliver': 'விநியோகம் பதிவு செய்',
    'driver.failed.title': 'என்ன நடந்தது?',
    'driver.sync.synced': 'முடிந்தது',
    'driver.sync.syncing': 'அனுப்புகிறது',
    'driver.sync.offline': 'இணைப்பு இல்லை',
    'driver.sync.waiting': '{count} காத்திருக்கும்',
  },
} as const

export type StringKey = keyof typeof STRINGS.en

const LANGUAGE_NAMES: Record<Language, string> = {
  en: 'English',
  si: 'සිංහල',
  ta: 'தமிழ்',
}

export function languageName(language: Language) {
  return LANGUAGE_NAMES[language]
}

/** Falls back to English, then to the key itself, so a missing translation shows something usable. */
export function t(language: Language, key: StringKey, values?: Record<string, string | number>) {
  const table = STRINGS[language] as Partial<Record<StringKey, string>>
  let text = table[key] ?? STRINGS.en[key] ?? key
  for (const [name, value] of Object.entries(values ?? {})) {
    text = text.replaceAll(`{${name}}`, String(value))
  }
  return text
}