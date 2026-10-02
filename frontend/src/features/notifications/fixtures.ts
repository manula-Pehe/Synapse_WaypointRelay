import type { Role } from '../../app/auth'
import type { Notification } from './data'
const t = (en: string, si: string, ta: string) => ({ en, si, ta })
// Fictional quantities, IDs and times; these are not operational delivery records.
const store: Notification[] = [
  { id: 's-delivery', severity: 'critical', category: 'deliveries', day: 'today', time: '7:18 AM', icon: 'check', needsAction: true, read: false,
    title: t('Delivered · 46 of 48 chilled cases', 'බෙදාහැර ඇත · ශීත කළ පෙට්ටි 48න් 46ක්', 'விநியோகிக்கப்பட்டது · 48 குளிரூட்டப்பட்ட பெட்டிகளில் 46'),
    message: t('Please confirm what you received. A replacement is being arranged.', 'ඔබට ලැබුණු දේ තහවුරු කරන්න. ආදේශකයක් සූදානම් කරමින් පවතී.', 'நீங்கள் பெற்றதை உறுதிப்படுத்தவும். மாற்று ஏற்பாடு செய்யப்படுகிறது.') },
  { id: 's-arrival', severity: 'warning', category: 'deliveries', day: 'today', time: '6:42 AM', icon: 'warning', read: false,
    title: t('Arrival changed · now 7:15–7:45 AM', 'පැමිණීම වෙනස් වී ඇත · පෙ.ව. 7:15–7:45', 'வருகை மாறியது · காலை 7:15–7:45'),
    message: t('Your truck left the depot 12 min late. We’ll update you if this changes again.', 'ට්‍රක් රථය විනාඩි 12ක් ප්‍රමාද වී පිටත් විය. වෙනස්කම් දැනුම් දෙන්නෙමු.', 'லாரி 12 நிமிடங்கள் தாமதமாகப் புறப்பட்டது. மீண்டும் மாறினால் அறிவிப்போம்.') },
  { id: 's-issue', severity: 'info', category: 'issues', day: 'today', time: '8:24 AM', icon: 'info', read: false,
    title: t('Issue answered · DEMO-021', 'ගැටලුවට පිළිතුරු ලබා දී ඇත · DEMO-021', 'சிக்கலுக்கு பதில் அளிக்கப்பட்டது · DEMO-021'),
    message: t('Dispatch booked replacement cases for the next delivery.', 'ඊළඟ බෙදාහැරීම සඳහා ආදේශක පෙට්ටි වෙන් කර ඇත.', 'அடுத்த விநியோகத்திற்கு மாற்றுப் பெட்டிகள் பதிவு செய்யப்பட்டன.') },
  { id: 's-plan', severity: 'info', category: 'deliveries', day: 'yesterday', time: '7:20 PM', icon: 'truck', read: true,
    title: t('Plan published · morning delivery window', 'සැලැස්ම ප්‍රකාශිතයි · උදෑසන බෙදාහැරීම', 'திட்டம் வெளியிடப்பட்டது · காலை விநியோகம்'),
    message: t('Predicted arrival 6:45–7:15 AM.', 'අපේක්ෂිත පැමිණීම පෙ.ව. 6:45–7:15.', 'எதிர்பார்க்கப்படும் வருகை காலை 6:45–7:15.') },
  { id: 's-order', severity: 'info', category: 'orders', day: 'yesterday', time: '2:25 PM', icon: 'check', read: true,
    title: t('Orders confirmed for the next run', 'ඊළඟ වාරය සඳහා ඇණවුම් තහවුරු කර ඇත', 'அடுத்த சுற்றுக்கான ஆர்டர்கள் உறுதிசெய்யப்பட்டன'),
    message: t('Ambient and chilled orders are confirmed.', 'සාමාන්‍ය සහ ශීත කළ ඇණවුම් තහවුරු කර ඇත.', 'சாதாரண மற்றும் குளிரூட்டப்பட்ட ஆர்டர்கள் உறுதிசெய்யப்பட்டன.') },
  { id: 's-reminder', severity: 'warning', category: 'orders', day: 'yesterday', time: '3:10 PM', icon: 'warning', read: true,
    title: t('Reminder · chilled order needs confirmation', 'මතක් කිරීම · ශීත කළ ඇණවුම තහවුරු කරන්න', 'நினைவூட்டல் · குளிரூட்டப்பட்ட ஆர்டரை உறுதிசெய்யவும்'),
    message: t('Orders close at 4:00 PM.', 'ඇණවුම් ප.ව. 4:00ට අවසන් වේ.', 'ஆர்டர்கள் மாலை 4:00 மணிக்கு முடிவடையும்.') },
]
const dispatch: Notification[] = [
  { ...store[0], id: 'd-breakdown', icon: 'critical', time: '6:54 AM', title: t('Demo vehicle breakdown · trip 2', 'ආදර්ශ වාහන බිඳවැටීම · ගමන 2', 'மாதிரி வாகனப் பழுது · பயணம் 2'), message: t('2 stops left · chilled delivery needs reassignment.', 'නැවතුම් 2ක් ඉතිරියි · නැවත පැවරීම අවශ්‍යයි.', '2 நிறுத்தங்கள் மீதமுள்ளன · மறுஒதுக்கீடு தேவை.') },
  { ...store[0], id: 'd-conflict', icon: 'critical', time: '6:16 AM', title: t('Sync conflict · demo outlet', 'සමමුහුර්ත ගැටුම · ආදර්ශ අලෙවිසැල', 'ஒத்திசைவு முரண்பாடு · மாதிரிக் கடை'), message: t('Delivered offline after your move.', 'මාරු කිරීමෙන් පසු නොබැඳිව බෙදාහැර ඇත.', 'மாற்றத்திற்குப் பிறகு இணையமின்றி விநியோகிக்கப்பட்டது.') },
  { ...store[0], id: 'd-failed', icon: 'critical', time: '6:05 AM', title: t('Failed delivery · demo outlet', 'බෙදාහැරීම අසාර්ථකයි · ආදර්ශ අලෙවිසැල', 'விநியோகம் தோல்வி · மாதிரிக் கடை'), message: t('Store closed · cases returning to depot.', 'අලෙවිසැල වසා ඇත · පෙට්ටි ආපසු එයි.', 'கடை மூடப்பட்டுள்ளது · பெட்டிகள் திரும்புகின்றன.') },
  { ...store[1], id: 'd-late', time: '6:42 AM', title: t('Late risk · demo outlet', 'ප්‍රමාද අවදානම · ආදර්ශ අලෙවිසැල', 'தாமத அபாயம் · மாதிரிக் கடை') },
  { ...store[1], id: 'd-short', time: '4:20 AM', title: t('Shortfall at dock · demo outlet', 'පැටවීමේදී හිඟයක් · ආදර්ශ අලෙවිසැල', 'ஏற்றுமுனையில் பற்றாக்குறை · மாதிரிக் கடை'), message: t('3 cases missing · replacement being arranged.', 'පෙට්ටි 3ක් අඩුයි · ආදේශක සූදානම් කරයි.', '3 பெட்டிகள் குறைவு · மாற்று ஏற்பாடு செய்யப்படுகிறது.') },
  { ...store[2], id: 'd-issue', title: t('Issue reported · DEMO-021', 'ගැටලුව වාර්තා කර ඇත · DEMO-021', 'சிக்கல் தெரிவிக்கப்பட்டது · DEMO-021') },
  { ...store[3], id: 'd-online', read: false, title: t('Demo vehicle back online', 'ආදර්ශ වාහනය නැවත සබැඳියි', 'மாதிரி வாகனம் மீண்டும் இணையத்தில்'), message: t('4 stops synced successfully.', 'නැවතුම් 4ක් සමමුහුර්ත කර ඇත.', '4 நிறுத்தங்கள் ஒத்திசைக்கப்பட்டன.') },
  { ...store[4], id: 'd-orders', read: false, title: t('Yesterday · unconfirmed orders', 'ඊයේ · තහවුරු නොකළ ඇණවුම්', 'நேற்று · உறுதிசெய்யப்படாத ஆர்டர்கள்'), message: t('Reminders sent · all resolved.', 'මතක් කිරීම් යවා ඇත · සියල්ල විසඳා ඇත.', 'நினைவூட்டல்கள் அனுப்பப்பட்டன · அனைத்தும் தீர்க்கப்பட்டன.') },
]
export function notificationFixtures(role: Role): Notification[] {
  return role === 'STORE_MANAGER' ? store : role === 'DISPATCHER' ? dispatch : []
}
