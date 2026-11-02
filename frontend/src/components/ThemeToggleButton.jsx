import { useTheme } from '../theme/useTheme';

export default function ThemeToggleButton({ className = '' }) {
  const { isDarkMode, toggleTheme } = useTheme();
  const label = isDarkMode ? 'Switch to Light Mode' : 'Switch to Dark Mode';

  return (
    <button
      type="button"
      className={`theme-toggle-btn ${className}`.trim()}
      onClick={toggleTheme}
      aria-label={label}
      title={label}
    >
      {isDarkMode ? 'Light Mode' : 'Dark Mode'}
    </button>
  );
}
