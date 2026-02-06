import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import type { ValidationResponse } from '../../types/validation';
import { ScoreGauge } from './ScoreGauge';
import { IssueCard } from './IssueCard';
import { CopyButton } from '../ui/CopyButton';
import { cn } from '../../lib/utils';

interface ValidationResultsProps {
  result: ValidationResponse;
  onHighlightAtoms?: (atoms: number[]) => void;
  className?: string;
}

export function ValidationResults({
  result,
  onHighlightAtoms,
  className = '',
}: ValidationResultsProps) {
  const [showAllChecks, setShowAllChecks] = useState(false);

  const { molecule_info, overall_score, issues, all_checks, execution_time_ms } = result;

  return (
    <div className={cn('space-y-6', className)}>
      {/* Score Summary */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4 text-center">
          Validation Score
        </h3>
        <ScoreGauge score={overall_score} size={140} className="mx-auto" />
        <div className="mt-4 text-center text-sm text-[var(--color-text-muted)]">
          Completed in {execution_time_ms.toFixed(0)}ms
        </div>
      </div>

      {/* Molecule Information */}
      <div className="card p-6">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">
          Molecule Information
        </h3>
        <div className="space-y-2 text-sm">
          {molecule_info.canonical_smiles && (
            <div className="flex items-start">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">SMILES:</span>
              <code className="flex-1 text-[var(--color-text-secondary)] font-mono text-xs break-all">
                {molecule_info.canonical_smiles}
              </code>
              <CopyButton text={molecule_info.canonical_smiles} className="ml-2 shrink-0" />
            </div>
          )}
          {molecule_info.molecular_formula && (
            <div className="flex items-center">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">Formula:</span>
              <span className="text-[var(--color-text-secondary)]">{molecule_info.molecular_formula}</span>
            </div>
          )}
          {molecule_info.molecular_weight && (
            <div className="flex items-center">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">Mol. Weight:</span>
              <span className="text-[var(--color-text-secondary)]">
                {molecule_info.molecular_weight.toFixed(2)} g/mol
              </span>
            </div>
          )}
          {molecule_info.inchi && (
            <div className="flex items-start">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">InChI:</span>
              <code className="flex-1 text-[var(--color-text-secondary)] font-mono text-xs break-all">
                {molecule_info.inchi}
              </code>
              <CopyButton text={molecule_info.inchi} className="ml-2 shrink-0" />
            </div>
          )}
          {molecule_info.inchikey && (
            <div className="flex items-start">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">InChIKey:</span>
              <code className="flex-1 text-[var(--color-text-secondary)] font-mono text-xs break-all">
                {molecule_info.inchikey}
              </code>
              <CopyButton text={molecule_info.inchikey} className="ml-2 shrink-0" />
            </div>
          )}
          {molecule_info.num_atoms !== null && (
            <div className="flex items-center">
              <span className="font-medium text-[var(--color-text-secondary)] w-32 shrink-0">Atoms:</span>
              <span className="text-[var(--color-text-secondary)]">{molecule_info.num_atoms}</span>
            </div>
          )}
        </div>
      </div>

      {/* Issues */}
      {issues.length > 0 ? (
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">
            Issues Found ({issues.length})
          </h3>
          <div className="space-y-3">
            {issues.map((issue, index) => (
              <IssueCard
                key={`${issue.check_name}-${index}`}
                issue={issue}
                onAtomHover={onHighlightAtoms}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="rounded-xl p-6 text-center bg-yellow-500/10 border border-yellow-500/20">
          <div className="text-4xl mb-2">✓</div>
          <h3 className="text-lg font-semibold text-amber-600 dark:text-yellow-400 mb-1">
            No Issues Found
          </h3>
          <p className="text-sm text-amber-600/80 dark:text-yellow-400/80">
            All validation checks passed successfully
          </p>
        </div>
      )}

      {/* All Checks (collapsible) */}
      <div className="card p-6">
        <button
          onClick={() => setShowAllChecks(!showAllChecks)}
          className="w-full flex items-center justify-between text-left"
        >
          <h3 className="text-lg font-semibold text-[var(--color-text-primary)]">
            All Checks ({all_checks.length})
          </h3>
          <ChevronDown
            className={cn(
              'w-5 h-5 text-[var(--color-text-muted)] transition-transform',
              showAllChecks && 'rotate-180'
            )}
          />
        </button>

        {showAllChecks && (
          <div className="mt-4 space-y-2">
            {all_checks.map((check, index) => (
              <div
                key={`${check.check_name}-${index}`}
                className="flex items-center justify-between py-2 px-3 bg-[var(--color-surface-sunken)] rounded-lg"
              >
                <div className="flex items-center gap-2">
                  <span className={check.passed ? 'text-amber-500 dark:text-yellow-400' : 'text-red-500'}>
                    {check.passed ? '✓' : '✗'}
                  </span>
                  <span className="text-sm font-medium text-[var(--color-text-primary)]">
                    {check.check_name.replace(/_/g, ' ')}
                  </span>
                </div>
                <span
                  className={cn(
                    'text-xs px-2 py-1 rounded-md font-medium',
                    check.passed
                      ? 'bg-yellow-500/10 text-amber-600 dark:text-yellow-400'
                      : check.severity === 'critical'
                      ? 'bg-red-500/10 text-red-600 dark:text-red-400'
                      : check.severity === 'error'
                      ? 'bg-orange-500/10 text-orange-600 dark:text-orange-400'
                      : check.severity === 'warning'
                      ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
                      : 'bg-sky-500/10 text-sky-600 dark:text-sky-400'
                  )}
                >
                  {check.passed ? 'PASS' : check.severity.toUpperCase()}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
