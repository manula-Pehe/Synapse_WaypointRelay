// Driver theme. Dark is the default because the driver app is used in a cab before dawn
// The sun/moon switch on every screen flips it and the choice is remembered.

import { createContext, useContext } from 'react'

export type Theme = 'dark' | 'light'

const STORAGE_KEY = 'waypoint.driver.theme'
export interface DriverColors {
  bg: string
  surface: string
  surface2: string
  border: string
  ink: string
  ink2: string
  brand: string
  /** Text on a filled brand/danger button - flips, because the fills flip with the theme. */
  onAccent: string
  danger: string
  warn: string
  ok: string
}

const DARK: DriverColors = {
  bg: '#10131B',
  surface: '#1A1E25',
  surface2: '#23272E',
  border: '#2E333B',
  ink: '#E8E9EE',
  ink2: '#A7A9BC',
  brand: '#8DB8F2',
  onAccent: '#0B0E14',
  danger: '#E58A8A',
  warn: '#E0C078',
  ok: '#8FCB9B',
}

const LIGHT: DriverColors = {
  bg: '#F5F7FA',
  surface: '#FFFFFF',
  surface2: '#EDF0F5',
  border: '#DCE1E9',
  ink: '#151922',
  ink2: '#5A6172',
  brand: '#1D4E89',
  onAccent: '#FFFFFF',
  danger: '#B3453F',
  warn: '#8A6A18',
  ok: '#2F7A45',
}

export function colorsFor(theme: Theme): DriverColors {
  return theme === 'dark' ? DARK : LIGHT
}

export function stored(): Theme {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved === 'light' || saved === 'dark' ? saved : 'dark'
  } catch {
    // Private browsing can refuse storage; the driver still gets a working app, just not a remembered choice.
    return 'dark'
  }
}

export interface DriverTheme {
  theme: Theme
  colors: DriverColors
  toggle: () => void
}

export const DriverThemeContext = createContext<DriverTheme | null>(null)

/** Dark by default, remembered across reloads, and the same on every screen. */
export function useDriverTheme(): DriverTheme {
  const theme = useContext(DriverThemeContext)
  if (!theme) {
    throw new Error('useDriverTheme must be used inside <DriverThemeProvider>')
  }
  return theme
}