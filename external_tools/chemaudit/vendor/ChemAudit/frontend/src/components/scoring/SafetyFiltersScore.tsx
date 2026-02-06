import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Shield,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  ChevronDown,
  Info
} from 'lucide-react';
import type { SafetyFilterResult, FilterAlertResult, ChEMBLAlertsResult } from '../../types/scoring';
import { InfoTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';

interface SafetyFiltersScoreProps {
  result: SafetyFilterResult;
}

/**
 * Individual filter result display
 */
function FilterCard({
  name,
  description,
  result,
  delay = 0
}: {
  name: string;
  description: string;
  result: FilterAlertResult;
  delay?: number;
}) {
  const [showAlerts, setShowAlerts] = useState(false);

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className={cn(
        'rounded-xl p-3',
        'bg-[var(--color-surface-sunken)]',
        'border',
        result.passed
          ? 'border-emerald-500/20'
          : 'border-red-500/20'
      )}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          {result.passed ? (
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
          ) : (
            <ShieldAlert className="w-4 h-4 text-red-500" />
          )}
          <span className="text-sm font-medium text-[var(--color-text-primary)]">{name}</span>
          <InfoTooltip title={name} content={<span className="text-xs">{description}</span>} />
        </div>
        <span
          className={cn(
            'text-xs font-medium px-2 py-0.5 rounded-full',
            result.passed
              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
              : 'bg-red-500/10 text-red-600 dark:text-red-400'
          )}
        >
          {result.passed ? 'Clear' : `${result.alert_count} alert${result.alert_count !== 1 ? 's' : ''}`}
        </span>
      </div>

      {/* Show alerts if any */}
      {!result.passed && result.alerts.length > 0 && (
        <>
          <button
            onClick={() => setShowAlerts(!showAlerts)}
            className={cn(
              'w-full flex items-center justify-between px-2 py-1 mt-1 rounded-lg',
              'text-xs text-red-600 dark:text-red-400',
              'hover:bg-red-500/5 transition-colors'
            )}
          >
            <span>View alerts</span>
            <ChevronDown className={cn(
              'w-3 h-3 transition-transform',
              showAlerts && 'rotate-180'
            )} />
          </button>
          {showAlerts && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="mt-2 p-2 rounded-lg bg-red-500/5 text-xs text-red-600 dark:text-red-400 max-h-24 overflow-y-auto"
            >
              {result.alerts.map((alert, i) => (
                <div key={i} className="flex items-start gap-1.5 py-0.5">
                  <AlertTriangle className="w-3 h-3 flex-shrink-0 mt-0.5" />
                  <span>{alert}</span>
                </div>
              ))}
            </motion.div>
          )}
        </>
      )}
    </motion.div>
  );
}

/**
 * ChEMBL alerts section
 */
function ChEMBLAlertsSection({ chembl }: { chembl: ChEMBLAlertsResult }) {
  const [expanded, setExpanded] = useState(false);

  const alertFilters = [
    { key: 'bms', name: 'BMS', desc: 'Bristol-Myers Squibb reactive groups' },
    { key: 'dundee', name: 'Dundee', desc: 'University of Dundee alerts' },
    { key: 'glaxo', name: 'Glaxo', desc: 'GSK structural alerts' },
    { key: 'inpharmatica', name: 'Inpharmatica', desc: 'Inpharmatica alerts' },
    { key: 'lint', name: 'LINT', desc: 'Lilly internal alerts' },
    { key: 'mlsmr', name: 'MLSMR', desc: 'Molecular Libraries alerts' },
    { key: 'schembl', name: 'SureChEMBL', desc: 'SureChEMBL reactivity' },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.3 }}
      className={cn(
        'rounded-xl p-3',
        'bg-[var(--color-surface-sunken)]',
        'border',
        chembl.passed ? 'border-emerald-500/20' : 'border-amber-500/20'
      )}
    >
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between"
      >
        <div className="flex items-center gap-2">
          {chembl.passed ? (
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
          ) : (
            <ShieldAlert className="w-4 h-4 text-amber-500" />
          )}
          <span className="text-sm font-medium text-[var(--color-text-primary)]">ChEMBL Alerts</span>
          <InfoTooltip
            asSpan
            title="ChEMBL Structural Alerts"
            content={
              <span className="text-xs">
                Additional structural alerts from pharmaceutical companies (BMS, Dundee, Glaxo, etc.)
              </span>
            }
          />
        </div>
        <div className="flex items-center gap-2">
          <span
            className={cn(
              'text-xs font-medium px-2 py-0.5 rounded-full',
              chembl.passed
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
            )}
          >
            {chembl.passed ? 'Clear' : `${chembl.total_alerts} alert${chembl.total_alerts !== 1 ? 's' : ''}`}
          </span>
          <ChevronDown className={cn(
            'w-4 h-4 text-[var(--color-text-muted)] transition-transform',
            expanded && 'rotate-180'
          )} />
        </div>
      </button>

      {expanded && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          className="mt-3 space-y-1"
        >
          {alertFilters.map(filter => {
            const filterResult = chembl[filter.key as keyof ChEMBLAlertsResult] as FilterAlertResult | null;
            if (!filterResult) return null;

            return (
              <div key={filter.key} className="flex items-center justify-between text-xs py-1">
                <div className="flex items-center gap-2">
                  {filterResult.passed ? (
                    <ShieldCheck className="w-3 h-3 text-emerald-500" />
                  ) : (
                    <ShieldAlert className="w-3 h-3 text-red-500" />
                  )}
                  <span className="text-[var(--color-text-secondary)]">{filter.name}</span>
                </div>
                <span className={cn(
                  'text-xs',
                  filterResult.passed ? 'text-emerald-500' : 'text-red-500'
                )}>
                  {filterResult.passed ? 'Pass' : `${filterResult.alert_count} alerts`}
                </span>
              </div>
            );
          })}
        </motion.div>
      )}
    </motion.div>
  );
}

/**
 * Displays safety filter results including PAINS, Brenk, NIH, ZINC, and ChEMBL.
 */
export function SafetyFiltersScore({ result }: SafetyFiltersScoreProps) {
  const { pains, brenk, nih, zinc, chembl, all_passed, total_alerts, interpretation } = result;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'rounded-2xl p-5',
        'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface)]',
        'border',
        all_passed
          ? 'border-emerald-500/20'
          : 'border-red-500/20'
      )}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={cn(
            'w-10 h-10 rounded-xl flex items-center justify-center',
            all_passed
              ? 'bg-emerald-500/10 text-emerald-500'
              : 'bg-red-500/10 text-red-500'
          )}>
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-semibold text-[var(--color-text-primary)]">Safety Filters</h3>
            <p className="text-xs text-[var(--color-text-muted)]">Structural alert screening</p>
          </div>
        </div>

        {/* Overall status badge */}
        <div className={cn(
          'flex items-center gap-2 px-3 py-1.5 rounded-full',
          all_passed
            ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
            : 'bg-red-500/10 text-red-600 dark:text-red-400'
        )}>
          {all_passed ? (
            <>
              <ShieldCheck className="w-4 h-4" />
              <span className="text-sm font-medium">All Clear</span>
            </>
          ) : (
            <>
              <AlertTriangle className="w-4 h-4" />
              <span className="text-sm font-medium">{total_alerts} Alert{total_alerts !== 1 ? 's' : ''}</span>
            </>
          )}
        </div>
      </div>

      {/* Filter cards */}
      <div className="space-y-2">
        <FilterCard
          name="PAINS"
          description="Pan Assay Interference Compounds - 480 patterns that cause false positives in HTS"
          result={pains}
          delay={0.1}
        />
        <FilterCard
          name="Brenk"
          description="Structural alerts for toxicity and unfavorable pharmacokinetics"
          result={brenk}
          delay={0.15}
        />
        {nih && (
          <FilterCard
            name="NIH"
            description="NIH Molecular Libraries structural alerts for HTS interference"
            result={nih}
            delay={0.2}
          />
        )}
        {zinc && (
          <FilterCard
            name="ZINC"
            description="Drug-likeness and reactivity filters from ZINC database"
            result={zinc}
            delay={0.25}
          />
        )}
        {chembl && (
          <ChEMBLAlertsSection chembl={chembl} />
        )}
      </div>

      {/* Interpretation */}
      <div className={cn(
        'mt-4 p-3 rounded-xl text-xs',
        all_passed
          ? 'bg-emerald-500/5 border border-emerald-500/10 text-emerald-700 dark:text-emerald-300'
          : 'bg-red-500/5 border border-red-500/10 text-red-700 dark:text-red-300'
      )}>
        <div className="flex items-start gap-2">
          <Info className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{interpretation}</span>
        </div>
      </div>
    </motion.div>
  );
}
