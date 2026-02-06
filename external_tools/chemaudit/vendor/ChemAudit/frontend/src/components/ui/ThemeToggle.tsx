import { motion, AnimatePresence } from 'framer-motion';
import { Sun, Moon, Monitor } from 'lucide-react';
import { useThemeContext } from '../../contexts/ThemeContext';
import type { Theme } from '../../hooks/useTheme';
import { cn } from '../../lib/utils';

interface ThemeToggleProps {
  className?: string;
  showLabel?: boolean;
  variant?: 'button' | 'dropdown';
}

const themes: { value: Theme; label: string; icon: typeof Sun }[] = [
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
  { value: 'system', label: 'System', icon: Monitor },
];

/**
 * Claymorphism theme toggle button with stunning animations
 */
export function ThemeToggle({ className, showLabel = false, variant = 'button' }: ThemeToggleProps) {
  const { theme, setTheme, resolvedTheme } = useThemeContext();

  if (variant === 'dropdown') {
    return <ThemeDropdown className={className} />;
  }

  // Direct toggle between light and dark (use dropdown variant for system option)
  const toggleTheme = () => {
    setTheme(resolvedTheme === 'dark' ? 'light' : 'dark');
  };

  const isDark = resolvedTheme === 'dark';
  const Icon = isDark ? Moon : Sun;

  return (
    <motion.button
      onClick={toggleTheme}
      className={cn(
        'relative p-3 rounded-2xl overflow-hidden',
        // Claymorphism base
        'bg-gradient-to-br',
        isDark
          ? 'from-slate-700 via-slate-800 to-slate-900'
          : 'from-amber-50 via-orange-50 to-yellow-50',
        // Claymorphism shadows
        isDark
          ? 'shadow-[inset_2px_2px_6px_rgba(255,255,255,0.05),inset_-2px_-2px_6px_rgba(0,0,0,0.4),0_4px_16px_rgba(0,0,0,0.4)]'
          : 'shadow-[inset_2px_2px_6px_rgba(255,255,255,0.9),inset_-2px_-2px_6px_rgba(0,0,0,0.08),0_4px_16px_rgba(0,0,0,0.1)]',
        // Border
        isDark
          ? 'border border-slate-600/50'
          : 'border border-amber-200/60',
        'transition-all duration-300',
        className
      )}
      whileHover={{
        scale: 1.08,
        boxShadow: isDark
          ? 'inset 2px 2px 8px rgba(255,255,255,0.08), inset -2px -2px 8px rgba(0,0,0,0.5), 0 8px 24px rgba(0,0,0,0.5)'
          : 'inset 2px 2px 8px rgba(255,255,255,1), inset -2px -2px 8px rgba(0,0,0,0.1), 0 8px 24px rgba(251,191,36,0.3)'
      }}
      whileTap={{
        scale: 0.92,
        boxShadow: isDark
          ? 'inset 3px 3px 8px rgba(0,0,0,0.5), inset -1px -1px 4px rgba(255,255,255,0.03), 0 2px 8px rgba(0,0,0,0.3)'
          : 'inset 3px 3px 8px rgba(0,0,0,0.12), inset -1px -1px 4px rgba(255,255,255,0.6), 0 2px 8px rgba(0,0,0,0.08)'
      }}
      aria-label={`Current theme: ${theme}. Click to change.`}
    >
      {/* Glow effect behind icon */}
      <motion.div
        className={cn(
          'absolute inset-0 rounded-2xl opacity-0',
          isDark
            ? 'bg-gradient-to-br from-indigo-500/20 via-purple-500/20 to-blue-500/20'
            : 'bg-gradient-to-br from-amber-300/40 via-orange-300/30 to-yellow-300/40'
        )}
        animate={{ opacity: [0.3, 0.6, 0.3] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
      />

      <AnimatePresence mode="wait" initial={false}>
        <motion.div
          key={resolvedTheme}
          initial={{ rotate: -180, scale: 0, opacity: 0 }}
          animate={{ rotate: 0, scale: 1, opacity: 1 }}
          exit={{ rotate: 180, scale: 0, opacity: 0 }}
          transition={{ duration: 0.4, type: 'spring', stiffness: 200 }}
          className="relative z-10"
        >
          <Icon
            className={cn(
              'w-5 h-5',
              isDark
                ? 'text-indigo-300 drop-shadow-[0_0_8px_rgba(165,180,252,0.6)]'
                : 'text-amber-500 drop-shadow-[0_0_8px_rgba(251,191,36,0.8)]'
            )}
          />
        </motion.div>
      </AnimatePresence>

      {showLabel && (
        <span className={cn(
          'ml-2 text-sm font-medium capitalize',
          isDark ? 'text-slate-200' : 'text-amber-700'
        )}>
          {theme}
        </span>
      )}
    </motion.button>
  );
}

/**
 * Claymorphism theme dropdown selector
 */
function ThemeDropdown({ className }: { className?: string }) {
  const { theme, setTheme, resolvedTheme } = useThemeContext();
  const isDark = resolvedTheme === 'dark';

  return (
    <div className={cn('relative', className)}>
      <div className={cn(
        'flex items-center gap-1 p-1.5 rounded-2xl',
        // Claymorphism container
        isDark
          ? 'bg-slate-800/80 shadow-[inset_1px_1px_4px_rgba(255,255,255,0.03),inset_-1px_-1px_4px_rgba(0,0,0,0.3),0_4px_12px_rgba(0,0,0,0.3)]'
          : 'bg-white/80 shadow-[inset_1px_1px_4px_rgba(255,255,255,0.8),inset_-1px_-1px_4px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.08)]',
        'border',
        isDark ? 'border-slate-700/50' : 'border-amber-100'
      )}>
        {themes.map(({ value, icon: Icon }) => {
          const isActive = theme === value;
          return (
            <motion.button
              key={value}
              onClick={() => setTheme(value)}
              className={cn(
                'relative p-2.5 rounded-xl transition-colors duration-200',
                isActive
                  ? isDark
                    ? 'text-indigo-300'
                    : 'text-amber-600'
                  : isDark
                    ? 'text-slate-500 hover:text-slate-300'
                    : 'text-slate-400 hover:text-slate-600'
              )}
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              aria-label={`Switch to ${value} theme`}
            >
              {isActive && (
                <motion.div
                  layoutId="theme-indicator"
                  className={cn(
                    'absolute inset-0 rounded-xl',
                    isDark
                      ? 'bg-gradient-to-br from-indigo-500/20 to-purple-500/20 shadow-[inset_1px_1px_3px_rgba(255,255,255,0.1),0_2px_8px_rgba(99,102,241,0.3)]'
                      : 'bg-gradient-to-br from-amber-100 to-orange-100 shadow-[inset_1px_1px_3px_rgba(255,255,255,0.8),0_2px_8px_rgba(251,191,36,0.2)]'
                  )}
                  transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                />
              )}
              <Icon className={cn(
                'w-4 h-4 relative z-10',
                isActive && (isDark
                  ? 'drop-shadow-[0_0_6px_rgba(165,180,252,0.6)]'
                  : 'drop-shadow-[0_0_6px_rgba(251,191,36,0.6)]')
              )} />
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}

/**
 * Full theme selector with labels (for settings pages)
 */
export function ThemeSelector({ className }: { className?: string }) {
  const { theme, setTheme, resolvedTheme } = useThemeContext();
  const isDark = resolvedTheme === 'dark';

  return (
    <div className={cn('space-y-2', className)}>
      <label className="text-sm font-medium text-[var(--color-text-primary)]">Theme</label>
      <div className="flex flex-wrap gap-2">
        {themes.map(({ value, label, icon: Icon }) => {
          const isActive = theme === value;
          return (
            <motion.button
              key={value}
              onClick={() => setTheme(value)}
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 rounded-2xl',
                'border-2 transition-all duration-200',
                isActive
                  ? cn(
                      isDark
                        ? 'border-indigo-500/50 bg-gradient-to-br from-indigo-500/20 to-purple-500/20 text-indigo-300'
                        : 'border-amber-400/50 bg-gradient-to-br from-amber-50 to-orange-50 text-amber-700',
                      // Claymorphism for active
                      isDark
                        ? 'shadow-[inset_1px_1px_4px_rgba(255,255,255,0.05),0_4px_12px_rgba(99,102,241,0.2)]'
                        : 'shadow-[inset_1px_1px_4px_rgba(255,255,255,0.8),0_4px_12px_rgba(251,191,36,0.15)]'
                    )
                  : cn(
                      'border-transparent bg-[var(--color-surface-elevated)] text-[var(--color-text-secondary)]',
                      'hover:bg-[var(--color-primary)]/5',
                      'shadow-[inset_1px_1px_3px_rgba(255,255,255,0.5),inset_-1px_-1px_3px_rgba(0,0,0,0.05)]'
                    )
              )}
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.98 }}
            >
              <Icon className={cn(
                'w-4 h-4',
                isActive && (isDark
                  ? 'drop-shadow-[0_0_4px_rgba(165,180,252,0.6)]'
                  : 'drop-shadow-[0_0_4px_rgba(251,191,36,0.6)]')
              )} />
              <span className="text-sm font-medium">{label}</span>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}
