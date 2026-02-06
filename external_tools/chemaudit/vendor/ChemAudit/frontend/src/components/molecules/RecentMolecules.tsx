import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { History, ChevronDown, X, Trash2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { RecentMolecule } from '../../hooks/useRecentMolecules';

interface RecentMoleculesProps {
  recent: RecentMolecule[];
  onSelect: (smiles: string) => void;
  onRemove: (smiles: string) => void;
  onClear: () => void;
}

/**
 * Dropdown showing recently validated molecules.
 * Allows quick re-selection of previous molecules.
 */
export function RecentMolecules({
  recent,
  onSelect,
  onRemove,
  onClear,
}: RecentMoleculesProps) {
  const [isOpen, setIsOpen] = useState(false);

  if (recent.length === 0) {
    return null;
  }

  // Format relative time
  const formatTime = (timestamp: number): string => {
    const now = Date.now();
    const diff = now - timestamp;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  };

  // Truncate long SMILES for display
  const truncateSmiles = (smiles: string, maxLength = 40): string => {
    if (smiles.length <= maxLength) return smiles;
    return smiles.slice(0, maxLength - 3) + '...';
  };

  return (
    <div className="relative">
      {/* Trigger button - Distinct style with amber accent */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          'flex items-center gap-2 px-4 py-1.5 rounded-full text-sm font-medium transition-all',
          'bg-gradient-to-r from-amber-500/10 to-orange-500/10',
          'border border-amber-500/30',
          'text-amber-700 dark:text-amber-400',
          'hover:from-amber-500/20 hover:to-orange-500/20',
          'hover:border-amber-500/50',
          'hover:shadow-[0_0_16px_rgba(217,119,6,0.2)]',
          isOpen && 'from-amber-500/20 to-orange-500/20 border-amber-500/50 shadow-[0_0_16px_rgba(217,119,6,0.25)]'
        )}
      >
        <History className="w-4 h-4" />
        <span>Recent ({recent.length})</span>
        <ChevronDown className={cn(
          'w-4 h-4 transition-transform',
          isOpen && 'rotate-180'
        )} />
      </button>

      {/* Dropdown */}
      <AnimatePresence>
        {isOpen && (
          <>
            {/* Backdrop to close on click outside */}
            <div
              className="fixed inset-0 z-10"
              onClick={() => setIsOpen(false)}
            />

            <motion.div
              initial={{ opacity: 0, y: -10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.95 }}
              transition={{ duration: 0.15 }}
              className={cn(
                'absolute top-full left-0 mt-2 z-20',
                'w-80 max-h-80 overflow-hidden',
                'bg-[var(--color-surface-elevated)] rounded-xl',
                'border border-[var(--color-border)]',
                'shadow-xl shadow-black/10 dark:shadow-black/30'
              )}
            >
              {/* Header with clear button */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--color-border)]">
                <span className="text-sm font-medium text-[var(--color-text-primary)]">
                  Recent Molecules
                </span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onClear();
                    setIsOpen(false);
                  }}
                  className="flex items-center gap-1.5 text-xs text-[var(--color-text-muted)] hover:text-red-500 transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Clear all
                </button>
              </div>

              {/* Molecule list */}
              <div className="max-h-60 overflow-y-auto">
                {recent.map((item) => (
                  <div
                    key={item.smiles}
                    className={cn(
                      'group flex items-center gap-3 px-4 py-2.5',
                      'hover:bg-[var(--color-surface-sunken)] transition-colors',
                      'cursor-pointer'
                    )}
                    onClick={() => {
                      onSelect(item.smiles);
                      setIsOpen(false);
                    }}
                  >
                    {/* Molecule info */}
                    <div className="flex-1 min-w-0">
                      <p className="font-mono text-sm text-[var(--color-text-primary)] truncate">
                        {truncateSmiles(item.smiles)}
                      </p>
                      <p className="text-xs text-[var(--color-text-muted)]">
                        {formatTime(item.timestamp)}
                        {item.name && ` · ${item.name}`}
                      </p>
                    </div>

                    {/* Remove button */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onRemove(item.smiles);
                      }}
                      className={cn(
                        'p-1.5 rounded-lg opacity-0 group-hover:opacity-100',
                        'text-[var(--color-text-muted)] hover:text-red-500',
                        'hover:bg-red-500/10 transition-all'
                      )}
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
