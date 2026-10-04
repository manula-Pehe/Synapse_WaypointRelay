import { useAuth } from '../../app/auth'

const en = {
  home: 'Home', orders: 'Orders', deliveries: 'Deliveries', issues: 'Issues',
  settings: 'Settings', more: 'More', signOut: 'Sign out', store: 'Store',
  deliveryWindow: 'Delivery window', newOrder: 'New order', reviewAndConfirm: 'Review and confirm',
  hello: 'Hello', storeManager: 'Store manager', extraDelivery: 'For an extra delivery, or a day you do not usually get one',
  reviewQuantitiesBeforeCutoff: 'Review your quantities before the cut-off',
  upcomingAndPast: 'Upcoming and past orders for', yourOutlet: 'your outlet',
  loadingCutoff: 'Loading cut-off…', extraOneOff: 'Extra or one-off delivery',
  reviewQuantities: 'Review quantities', upcoming: 'upcoming',
  outletDetails: 'Outlet details', outlet: 'Outlet', depot: 'Depot', access: 'Access',
  account: 'Account', language: 'Language', appliesEverywhere: 'Applies to your account on every device.',
  alerts: 'Alerts', alertsHelp: 'Choose which updates appear in your notification bell.',
  loadingAlerts: 'Loading alert settings…', viewNotifications: 'View notifications',
  history: 'History', historyHelp: 'Past deliveries and proof.', browseHistory: 'Browse history',
  deliveriesAlert: 'Deliveries', ordersAlert: 'Orders', issuesAlert: 'Issues',
  loadingOutlet: 'Loading outlet details…', moreOptions: 'More options',
  storeInformation: 'Store information', window: 'Window',
  languageSaved: 'Language saved.',
  alertsSummary: 'Order, delivery and issue updates', historySummary: 'Past runs and receipts',
} as const

type StoreKey = keyof typeof en
type StoreMessages = Record<StoreKey, string>

const si: StoreMessages = {
  home: 'මුල් පිටුව', orders: 'ඇණවුම්', deliveries: 'බෙදාහැරීම්', issues: 'ගැටලු',
  settings: 'සැකසුම්', more: 'තවත්', signOut: 'ඉවත් වන්න', store: 'වෙළඳසැල',
  deliveryWindow: 'බෙදාහැරීමේ වේලා පරාසය', newOrder: 'නව ඇණවුම', reviewAndConfirm: 'සමාලෝචනය කර තහවුරු කරන්න',
  hello: 'ආයුබෝවන්', storeManager: 'වෙළඳසැල් කළමනාකරු', extraDelivery: 'අමතර බෙදාහැරීමක් හෝ සාමාන්‍යයෙන් බෙදා නොහරින දිනයක් සඳහා',
  reviewQuantitiesBeforeCutoff: 'අවසන් වේලාවට පෙර ප්‍රමාණ සමාලෝචනය කරන්න',
  upcomingAndPast: 'ඉදිරි සහ පෙර ඇණවුම්:', yourOutlet: 'ඔබගේ වෙළඳසැල',
  loadingCutoff: 'අවසන් වේලාව පූරණය වෙමින්…', extraOneOff: 'අමතර බෙදාහැරීම',
  reviewQuantities: 'ප්‍රමාණ සමාලෝචනය කරන්න', upcoming: 'ඉදිරි',
  outletDetails: 'වෙළඳසැලේ විස්තර', outlet: 'වෙළඳසැල', depot: 'ගබඩාව', access: 'ප්‍රවේශය',
  account: 'ගිණුම', language: 'භාෂාව', appliesEverywhere: 'මෙම ගිණුමේ සියලු උපාංග සඳහා අදාළ වේ.',
  alerts: 'දැනුම්දීම්', alertsHelp: 'දැනුම්දීම් සීනුවේ පෙන්වන යාවත්කාලීන තෝරන්න.',
  loadingAlerts: 'දැනුම්දීම් සැකසුම් පූරණය වෙමින්…', viewNotifications: 'දැනුම්දීම් බලන්න',
  history: 'ඉතිහාසය', historyHelp: 'පෙර බෙදාහැරීම් සහ සාක්ෂි.', browseHistory: 'ඉතිහාසය බලන්න',
  deliveriesAlert: 'බෙදාහැරීම්', ordersAlert: 'ඇණවුම්', issuesAlert: 'ගැටලු',
  loadingOutlet: 'වෙළඳසැලේ විස්තර පූරණය වෙමින්…', moreOptions: 'තවත් විකල්ප',
  storeInformation: 'වෙළඳසැලේ තොරතුරු', window: 'වේලා පරාසය',
  languageSaved: 'භාෂාව සුරැකිණි.',
  alertsSummary: 'ඇණවුම්, බෙදාහැරීම් සහ ගැටලු යාවත්කාලීන', historySummary: 'පෙර ගමන් සහ ලදුපත්',
}

const ta: StoreMessages = {
  home: 'முகப்பு', orders: 'ஆர்டர்கள்', deliveries: 'விநியோகங்கள்', issues: 'சிக்கல்கள்',
  settings: 'அமைப்புகள்', more: 'மேலும்', signOut: 'வெளியேறு', store: 'கடை',
  deliveryWindow: 'விநியோக நேர வரம்பு', newOrder: 'புதிய ஆர்டர்', reviewAndConfirm: 'சரிபார்த்து உறுதிசெய்',
  hello: 'வணக்கம்', storeManager: 'கடை மேலாளர்', extraDelivery: 'கூடுதல் விநியோகத்திற்கோ வழக்கமில்லாத நாளுக்கோ',
  reviewQuantitiesBeforeCutoff: 'கடைசி நேரத்திற்கு முன் அளவுகளைச் சரிபார்க்கவும்',
  upcomingAndPast: 'வரவிருக்கும் மற்றும் முந்தைய ஆர்டர்கள்:', yourOutlet: 'உங்கள் கடை',
  loadingCutoff: 'கடைசி நேரம் ஏற்றப்படுகிறது…', extraOneOff: 'கூடுதல் விநியோகம்',
  reviewQuantities: 'அளவுகளைச் சரிபார்க்கவும்', upcoming: 'வரவிருக்கும்',
  outletDetails: 'கடை விவரங்கள்', outlet: 'கடை', depot: 'கிடங்கு', access: 'நுழைவு',
  account: 'கணக்கு', language: 'மொழி', appliesEverywhere: 'இந்தக் கணக்கின் எல்லா சாதனங்களுக்கும் பொருந்தும்.',
  alerts: 'அறிவிப்புகள்', alertsHelp: 'அறிவிப்பு மணியில் தோன்றும் புதுப்பிப்புகளைத் தேர்ந்தெடுக்கவும்.',
  loadingAlerts: 'அறிவிப்பு அமைப்புகள் ஏற்றப்படுகின்றன…', viewNotifications: 'அறிவிப்புகளைப் பார்',
  history: 'வரலாறு', historyHelp: 'முந்தைய விநியோகங்களும் சான்றுகளும்.', browseHistory: 'வரலாற்றைப் பார்',
  deliveriesAlert: 'விநியோகங்கள்', ordersAlert: 'ஆர்டர்கள்', issuesAlert: 'சிக்கல்கள்',
  loadingOutlet: 'கடை விவரங்கள் ஏற்றப்படுகின்றன…', moreOptions: 'மேலும் விருப்பங்கள்',
  storeInformation: 'கடை தகவல்', window: 'நேர வரம்பு',
  languageSaved: 'மொழி சேமிக்கப்பட்டது.',
  alertsSummary: 'ஆர்டர், விநியோகம் மற்றும் சிக்கல் புதுப்பிப்புகள்', historySummary: 'முந்தைய பயணங்களும் ரசீதுகளும்',
}

const messages: Record<'en' | 'si' | 'ta', StoreMessages> = { en, si, ta }
export function useStoreMessages() {
  return messages[useAuth().language]
}
