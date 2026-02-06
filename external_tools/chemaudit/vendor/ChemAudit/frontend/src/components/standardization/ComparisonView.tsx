/**
 * Shows comparison of original and standardized molecules.
 * When structures are identical, displays a single viewer with a badge.
 */
import type { ReactElement } from 'react';
import { MoleculeViewer } from '../molecules/MoleculeViewer';
import { CopyButton } from '../ui/CopyButton';

interface ComparisonViewProps {
  originalSmiles: string;
  standardizedSmiles: string | null;
  className?: string;
}

const VIEWER_WIDTH = 550;
const VIEWER_HEIGHT = 300;

const VIEWER_CONTAINER_CLASS =
  'border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-900 p-2 min-h-[280px]';

export function ComparisonView({
  originalSmiles,
  standardizedSmiles,
  className = '',
}: ComparisonViewProps): ReactElement {
  const isIdentical = originalSmiles === standardizedSmiles;

  // If identical, show single viewer with badge
  if (isIdentical || !standardizedSmiles) {
    return (
      <div className={`flex flex-col ${className}`}>
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-[var(--color-text-secondary)]">Structure</span>
            {isIdentical && standardizedSmiles && (
              <span className="text-xs font-medium px-2 py-0.5 rounded bg-yellow-100 dark:bg-yellow-900/30 text-amber-700 dark:text-yellow-400">
                No changes needed
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <code className="text-xs text-[var(--color-text-muted)] font-mono bg-[var(--color-surface-sunken)] px-2 py-0.5 rounded">
              {originalSmiles}
            </code>
            <CopyButton text={originalSmiles} size={12} />
          </div>
        </div>
        <div className={VIEWER_CONTAINER_CLASS}>
          <MoleculeViewer
            smiles={originalSmiles}
            width={VIEWER_WIDTH}
            height={VIEWER_HEIGHT}
          />
        </div>
      </div>
    );
  }

  // Vertical (top/bottom) comparison
  return (
    <div className={`flex flex-col gap-4 ${className}`}>
      {/* Original */}
      <div className="flex flex-col">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-[var(--color-text-secondary)]">Original</span>
          <div className="flex items-center gap-2">
            <code className="text-xs text-[var(--color-text-muted)] font-mono bg-[var(--color-surface-sunken)] px-2 py-0.5 rounded">
              {originalSmiles}
            </code>
            <CopyButton text={originalSmiles} size={12} />
          </div>
        </div>
        <div className={VIEWER_CONTAINER_CLASS}>
          <MoleculeViewer
            smiles={originalSmiles}
            width={VIEWER_WIDTH}
            height={VIEWER_HEIGHT}
          />
        </div>
      </div>

      {/* Arrow */}
      <div className="flex items-center justify-center">
        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-[var(--color-primary)]/10 text-[var(--color-primary)]">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
          </svg>
        </div>
      </div>

      {/* Standardized */}
      <div className="flex flex-col">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-[var(--color-primary)]">Standardized</span>
          <div className="flex items-center gap-2">
            <code className="text-xs text-[var(--color-text-muted)] font-mono bg-[var(--color-surface-sunken)] px-2 py-0.5 rounded">
              {standardizedSmiles}
            </code>
            <CopyButton text={standardizedSmiles} size={12} />
          </div>
        </div>
        <div className="border border-[var(--color-primary)] rounded-lg bg-[var(--color-primary)]/5 p-2 min-h-[280px]">
          <MoleculeViewer
            smiles={standardizedSmiles}
            width={VIEWER_WIDTH}
            height={VIEWER_HEIGHT}
          />
        </div>
      </div>
    </div>
  );
}
