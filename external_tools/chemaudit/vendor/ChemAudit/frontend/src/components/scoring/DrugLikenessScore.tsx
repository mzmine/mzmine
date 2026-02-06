import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Pill,
  CheckCircle2,
  XCircle,
  ChevronDown,
  Beaker
} from 'lucide-react';
import type { DrugLikenessResult } from '../../types/scoring';
import { InfoTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';

interface DrugLikenessScoreProps {
  result: DrugLikenessResult;
}

/**
 * Status badge component
 */
function StatusBadge({ passed, label }: { passed: boolean; label: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium',
        passed
          ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
          : 'bg-red-500/10 text-red-600 dark:text-red-400'
      )}
    >
      {passed ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
      {label}
    </span>
  );
}

/**
 * Property row for displaying key-value pairs
 */
function PropertyRow({
  label,
  value,
  threshold,
  passed
}: {
  label: string;
  value: string | number;
  threshold?: string;
  passed?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-1.5 border-b border-[var(--color-border)]/50 last:border-0">
      <span className="text-xs text-[var(--color-text-muted)]">{label}</span>
      <div className="flex items-center gap-2">
        <span className={cn(
          'text-sm font-medium',
          passed === undefined
            ? 'text-[var(--color-text-primary)]'
            : passed
              ? 'text-emerald-600 dark:text-emerald-400'
              : 'text-red-600 dark:text-red-400'
        )}>
          {typeof value === 'number' ? value.toFixed(2) : value}
        </span>
        {threshold && (
          <span className="text-xs text-[var(--color-text-muted)]">({threshold})</span>
        )}
      </div>
    </div>
  );
}

/**
 * Displays drug-likeness scores including Lipinski, QED, Veber, and more.
 */
export function DrugLikenessScore({ result }: DrugLikenessScoreProps) {
  const [showExtended, setShowExtended] = useState(false);
  const { lipinski, qed, veber, ro3, ghose, egan, muegge, interpretation } = result;

  // QED color based on score
  const qedColor = qed.score >= 0.67
    ? 'text-emerald-500'
    : qed.score >= 0.49
      ? 'text-amber-500'
      : 'text-red-500';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'rounded-2xl p-5',
        'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface)]',
        'border border-[var(--color-border)]'
      )}
    >
      {/* Header */}
      <div className="flex items-center gap-3 mb-4">
        <div className={cn(
          'w-10 h-10 rounded-xl flex items-center justify-center',
          'bg-purple-500/10 text-purple-500'
        )}>
          <Pill className="w-5 h-5" />
        </div>
        <div>
          <h3 className="font-semibold text-[var(--color-text-primary)]">Drug-likeness</h3>
          <p className="text-xs text-[var(--color-text-muted)]">Oral bioavailability prediction</p>
        </div>
      </div>

      {/* Main scores grid */}
      <div className="grid grid-cols-2 gap-3 mb-4">
        {/* QED Score */}
        <div className={cn(
          'rounded-xl p-3',
          'bg-[var(--color-surface-sunken)]'
        )}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs text-[var(--color-text-muted)]">QED Score</span>
            <InfoTooltip
              title="Quantitative Estimate of Drug-likeness"
              content={
                <div className="text-xs">
                  <p>Composite score based on 8 molecular properties.</p>
                  <ul className="mt-1 text-white/70">
                    <li>&ge;0.67: Favorable</li>
                    <li>0.49-0.67: Moderate</li>
                    <li>&lt;0.49: Unfavorable</li>
                  </ul>
                </div>
              }
            />
          </div>
          <div className={cn('text-2xl font-bold', qedColor)}>
            {qed.score.toFixed(2)}
          </div>
          <div className="text-xs text-[var(--color-text-muted)] mt-1">
            {qed.interpretation}
          </div>
        </div>

        {/* Lipinski */}
        <div className={cn(
          'rounded-xl p-3',
          'bg-[var(--color-surface-sunken)]'
        )}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs text-[var(--color-text-muted)]">Lipinski Ro5</span>
            <InfoTooltip
              title="Lipinski's Rule of Five"
              content={
                <div className="text-xs">
                  <p>Predicts oral bioavailability:</p>
                  <ul className="mt-1 text-white/70">
                    <li>MW &le; 500</li>
                    <li>LogP &le; 5</li>
                    <li>HBD &le; 5</li>
                    <li>HBA &le; 10</li>
                  </ul>
                  <p className="mt-1">1 violation allowed</p>
                </div>
              }
            />
          </div>
          <StatusBadge passed={lipinski.passed} label={lipinski.passed ? 'Pass' : 'Fail'} />
          <div className="text-xs text-[var(--color-text-muted)] mt-2">
            {lipinski.violations} violation{lipinski.violations !== 1 ? 's' : ''}
          </div>
        </div>
      </div>

      {/* Lipinski Details */}
      <div className={cn(
        'rounded-xl p-3 mb-3',
        'bg-[var(--color-surface-sunken)]'
      )}>
        <div className="flex items-center gap-2 mb-2">
          <Beaker className="w-4 h-4 text-[var(--color-text-muted)]" />
          <span className="text-xs font-medium text-[var(--color-text-secondary)]">
            Lipinski Properties
          </span>
        </div>
        <div className="grid grid-cols-2 gap-x-4">
          <PropertyRow label="MW" value={lipinski.mw} threshold="≤500" passed={lipinski.details.mw_ok} />
          <PropertyRow label="LogP" value={lipinski.logp} threshold="≤5" passed={lipinski.details.logp_ok} />
          <PropertyRow label="HBD" value={lipinski.hbd} threshold="≤5" passed={lipinski.details.hbd_ok} />
          <PropertyRow label="HBA" value={lipinski.hba} threshold="≤10" passed={lipinski.details.hba_ok} />
        </div>
      </div>

      {/* Additional Rules */}
      <div className="flex flex-wrap gap-2 mb-3">
        <StatusBadge passed={veber.passed} label={`Veber ${veber.passed ? '✓' : '✗'}`} />
        <StatusBadge passed={ro3.passed} label={`Ro3 ${ro3.passed ? '✓' : '✗'}`} />
        {ghose && <StatusBadge passed={ghose.passed} label={`Ghose ${ghose.passed ? '✓' : '✗'}`} />}
        {egan && <StatusBadge passed={egan.passed} label={`Egan ${egan.passed ? '✓' : '✗'}`} />}
        {muegge && <StatusBadge passed={muegge.passed} label={`Muegge ${muegge.passed ? '✓' : '✗'}`} />}
      </div>

      {/* Extended Details Toggle */}
      {(ghose || egan || muegge) && (
        <button
          onClick={() => setShowExtended(!showExtended)}
          className={cn(
            'w-full flex items-center justify-between px-3 py-2 rounded-lg',
            'text-xs text-[var(--color-text-muted)]',
            'hover:bg-[var(--color-surface-sunken)] transition-colors'
          )}
        >
          <span>Extended filter details</span>
          <ChevronDown className={cn(
            'w-4 h-4 transition-transform',
            showExtended && 'rotate-180'
          )} />
        </button>
      )}

      {showExtended && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          className="mt-2 space-y-2"
        >
          {/* Veber Details */}
          <div className="rounded-lg p-2 bg-[var(--color-surface-sunken)] text-xs">
            <div className="font-medium text-[var(--color-text-secondary)] mb-1">Veber Rules</div>
            <div className="text-[var(--color-text-muted)]">
              RotBonds: {veber.rotatable_bonds} (≤10) | TPSA: {veber.tpsa.toFixed(1)} (≤140)
            </div>
          </div>

          {/* Rule of Three */}
          <div className="rounded-lg p-2 bg-[var(--color-surface-sunken)] text-xs">
            <div className="font-medium text-[var(--color-text-secondary)] mb-1">Rule of Three (Fragments)</div>
            <div className="text-[var(--color-text-muted)]">
              MW: {ro3.mw.toFixed(0)} (&lt;300) | LogP: {ro3.logp.toFixed(1)} (≤3) | {ro3.violations} violations
            </div>
          </div>

          {ghose && (
            <div className="rounded-lg p-2 bg-[var(--color-surface-sunken)] text-xs">
              <div className="font-medium text-[var(--color-text-secondary)] mb-1">Ghose Filter</div>
              <div className="text-[var(--color-text-muted)]">
                Atoms: {ghose.atom_count} (20-70) | MR: {ghose.molar_refractivity.toFixed(1)} (40-130)
              </div>
            </div>
          )}
        </motion.div>
      )}

      {/* Interpretation */}
      <div className={cn(
        'mt-3 p-3 rounded-xl text-xs',
        'bg-purple-500/5 border border-purple-500/10',
        'text-[var(--color-text-secondary)]'
      )}>
        {interpretation}
      </div>
    </motion.div>
  );
}
