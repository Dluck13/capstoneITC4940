import { useEffect, useMemo, useState } from 'react';
import { ThemeContext } from './themeContextStore';

const THEME_STORAGE_KEY = 'itops-dashboard-theme';
const LIGHT_THEME = 'light';
const DARK_THEME = 'dark';
const VALID_THEMES = new Set([LIGHT_THEME, DARK_THEME]);

const normalizeTheme = (theme) => (VALID_THEMES.has(theme) ? theme : LIGHT_THEME);

const readStoredTheme = () => {
  if (typeof window === 'undefined') {
    return LIGHT_THEME;
  }

  try {
    return normalizeTheme(window.localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return LIGHT_THEME;
  }
};

const applyTheme = (theme) => {
  if (typeof document === 'undefined') {
    return;
  }

  const nextTheme = normalizeTheme(theme);
  document.documentElement.setAttribute('data-theme', nextTheme);
  document.documentElement.style.colorScheme = nextTheme;
};

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(readStoredTheme);

  useEffect(() => {
    const normalizedTheme = normalizeTheme(theme);
    applyTheme(normalizedTheme);

    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, normalizedTheme);
    } catch {
      // Ignore storage availability issues and keep runtime theme behavior.
    }
  }, [theme]);

  const value = useMemo(
    () => ({
      theme,
      isDarkMode: theme === DARK_THEME,
      setTheme: (nextTheme) => setTheme(normalizeTheme(nextTheme)),
      toggleTheme: () => setTheme((current) => (current === DARK_THEME ? LIGHT_THEME : DARK_THEME)),
    }),
    [theme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
