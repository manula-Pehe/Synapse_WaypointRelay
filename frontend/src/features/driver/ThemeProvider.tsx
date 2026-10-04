import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { DriverThemeContext, colorsFor, stored, type Theme } from './theme'

const STORAGE_KEY = 'waypoint.driver.theme'

/**
 * Holds the one copy of the theme for the whole driver app.
 *
 * It has to be a provider rather than a plain hook: the switch lives in the shell but the screens
 * paint themselves from the same colours, and a second `useState` copy would keep rendering dark
 * after the driver had already chosen light.
 *
 * It lives in its own file so `theme.ts` stays free of components, which keeps Vite's fast refresh
 * working on both.
 */
export default function DriverThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(stored)

  useEffect(() => {
    document.documentElement.style.background = colorsFor(theme).bg
    document.documentElement.style.colorScheme = theme
  }, [theme])

  const toggle = useCallback(() => {
    setTheme((current) => {
      const next = current === 'dark' ? 'light' : 'dark'
      try {
        localStorage.setItem(STORAGE_KEY, next)
      } catch {
        // Nothing to do - the switch still works for this session.
      }
      return next
    })
  }, [])

  const value = { theme, colors: colorsFor(theme), toggle }

  return <DriverThemeContext.Provider value={value}>{children}</DriverThemeContext.Provider>
}
