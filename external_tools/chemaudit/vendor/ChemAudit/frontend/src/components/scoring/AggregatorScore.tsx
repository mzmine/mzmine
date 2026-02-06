import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Layers,
  AlertTriangle,
  ChevronDown,
  Info
} from 'lucide-react';
import type { AggregatorLikelihoodResult } from '../../types/scoring';
import { InfoTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';

interface AggregatorScoreProps {
  result: AggregatorLikelihoodResult;
}

/**
 * Get color scheme based on likelihood
 */
function getLikelihoodColor(likelihood: string) {
  switch (likelihood) {
    case 'low':
      return {
        bg: 'bg-emerald-500/10',
        text: 'text-emerald-600 dark:text-emerald-400',
        border: 'border-emerald-500/20',
        gradient: 'from-emerald-500/20 to-emerald-500/5'
      };
    case 'moderate':
      return {
        bg: 'bg-amber-500/10',
        text: 'text-amber-600 dark:text-amber-400',
        border: 'border-amber-500/20',
        gradient: 'from-amber-500/20 to-amber-500/5'
      };
    case 'high':
      return {
        bg: 'bg-red-500/10',
        text: 'text-red-600 dark:text-red-400',
        border: 'border-red-500/20',
        gradient: 'from-red-500/20 to-red-500/5'
      };
    default:
      return {
        bg: 'bg-slate-500/10',
        text: 'text-slate-600 dark:text-slate-400',
        border: 'border-slate-500/20',
        gradient: 'from-slate-500/20 to-slate-500/5'
      };
  }
}

/**
 * Displays aggregator likelihood prediction results.
 */
export function AggregatorScore({ result }: AggregatorScoreProps) {
  const [showDetails, setShowDetails] = useState(false);
  const {
    likelihood,
    risk_score,
    logp,
    tpsa,
    mw,
    aromatic_rings,
    risk_factors,
    interpretation
  } = result;

  const color = getLikelihoodColor(likelihood);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'rounded-2xl p-5',
        'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface)]',
        'border',
        color.border
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={cn(
            'w-10 h-10 rounded-xl flex items-center justify-center',
            color.bg, color.text
          )}>
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-[var(--color-text-primary)]">Aggregator Likelihood</h3>
              <InfoTooltip
                title="What is Aggregation?"
                content={
                  <div className="text-xs space-y-2">
                    <p>Colloidal aggregation occurs when molecules form non-specific aggregates in solution.</p>
                    <p className="text-white/80">Aggregators can cause:</p>
                    <ul className="list-disc list-inside text-white/70">
                      <li>False positives in HTS assays</li>
                      <li>Non-specific enzyme inhibition</li>
                      <li>Misleading SAR data</li>
                    </ul>
                    <p className="text-white/60 mt-2">High LogP and low TPSA increase aggregation risk.</p>
                  </div>
                }
              />
            </div>
            <p className="text-xs text-[var(--color-text-muted)]">Colloidal aggregate formation risk</p>
          </div>
        </div>

        {/* Risk badge */}
        <div className={cn(
          'flex flex-col items-end gap-1'
        )}>
          <span className={cn(
            'px-3 py-1 rounded-full text-sm font-medium capitalize',
            color.bg, color.text
          )}>
            {likelihood} risk
          </span>
          <span className="text-xs text-[var(--color-text-muted)]">
            Score: {(risk_score * 100).toFixed(0)}%
          </span>
        </div>
      </div>

      {/* Risk score bar */}
      <div className="mb-4">
        <div className="flex items-center justify-between text-xs text-[var(--color-text-muted)] mb-1">
          <span>Low risk</span>
          <span>High risk</span>
        </div>
        <div className="h-2 bg-[var(--color-surface-sunken)] rounded-full overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${risk_score * 100}%` }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
            className={cn(
              'h-full rounded-full',
              likelihood === 'low' && 'bg-emerald-500',
              likelihood === 'moderate' && 'bg-amber-500',
              likelihood === 'high' && 'bg-red-500'
            )}
          />
        </div>
      </div>

      {/* Key metrics */}
      <div className="grid grid-cols-4 gap-2 mb-4">
        <div className="text-center p-2 rounded-lg bg-[var(--color-surface-sunken)]">
          <div className="text-lg font-bold text-[var(--color-text-primary)]">{logp.toFixed(1)}</div>
          <div className="text-xs text-[var(--color-text-muted)]">LogP</div>
        </div>
        <div className="text-center p-2 rounded-lg bg-[var(--color-surface-sunken)]">
          <div className="text-lg font-bold text-[var(--color-text-primary)]">{tpsa.toFixed(0)}</div>
          <div className="text-xs text-[var(--color-text-muted)]">TPSA</div>
        </div>
        <div className="text-center p-2 rounded-lg bg-[var(--color-surface-sunken)]">
          <div className="text-lg font-bold text-[var(--color-text-primary)]">{mw.toFixed(0)}</div>
          <div className="text-xs text-[var(--color-text-muted)]">MW</div>
        </div>
        <div className="text-center p-2 rounded-lg bg-[var(--color-surface-sunken)]">
          <div className="text-lg font-bold text-[var(--color-text-primary)]">{aromatic_rings}</div>
          <div className="text-xs text-[var(--color-text-muted)]">Ar Rings</div>
        </div>
      </div>

      {/* Risk factors (expandable) */}
      {risk_factors.length > 0 && (
        <div className="mb-4">
          <button
            onClick={() => setShowDetails(!showDetails)}
            className={cn(
              'w-full flex items-center justify-between px-3 py-2 rounded-xl',
              'bg-[var(--color-surface-sunken)]',
              'text-sm text-[var(--color-text-secondary)]',
              'hover:bg-[var(--color-surface-elevated)] transition-colors'
            )}
          >
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <span>{risk_factors.length} risk factor{risk_factors.length !== 1 ? 's' : ''} identified</span>
            </div>
            <ChevronDown className={cn(
              'w-4 h-4 transition-transform',
              showDetails && 'rotate-180'
            )} />
          </button>

          {showDetails && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="mt-2 p-3 rounded-xl bg-[var(--color-surface-sunken)]"
            >
              <ul className="space-y-1.5">
                {risk_factors.map((factor, i) => (
                  <li key={i} className="flex items-start gap-2 text-xs text-[var(--color-text-secondary)]">
                    <AlertTriangle className="w-3 h-3 text-amber-500 flex-shrink-0 mt-0.5" />
                    <span>{factor}</span>
                  </li>
                ))}
              </ul>
            </motion.div>
          )}
        </div>
      )}

      {/* Interpretation */}
      <div className={cn(
        'p-3 rounded-xl text-xs',
        color.bg,
        'border',
        color.border
      )}>
        <div className="flex items-start gap-2">
          <Info className={cn('w-4 h-4 flex-shrink-0 mt-0.5', color.text)} />
          <span className={color.text}>{interpretation}</span>
        </div>
      </div>

      {/* Recommendations for high risk */}
      {likelihood === 'high' && (
        <div className="mt-3 p-3 rounded-xl bg-amber-500/5 border border-amber-500/10">
          <p className="text-xs text-amber-700 dark:text-amber-300">
            <strong>Recommendations:</strong> Consider counter-screening with 0.01% Triton X-100 or
            perform dynamic light scattering (DLS) to confirm non-aggregation behavior.
          </p>
        </div>
      )}
    </motion.div>
  );
}
