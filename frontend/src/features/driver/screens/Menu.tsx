import { useNavigate } from 'react-router-dom'
import { useSyncStatus } from '../../../lib/offline'
import DriverLayout from '../DriverLayout'
import { Button, Card, Label } from '../components'
import { languageName, t, type Language } from '../i18n'

export interface MenuProps {
  language: Language
  onLanguage: (language: Language) => void
  onToggleTheme: () => void
}

const LANGUAGES: Language[] = ['en', 'si', 'ta']

/** R7 - theme, language, call the depot, report a problem, sign out. */
export default function Menu({ language, onLanguage, onToggleTheme }: MenuProps) {
  const navigate = useNavigate()
  const { waiting } = useSyncStatus()

  return (
    <DriverLayout
      title={t(language, 'driver.menu.title')}
      language={language}
      onToggleTheme={onToggleTheme}
      onBack={() => navigate('/driver/stops')}
    >
      <Card className="space-y-2">
        <Label>{t(language, 'driver.menu.language')}</Label>
        <div className="flex gap-2">
          {LANGUAGES.map((option) => (
            <Button
              key={option}
              variant={option === language ? 'primary' : 'secondary'}
              onClick={() => onLanguage(option)}
              testId={`lang-${option}`}
            >
              {languageName(option)}
            </Button>
          ))}
        </div>
      </Card>

      <Card>
        <Label>{t(language, 'driver.sync.synced')}</Label>
        <Button variant="ghost" onClick={() => navigate('/driver/sync')}>
          {waiting > 0 ? t(language, 'driver.sync.waiting', { count: waiting }) : t(language, 'driver.sync.synced')}
        </Button>
      </Card>

      <Button variant="secondary" full onClick={() => navigate('/driver/problem')} testId="report-problem">
        {t(language, 'driver.menu.problem')}
      </Button>

      <Button variant="secondary" full onClick={() => undefined}>
        {t(language, 'driver.menu.depot')}
      </Button>

      <Button variant="ghost" full onClick={() => navigate('/driver/sign-in')} testId="sign-out">
        {t(language, 'driver.menu.signout')}
      </Button>
    </DriverLayout>
  )
}