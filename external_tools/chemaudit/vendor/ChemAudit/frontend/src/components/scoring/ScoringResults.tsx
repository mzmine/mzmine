import { motion } from 'framer-motion';
import { TrendingUp, Clock } from 'lucide-react';
import { MLReadinessScore } from './MLReadinessScore';
import { NPLikenessScore } from './NPLikenessScore';
import { ScaffoldDisplay } from './ScaffoldDisplay';
import { DrugLikenessScore } from './DrugLikenessScore';
import { SafetyFiltersScore } from './SafetyFiltersScore';
import { ADMETScore } from './ADMETScore';
import { AggregatorScore } from './AggregatorScore';
import type { ScoringResponse } from '../../types/scoring';
import { cn } from '../../lib/utils';

interface ScoringResultsProps {
  scoringResponse: ScoringResponse;
}

interface AnimatedSectionProps {
  children: React.ReactNode;
  delay: number;
}

function AnimatedSection({ children, delay }: AnimatedSectionProps): JSX.Element {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
    >
      {children}
    </motion.div>
  );
}

/**
 * Combined display for all scoring results.
 */
export function ScoringResults({ scoringResponse }: ScoringResultsProps) {
  const {
    ml_readiness,
    np_likeness,
    scaffold,
    druglikeness,
    safety_filters,
    admet,
    aggregator,
    execution_time_ms
  } = scoringResponse;

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center justify-between"
      >
        <div className="flex items-center gap-3">
          <div className={cn(
            'w-10 h-10 rounded-xl flex items-center justify-center',
            'bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-accent)]/20',
            'text-[var(--color-primary)]'
          )}>
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
              Scoring Results
            </h2>
            <p className="text-xs text-[var(--color-text-muted)]">
              Comprehensive molecular analysis
            </p>
          </div>
        </div>
        <div className="flex items-center gap-1.5 text-xs text-[var(--color-text-muted)] bg-[var(--color-surface-sunken)] px-3 py-1.5 rounded-full">
          <Clock className="w-3.5 h-3.5" />
          <span>{execution_time_ms}ms</span>
        </div>
      </motion.div>

      {/* ML-Readiness breakdown */}
      {ml_readiness && (
        <MLReadinessScore result={ml_readiness} breakdownOnly />
      )}

      {/* Drug-likeness and Safety Filters grid */}
      {(druglikeness || safety_filters) && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {druglikeness && (
            <AnimatedSection delay={0.2}>
              <DrugLikenessScore result={druglikeness} />
            </AnimatedSection>
          )}
          {safety_filters && (
            <AnimatedSection delay={0.25}>
              <SafetyFiltersScore result={safety_filters} />
            </AnimatedSection>
          )}
        </div>
      )}

      {/* ADMET Predictions */}
      {admet && (
        <AnimatedSection delay={0.3}>
          <ADMETScore result={admet} />
        </AnimatedSection>
      )}

      {/* Aggregator Likelihood */}
      {aggregator && (
        <AnimatedSection delay={0.32}>
          <AggregatorScore result={aggregator} />
        </AnimatedSection>
      )}

      {/* NP-Likeness and Scaffold grid */}
      {(np_likeness || scaffold) && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {np_likeness && (
            <AnimatedSection delay={0.35}>
              <NPLikenessScore result={np_likeness} />
            </AnimatedSection>
          )}
          {scaffold && (
            <AnimatedSection delay={0.4}>
              <ScaffoldDisplay result={scaffold} />
            </AnimatedSection>
          )}
        </div>
      )}
    </div>
  );
}
