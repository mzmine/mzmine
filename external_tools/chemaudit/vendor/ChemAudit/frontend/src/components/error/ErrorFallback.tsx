import { motion } from 'framer-motion';
import { AlertTriangle, Home, RotateCcw } from 'lucide-react';
import { FallbackProps } from 'react-error-boundary';
import { cn } from '../../lib/utils';

export function ErrorFallback({ error, resetErrorBoundary }: FallbackProps) {
  const errorMessage = error instanceof Error ? error.message : String(error);
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="min-h-[60vh] flex items-center justify-center p-4"
    >
      <div className="card-glass p-8 max-w-md w-full text-center">
        {/* Icon */}
        <div className={cn(
          'w-16 h-16 mx-auto mb-6 rounded-2xl flex items-center justify-center',
          'bg-red-500/10 dark:bg-red-400/10'
        )}>
          <AlertTriangle className="w-8 h-8 text-red-500 dark:text-red-400" />
        </div>

        {/* Title */}
        <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2 font-display">
          Something went wrong
        </h1>
        <p className="text-[var(--color-text-secondary)] mb-6">
          An unexpected error occurred. You can try again or return home.
        </p>

        {/* Error details (collapsible in production) */}
        {import.meta.env.DEV && (
          <pre className="text-left text-xs bg-[var(--color-surface-sunken)] rounded-lg p-4 mb-6 overflow-auto max-h-32 text-red-500">
            {errorMessage}
          </pre>
        )}

        {/* Actions */}
        <div className="flex gap-3 justify-center">
          <button
            onClick={resetErrorBoundary}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all',
              'bg-[var(--color-primary)] text-white',
              'hover:opacity-90 active:scale-95'
            )}
          >
            <RotateCcw className="w-4 h-4" />
            Try Again
          </button>
          <button
            onClick={() => window.location.href = '/'}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all',
              'bg-[var(--color-surface-elevated)] text-[var(--color-text-secondary)]',
              'border border-[var(--color-border)]',
              'hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]'
            )}
          >
            <Home className="w-4 h-4" />
            Go Home
          </button>
        </div>
      </div>
    </motion.div>
  );
}
