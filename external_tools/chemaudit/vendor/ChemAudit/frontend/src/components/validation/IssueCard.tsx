import type { CheckResult, Severity } from '../../types/validation';
import { cn } from '../../lib/utils';

interface IssueCardProps {
  issue: CheckResult;
  onAtomHover?: (atoms: number[]) => void;
  className?: string;
}

export function IssueCard({ issue, onAtomHover, className = '' }: IssueCardProps) {
  const getSeverityStyles = (severity: Severity) => {
    switch (severity) {
      case 'critical':
        return {
          bg: 'bg-red-500/10 dark:bg-red-500/15',
          border: 'border-red-500/20 dark:border-red-500/30',
          badge: 'bg-red-500/15 text-red-600 dark:text-red-400',
          icon: '🚫',
        };
      case 'error':
        return {
          bg: 'bg-orange-500/10 dark:bg-orange-500/15',
          border: 'border-orange-500/20 dark:border-orange-500/30',
          badge: 'bg-orange-500/15 text-orange-600 dark:text-orange-400',
          icon: '⚠️',
        };
      case 'warning':
        return {
          bg: 'bg-amber-500/10 dark:bg-amber-500/15',
          border: 'border-amber-500/20 dark:border-amber-500/30',
          badge: 'bg-amber-500/15 text-amber-600 dark:text-amber-400',
          icon: '⚡',
        };
      case 'info':
        return {
          bg: 'bg-sky-500/10 dark:bg-sky-500/15',
          border: 'border-sky-500/20 dark:border-sky-500/30',
          badge: 'bg-sky-500/15 text-sky-600 dark:text-sky-400',
          icon: 'ℹ️',
        };
      default:
        return {
          bg: 'bg-[var(--color-surface-sunken)]',
          border: 'border-[var(--color-border)]',
          badge: 'bg-[var(--color-surface-sunken)] text-[var(--color-text-secondary)]',
          icon: '✓',
        };
    }
  };

  const styles = getSeverityStyles(issue.severity);

  const formatCheckName = (name: string) => {
    return name
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  return (
    <div
      className={cn(
        styles.bg,
        'border',
        styles.border,
        'rounded-xl p-4',
        className
      )}
      onMouseEnter={() => onAtomHover?.(issue.affected_atoms)}
      onMouseLeave={() => onAtomHover?.([])}
    >
      <div className="flex items-start gap-3">
        <span className="text-2xl mt-0.5">{styles.icon}</span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <h4 className="font-medium text-[var(--color-text-primary)]">
              {formatCheckName(issue.check_name)}
            </h4>
            <span className={cn('px-2 py-0.5 text-xs font-medium rounded-full', styles.badge)}>
              {issue.severity.toUpperCase()}
            </span>
          </div>
          <p className="text-sm text-[var(--color-text-secondary)]">{issue.message}</p>
          {issue.affected_atoms.length > 0 && (
            <div className="mt-2 text-xs text-[var(--color-text-muted)]">
              Affected atoms: {issue.affected_atoms.join(', ')}
            </div>
          )}
          {Object.keys(issue.details).length > 0 && (
            <div className="mt-2 text-xs text-[var(--color-text-muted)]">
              <details className="cursor-pointer">
                <summary className="hover:text-[var(--color-text-secondary)]">Additional details</summary>
                <pre className="mt-1 p-2 bg-[var(--color-surface-elevated)] rounded-lg border border-[var(--color-border)] overflow-x-auto text-[var(--color-text-secondary)]">
                  {JSON.stringify(issue.details, null, 2)}
                </pre>
              </details>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
