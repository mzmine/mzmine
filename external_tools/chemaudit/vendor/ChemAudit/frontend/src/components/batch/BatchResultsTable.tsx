import { Fragment, useState, useRef, useEffect } from 'react';
import { AlertTriangle, ShieldAlert, FlaskConical, Atom } from 'lucide-react';
import { MoleculeViewer } from '../molecules/MoleculeViewer';
import { CopyButton } from '../ui/CopyButton';
import { cn } from '../../lib/utils';
import type { BatchResult, BatchResultsFilters, SortField } from '../../types/batch';

interface BatchResultsTableProps {
  results: BatchResult[];
  page: number;
  pageSize: number;
  totalResults: number;
  totalPages: number;
  filters: BatchResultsFilters;
  sortBy: SortField;
  sortDir: 'asc' | 'desc';
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  onFiltersChange: (filters: BatchResultsFilters) => void;
  onSortChange: (field: SortField, dir: 'asc' | 'desc') => void;
  isLoading?: boolean;
  selectedIndices: Set<number>;
  onSelectionChange: (indices: Set<number>) => void;
}

/**
 * Paginated results table with filtering and sorting.
 */
export function BatchResultsTable({
  results,
  page,
  pageSize,
  totalResults,
  totalPages,
  filters,
  sortBy,
  sortDir,
  onPageChange,
  onPageSizeChange,
  onFiltersChange,
  onSortChange,
  isLoading = false,
  selectedIndices,
  onSelectionChange,
}: BatchResultsTableProps) {
  const [expandedRow, setExpandedRow] = useState<number | null>(null);
  const [highlightedAtoms, setHighlightedAtoms] = useState<number[]>([]);
  const headerCheckboxRef = useRef<HTMLInputElement>(null);

  const handleSort = (field: SortField) => {
    if (sortBy === field) {
      onSortChange(field, sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      onSortChange(field, 'asc');
    }
  };

  const getScoreColor = (score: number | null | undefined): string => {
    if (score === null || score === undefined) return 'text-[var(--color-text-muted)]';
    if (score >= 80) return 'text-amber-600 dark:text-yellow-400 bg-yellow-500/10';
    if (score >= 50) return 'text-orange-600 dark:text-orange-400 bg-orange-500/10';
    return 'text-red-600 dark:text-red-400 bg-red-500/10';
  };

  const truncateSmiles = (smiles: string, maxLen: number = 30): string => {
    if (smiles.length <= maxLen) return smiles;
    return smiles.substring(0, maxLen) + '...';
  };

  // Calculate checkbox states
  const pageIndices = results.map(r => r.index);
  const allPageSelected = pageIndices.length > 0 && pageIndices.every(idx => selectedIndices.has(idx));
  const somePageSelected = pageIndices.some(idx => selectedIndices.has(idx)) && !allPageSelected;

  // Update header checkbox indeterminate state
  useEffect(() => {
    if (headerCheckboxRef.current) {
      headerCheckboxRef.current.indeterminate = somePageSelected;
    }
  }, [somePageSelected]);

  // Handle header checkbox click
  const handleHeaderCheckboxClick = () => {
    const newSelection = new Set(selectedIndices);
    if (allPageSelected) {
      // Deselect all on page
      pageIndices.forEach(idx => newSelection.delete(idx));
    } else {
      // Select all on page
      pageIndices.forEach(idx => newSelection.add(idx));
    }
    onSelectionChange(newSelection);
  };

  // Handle individual checkbox click
  const handleRowCheckboxClick = (index: number, e: React.MouseEvent) => {
    e.stopPropagation();
    const newSelection = new Set(selectedIndices);
    if (newSelection.has(index)) {
      newSelection.delete(index);
    } else {
      newSelection.add(index);
    }
    onSelectionChange(newSelection);
  };

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-wrap items-center gap-4 p-4 bg-[var(--color-surface-sunken)] rounded-lg">
        {/* Status filter */}
        <div className="flex items-center space-x-2">
          <label className="text-sm text-[var(--color-text-secondary)]">Status:</label>
          <select
            value={filters.status_filter || ''}
            onChange={(e) =>
              onFiltersChange({
                ...filters,
                status_filter: e.target.value as 'success' | 'error' | undefined || undefined,
              })
            }
            className="border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
          >
            <option value="">All</option>
            <option value="success">Success</option>
            <option value="error">Error</option>
          </select>
        </div>

        {/* Score range filter */}
        <div className="flex items-center space-x-2">
          <label className="text-sm text-[var(--color-text-secondary)]">Score:</label>
          <input
            type="number"
            placeholder="Min"
            min="0"
            max="100"
            value={filters.min_score ?? ''}
            onChange={(e) =>
              onFiltersChange({
                ...filters,
                min_score: e.target.value ? Number(e.target.value) : undefined,
              })
            }
            className="w-16 border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
          />
          <span className="text-[var(--color-text-muted)]">-</span>
          <input
            type="number"
            placeholder="Max"
            min="0"
            max="100"
            value={filters.max_score ?? ''}
            onChange={(e) =>
              onFiltersChange({
                ...filters,
                max_score: e.target.value ? Number(e.target.value) : undefined,
              })
            }
            className="w-16 border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
          />
        </div>

        {/* Results info */}
        <div className="ml-auto text-sm text-[var(--color-text-muted)]">
          Showing {results.length} of {totalResults} results
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-[var(--color-surface-sunken)]">
              <th className="w-10 px-4 py-3 text-center">
                <input
                  ref={headerCheckboxRef}
                  type="checkbox"
                  checked={allPageSelected}
                  onChange={handleHeaderCheckboxClick}
                  className="w-4 h-4 cursor-pointer accent-[var(--color-primary)]"
                  aria-label="Select all on page"
                />
              </th>
              <th
                className="px-4 py-3 text-left text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('index')}
              >
                # {sortBy === 'index' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-left text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('name')}
              >
                Name {sortBy === 'name' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-left text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('smiles')}
              >
                SMILES {sortBy === 'smiles' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-center text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('score')}
              >
                Score {sortBy === 'score' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-center text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('qed')}
              >
                QED {sortBy === 'qed' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-center text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('safety')}
              >
                Safety {sortBy === 'safety' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-center text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('status')}
              >
                Status {sortBy === 'status' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
              <th
                className="px-4 py-3 text-center text-xs font-medium text-[var(--color-text-muted)] uppercase cursor-pointer hover:bg-[var(--color-surface-elevated)]"
                onClick={() => handleSort('issues')}
              >
                Issues {sortBy === 'issues' && (sortDir === 'asc' ? '\u2191' : '\u2193')}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--color-border)]">
            {isLoading ? (
              <tr>
                <td colSpan={9} className="px-4 py-8 text-center text-[var(--color-text-muted)]">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--color-primary)] mx-auto mb-2" />
                  Loading results...
                </td>
              </tr>
            ) : results.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-8 text-center text-[var(--color-text-muted)]">
                  No results match the current filters.
                </td>
              </tr>
            ) : (
              results.map((result) => (
                <Fragment key={result.index}>
                  <tr
                    className={`
                      hover:bg-[var(--color-surface-sunken)] cursor-pointer transition-colors
                      ${result.status === 'error' ? 'bg-red-500/5' : ''}
                      ${expandedRow === result.index ? 'bg-[var(--color-primary)]/5' : ''}
                    `}
                    onClick={() => {
                      setExpandedRow(expandedRow === result.index ? null : result.index);
                      setHighlightedAtoms([]);
                    }}
                  >
                    <td className="px-4 py-3 text-center" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selectedIndices.has(result.index)}
                        onChange={(e) => handleRowCheckboxClick(result.index, e as any)}
                        className="w-4 h-4 cursor-pointer accent-[var(--color-primary)]"
                        aria-label={`Select molecule ${result.index + 1}`}
                      />
                    </td>
                    <td className="px-4 py-3 text-sm text-[var(--color-text-muted)]">
                      {result.index + 1}
                    </td>
                    <td className="px-4 py-3 text-sm font-medium text-[var(--color-text-primary)]">
                      {result.name || '-'}
                    </td>
                    <td className="px-4 py-3 text-sm text-[var(--color-text-secondary)] font-mono">
                      {truncateSmiles(result.smiles)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`px-2 py-1 rounded text-sm font-medium ${getScoreColor(
                          result.validation?.overall_score
                        )}`}
                      >
                        {result.validation?.overall_score ?? '-'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      {result.scoring?.druglikeness ? (
                        <span className="px-2 py-1 rounded text-xs font-medium bg-purple-500/10 text-purple-600 dark:text-purple-400">
                          {result.scoring.druglikeness.qed_score.toFixed(2)}
                        </span>
                      ) : (
                        <span className="text-[var(--color-text-muted)]">-</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center">
                      {(() => {
                        const alertCount = result.alerts?.alert_count ?? 0;
                        const allClear = alertCount === 0 && (result.scoring?.safety_filters?.all_passed !== false);
                        return allClear ? (
                          <span className="px-2 py-1 rounded text-xs font-medium bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                            ✓
                          </span>
                        ) : (
                          <span className="px-2 py-1 rounded text-xs font-medium bg-red-500/10 text-red-600 dark:text-red-400">
                            {alertCount}
                          </span>
                        );
                      })()}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`px-2 py-1 rounded text-xs font-medium ${
                          result.status === 'success'
                            ? 'bg-yellow-500/10 text-amber-700 dark:text-yellow-400'
                            : 'bg-red-500/10 text-red-700 dark:text-red-400'
                        }`}
                      >
                        {result.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      {(() => {
                        const failedIssues = result.validation?.issues?.filter(i => !i.passed) ?? [];
                        const count = failedIssues.length;
                        if (count === 0) {
                          return <span className="text-[var(--color-text-muted)]">0</span>;
                        }
                        const hasError = failedIssues.some(i => i.severity === 'error');
                        return (
                          <span className={`px-2 py-1 rounded text-xs font-medium ${
                            hasError
                              ? 'bg-red-500/10 text-red-600 dark:text-red-400'
                              : 'bg-amber-500/10 text-amber-700 dark:text-amber-400'
                          }`}>
                            {count}
                          </span>
                        );
                      })()}
                    </td>
                  </tr>

                  {/* Expanded details — bento grid layout */}
                  {expandedRow === result.index && (
                    <tr>
                      <td colSpan={9} className="p-0">
                        <div className="px-3 py-3 bg-[var(--color-surface-sunken)] border-t border-[var(--color-border)]">
                          {/* Error banner — full width */}
                          {result.error && (
                            <div className="mb-2 rounded-xl bg-red-500/10 dark:bg-red-500/15 border border-red-500/20 p-2.5 flex items-center gap-2.5">
                              <div className="w-8 h-8 rounded-lg bg-red-500/20 flex items-center justify-center flex-shrink-0">
                                <AlertTriangle className="w-4 h-4 text-red-500" />
                              </div>
                              <p className="text-sm text-red-600 dark:text-red-400">{result.error}</p>
                            </div>
                          )}

                          {/* Bento grid */}
                          <div className="grid grid-cols-1 lg:grid-cols-[1fr_1fr_320px] gap-2 auto-rows-auto">

                            {/* ── Tile: Validation Issues ── */}
                            <div className={cn(
                              "card rounded-xl p-3 border-l-[3px]",
                              result.validation?.issues?.length
                                ? result.validation.issues.some(i => i.severity === 'error')
                                  ? 'border-l-red-500'
                                  : 'border-l-amber-500'
                                : 'border-l-emerald-500'
                            )}>
                              <div className="flex items-center gap-2 mb-2">
                                <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-amber-500/15 to-red-500/10 flex items-center justify-center">
                                  <AlertTriangle className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                                </div>
                                <div>
                                  <h4 className="text-xs font-semibold text-[var(--color-text-primary)] tracking-tight">Validation Issues</h4>
                                  <p className="text-[10px] text-[var(--color-text-muted)]">
                                    {result.validation?.issues?.length ?? 0} found
                                  </p>
                                </div>
                              </div>
                              {result.validation?.issues && result.validation.issues.length > 0 ? (
                                <ul className="space-y-1">
                                  {result.validation.issues.map((issue, i) => (
                                    <li
                                      key={i}
                                      className="flex items-start gap-2 text-[12px] cursor-default rounded-lg px-2 py-1 hover:bg-[var(--color-surface-sunken)] transition-colors"
                                      onMouseEnter={() => setHighlightedAtoms(issue.affected_atoms ?? [])}
                                      onMouseLeave={() => setHighlightedAtoms([])}
                                    >
                                      <span className={cn(
                                        "w-1.5 h-1.5 mt-[7px] rounded-full flex-shrink-0",
                                        issue.severity === 'error' ? 'bg-red-500' : 'bg-amber-500'
                                      )} />
                                      <span className="flex-1 text-[var(--color-text-secondary)] leading-snug">{issue.message}</span>
                                      {issue.affected_atoms && issue.affected_atoms.length > 0 && (
                                        <Atom className="w-3 h-3 text-[var(--color-text-muted)] flex-shrink-0 mt-0.5" />
                                      )}
                                    </li>
                                  ))}
                                </ul>
                              ) : (
                                <p className="text-[12px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 px-2 py-1">
                                  No structural issues detected
                                </p>
                              )}
                            </div>

                            {/* ── Tile: Safety Alerts ── */}
                            <div className={cn(
                              "card rounded-xl p-3 border-l-[3px]",
                              (result.alerts?.alert_count ?? 0) > 0 ? 'border-l-red-500' : 'border-l-emerald-500'
                            )}>
                              <div className="flex items-center gap-2 mb-2">
                                <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-red-500/15 to-rose-500/10 flex items-center justify-center">
                                  <ShieldAlert className="w-3.5 h-3.5 text-red-600 dark:text-red-400" />
                                </div>
                                <div>
                                  <h4 className="text-xs font-semibold text-[var(--color-text-primary)] tracking-tight">Safety Alerts</h4>
                                  <p className="text-[10px] text-[var(--color-text-muted)]">
                                    {result.alerts?.alert_count ?? 0} flagged
                                  </p>
                                </div>
                              </div>
                              {result.alerts?.alerts && result.alerts.alerts.length > 0 ? (
                                <div className="space-y-1">
                                  {result.alerts.alerts.map((alert, i) => (
                                    <div
                                      key={i}
                                      className="flex items-center gap-2 text-[12px] cursor-default rounded-lg px-2 py-1 hover:bg-[var(--color-surface-sunken)] transition-colors"
                                      onMouseEnter={() => setHighlightedAtoms(alert.matched_atoms ?? [])}
                                      onMouseLeave={() => setHighlightedAtoms([])}
                                    >
                                      <span className={cn(
                                        "inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold shrink-0 uppercase tracking-wide",
                                        alert.severity === 'critical'
                                          ? 'bg-red-500/20 text-red-600 dark:text-red-400'
                                          : 'bg-amber-500/15 text-amber-700 dark:text-amber-400'
                                      )}>
                                        {alert.catalog}
                                      </span>
                                      <span className="text-[var(--color-text-secondary)] truncate">{alert.rule_name}</span>
                                      {alert.matched_atoms && alert.matched_atoms.length > 0 && (
                                        <Atom className="w-3 h-3 text-[var(--color-text-muted)] flex-shrink-0 ml-auto" />
                                      )}
                                    </div>
                                  ))}
                                  {result.scoring?.safety_filters && (
                                    <div className="mt-2 pt-2 border-t border-[var(--color-border)] text-[11px] text-[var(--color-text-muted)] flex flex-wrap gap-x-3 gap-y-0.5 px-2">
                                      <span>PAINS: {result.scoring.safety_filters.pains_passed ? '✓' : '✗'}</span>
                                      <span>Brenk: {result.scoring.safety_filters.brenk_passed ? '✓' : '✗'}</span>
                                      {result.scoring.safety_filters.nih_passed !== undefined && (
                                        <span>NIH: {result.scoring.safety_filters.nih_passed ? '✓' : '✗'}</span>
                                      )}
                                      {result.scoring.safety_filters.zinc_passed !== undefined && (
                                        <span>ZINC: {result.scoring.safety_filters.zinc_passed ? '✓' : '✗'}</span>
                                      )}
                                      {result.scoring.safety_filters.chembl_passed !== undefined && (
                                        <span>ChEMBL: {result.scoring.safety_filters.chembl_passed ? '✓' : '✗'}</span>
                                      )}
                                    </div>
                                  )}
                                </div>
                              ) : (
                                <p className="text-[12px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 px-2 py-1">
                                  All safety screens passed
                                </p>
                              )}
                            </div>

                            {/* ── Tile: Molecule Viewer (right, spans 2 rows) ── */}
                            <div className="card-glass rounded-xl p-3 lg:row-span-2 flex flex-col">
                              {result.status === 'success' ? (
                                <>
                                  <div className="flex items-center gap-2 mb-2">
                                    <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-[var(--color-primary)]/15 to-[var(--color-accent)]/10 flex items-center justify-center">
                                      <FlaskConical className="w-3.5 h-3.5 text-[var(--color-primary)]" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                      <div className="flex items-center gap-1.5">
                                        <h4 className="text-xs font-semibold text-[var(--color-text-primary)] tracking-tight">Structure</h4>
                                        {result.standardization?.standardized_smiles && result.standardization.changed && (
                                          <span className="px-1.5 py-0.5 rounded text-[9px] font-medium bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 uppercase tracking-wide">
                                            Standardized
                                          </span>
                                        )}
                                      </div>
                                      {result.name && (
                                        <p className="text-[10px] text-[var(--color-text-muted)] truncate">{result.name}</p>
                                      )}
                                    </div>
                                  </div>
                                  <div className="flex-1 flex items-center justify-center rounded-xl bg-white dark:bg-gray-900/50 border border-[var(--color-border-subtle)] min-h-[180px]">
                                    {/* Use standardized SMILES for 2D depiction when available */}
                                    <MoleculeViewer
                                      smiles={result.standardization?.standardized_smiles || result.smiles}
                                      highlightAtoms={highlightedAtoms}
                                      width={280}
                                      height={220}
                                    />
                                  </div>
                                  {highlightedAtoms.length > 0 && (
                                    <div className="mt-1.5 flex justify-center">
                                      <div className="px-2.5 py-0.5 rounded-full bg-[var(--color-primary)]/10 border border-[var(--color-primary)]/20">
                                        <p className="text-[10px] font-medium text-[var(--color-primary)] whitespace-nowrap">
                                          {highlightedAtoms.length} atom{highlightedAtoms.length !== 1 ? 's' : ''} highlighted
                                        </p>
                                      </div>
                                    </div>
                                  )}
                                  {/* Show both original and standardized SMILES when standardization was applied */}
                                  {result.standardization?.standardized_smiles && result.standardization.changed ? (
                                    <div className="mt-2 space-y-1.5">
                                      {/* Original SMILES */}
                                      <div className="rounded-lg bg-[var(--color-surface-sunken)] border border-[var(--color-border-subtle)] px-2.5 py-1.5">
                                        <p className="text-[9px] font-medium text-[var(--color-text-muted)] uppercase tracking-wide mb-0.5">Original</p>
                                        <div className="flex items-start gap-2">
                                          <p className="text-[11px] font-mono text-[var(--color-text-secondary)] break-all flex-1 leading-relaxed">
                                            {result.smiles}
                                          </p>
                                          <CopyButton text={result.smiles} size={13} className="mt-0.5 flex-shrink-0" />
                                        </div>
                                      </div>
                                      {/* Standardized SMILES */}
                                      <div className="rounded-lg bg-emerald-500/5 border border-emerald-500/20 px-2.5 py-1.5">
                                        <p className="text-[9px] font-medium text-emerald-600 dark:text-emerald-400 uppercase tracking-wide mb-0.5">Standardized</p>
                                        <div className="flex items-start gap-2">
                                          <p className="text-[11px] font-mono text-[var(--color-text-secondary)] break-all flex-1 leading-relaxed">
                                            {result.standardization.standardized_smiles}
                                          </p>
                                          <CopyButton text={result.standardization.standardized_smiles} size={13} className="mt-0.5 flex-shrink-0" />
                                        </div>
                                      </div>
                                    </div>
                                  ) : (
                                    <div className="mt-2 flex items-start gap-2 rounded-lg bg-[var(--color-surface-sunken)] border border-[var(--color-border-subtle)] px-2.5 py-1.5">
                                      <p className="text-[11px] font-mono text-[var(--color-text-secondary)] break-all flex-1 leading-relaxed">
                                        {result.smiles}
                                      </p>
                                      <CopyButton text={result.smiles} size={13} className="mt-0.5 flex-shrink-0" />
                                    </div>
                                  )}
                                </>
                              ) : (
                                <div className="flex-1 flex items-center justify-center rounded-xl bg-red-500/5 border border-red-500/10 min-h-[180px]">
                                  <p className="text-sm text-red-500/70">Parse failed</p>
                                </div>
                              )}
                            </div>

                            {/* ── Tile: Scoring Grid (spans 2 cols on lg) ── */}
                            {result.scoring && (
                              <div className="card rounded-xl p-3 lg:col-span-2">
                                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                                  {/* ML-Readiness */}
                                  {result.scoring.ml_readiness && (
                                    <div className="rounded-lg bg-gradient-to-br from-[var(--color-primary)]/[0.07] to-transparent border border-[var(--color-border-subtle)] p-2.5 text-center">
                                      <p className="text-[10px] uppercase tracking-wider text-[var(--color-text-muted)] font-medium mb-0.5">ML-Readiness</p>
                                      <p className="text-xl font-bold text-gradient">{result.scoring.ml_readiness.score}</p>
                                    </div>
                                  )}

                                  {/* Drug-likeness */}
                                  {result.scoring.druglikeness && (
                                    <div className="rounded-lg bg-gradient-to-br from-purple-500/[0.07] to-transparent border border-[var(--color-border-subtle)] p-2.5 text-center">
                                      <p className="text-[10px] uppercase tracking-wider text-[var(--color-text-muted)] font-medium mb-0.5">QED Score</p>
                                      <p className="text-xl font-bold text-purple-600 dark:text-purple-400">
                                        {result.scoring.druglikeness.qed_score.toFixed(2)}
                                      </p>
                                      <p className={cn(
                                        "text-[10px] font-medium mt-0.5",
                                        result.scoring.druglikeness.lipinski_passed ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                                      )}>
                                        Lipinski: {result.scoring.druglikeness.lipinski_passed ? 'Pass' : 'Fail'}
                                      </p>
                                    </div>
                                  )}

                                  {/* Safety Summary */}
                                  {(result.scoring.safety_filters || (result.alerts?.alerts && result.alerts.alerts.length > 0)) && (
                                    <div className="rounded-lg bg-gradient-to-br from-emerald-500/[0.07] to-transparent border border-[var(--color-border-subtle)] p-2.5 text-center">
                                      <p className="text-[10px] uppercase tracking-wider text-[var(--color-text-muted)] font-medium mb-0.5">Safety</p>
                                      {(() => {
                                        const alertCount = result.alerts?.alert_count ?? 0;
                                        const allClear = alertCount === 0 && (result.scoring.safety_filters?.all_passed !== false);
                                        return (
                                          <p className={cn(
                                            "text-xl font-bold",
                                            allClear ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                                          )}>
                                            {allClear ? 'Clear' : alertCount}
                                          </p>
                                        );
                                      })()}
                                      <p className="text-[10px] text-[var(--color-text-muted)] mt-0.5">
                                        {(result.alerts?.alert_count ?? 0) === 0 ? 'No alerts' : 'alerts flagged'}
                                      </p>
                                    </div>
                                  )}

                                  {/* ADMET */}
                                  {result.scoring.admet && (
                                    <div className="rounded-lg bg-gradient-to-br from-cyan-500/[0.07] to-transparent border border-[var(--color-border-subtle)] p-2.5 text-center">
                                      <p className="text-[10px] uppercase tracking-wider text-[var(--color-text-muted)] font-medium mb-0.5">Synth. Access.</p>
                                      <p className="text-xl font-bold text-cyan-600 dark:text-cyan-400">
                                        {result.scoring.admet.sa_score.toFixed(1)}
                                      </p>
                                      <p className="text-[10px] text-[var(--color-text-muted)] mt-0.5">
                                        {result.scoring.admet.sa_classification} | Fsp3: {result.scoring.admet.fsp3.toFixed(2)}
                                      </p>
                                    </div>
                                  )}
                                </div>

                                {/* Interpretation */}
                                {result.scoring.ml_readiness?.interpretation && (
                                  <p className="text-[11px] text-[var(--color-text-secondary)] mt-2 px-1 leading-snug">
                                    {result.scoring.ml_readiness.interpretation}
                                  </p>
                                )}
                              </div>
                            )}

                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between px-4 py-3 bg-[var(--color-surface-sunken)] rounded-lg">
        <div className="flex items-center space-x-2">
          <label className="text-sm text-[var(--color-text-secondary)]">Per page:</label>
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
          >
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => onPageChange(page - 1)}
            disabled={page <= 1}
            className="px-3 py-1 border border-[var(--color-border)] rounded text-sm text-[var(--color-text-secondary)] disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[var(--color-surface-elevated)] transition-colors"
          >
            Previous
          </button>
          <span className="text-sm text-[var(--color-text-secondary)]">
            Page {page} of {totalPages}
          </span>
          <button
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages}
            className="px-3 py-1 border border-[var(--color-border)] rounded text-sm text-[var(--color-text-secondary)] disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[var(--color-surface-elevated)] transition-colors"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
