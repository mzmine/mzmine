import { useState } from 'react';
import { motion } from 'framer-motion';
import { Cpu, Fingerprint, Scale, ChevronRight, Sparkles, AlertCircle, Layers, Database } from 'lucide-react';
import type { MLReadinessResult } from '../../types/scoring';
import { ScoreChart } from './ScoreChart';
import { InfoTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';

interface MLReadinessScoreProps {
  result: MLReadinessResult;
  /** When true, hides the header/score chart and only shows breakdown */
  breakdownOnly?: boolean;
}

/**
 * Get color config based on percentage
 */
function getScoreColor(percentage: number) {
  if (percentage >= 80) return {
    gradient: 'from-yellow-500 to-amber-400',
    bg: 'bg-yellow-500/10',
    text: 'text-amber-500 dark:text-yellow-400',
    border: 'border-yellow-500/20',
    glow: 'shadow-yellow-500/20',
  };
  if (percentage >= 50) return {
    gradient: 'from-orange-500 to-orange-400',
    bg: 'bg-orange-500/10',
    text: 'text-orange-500',
    border: 'border-orange-500/20',
    glow: 'shadow-orange-500/20',
  };
  return {
    gradient: 'from-red-500 to-red-400',
    bg: 'bg-red-500/10',
    text: 'text-red-500',
    border: 'border-red-500/20',
    glow: 'shadow-red-500/20',
  };
}

/** Fingerprint type descriptions */
const FINGERPRINT_INFO: Record<string, { name: string; description: string }> = {
  morgan: { name: 'Morgan (ECFP)', description: 'Circular fingerprint based on atom connectivity' },
  morgan_features: { name: 'Morgan Features (FCFP)', description: 'Circular fingerprint with pharmacophore features' },
  maccs: { name: 'MACCS', description: '166 predefined structural keys' },
  atompair: { name: 'Atom Pair', description: 'Encodes pairs of atoms and topological distances' },
  topological_torsion: { name: 'Topological Torsion', description: 'Encodes torsion angle patterns' },
  rdkit_fp: { name: 'RDKit', description: 'Daylight-like path enumeration fingerprint' },
  avalon: { name: 'Avalon', description: 'Fast substructure fingerprint' },
};

interface BreakdownCardProps {
  icon: React.ReactNode;
  label: string;
  score: number;
  maxScore: number;
  detail: string;
  subDetail?: string;
  delay?: number;
  tooltip?: React.ReactNode;
}

function BreakdownCard({ icon, label, score, maxScore, detail, subDetail, delay = 0, tooltip }: BreakdownCardProps) {
  const percentage = maxScore > 0 ? (score / maxScore) * 100 : 0;
  const color = getScoreColor(percentage);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
      className={cn(
        'relative rounded-2xl p-4',
        'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface)]',
        'border border-[var(--color-border)]',
        'hover:border-[var(--color-primary)]/30 hover:shadow-lg hover:shadow-[var(--color-primary)]/5',
        'transition-all duration-300'
      )}
    >
      {/* Background glow effect - clipped to prevent overflow */}
      <div className="absolute inset-0 overflow-hidden rounded-2xl pointer-events-none">
        <div
          className={cn(
            'absolute -top-12 -right-12 w-32 h-32 rounded-full blur-3xl opacity-20',
            color.bg
          )}
        />
      </div>

      <div className="relative">
        {/* Header */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-3">
            <div className={cn(
              'w-10 h-10 rounded-xl flex items-center justify-center',
              color.bg, color.text
            )}>
              {icon}
            </div>
            <div>
              <div className="flex items-center gap-1">
                <h4 className="font-semibold text-[var(--color-text-primary)] text-sm">{label}</h4>
                {tooltip}
              </div>
              <p className="text-xs text-[var(--color-text-muted)]">{detail}</p>
            </div>
          </div>
          <div className="text-right">
            <div className={cn('text-2xl font-bold', color.text)}>
              {score.toFixed(0)}
            </div>
            <div className="text-xs text-[var(--color-text-muted)]">/ {maxScore}</div>
          </div>
        </div>

        {/* Progress bar */}
        <div className="h-2 bg-[var(--color-surface-sunken)] rounded-full overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${percentage}%` }}
            transition={{ duration: 0.8, delay: delay + 0.2, ease: 'easeOut' }}
            className={cn('h-full rounded-full bg-gradient-to-r', color.gradient)}
          />
        </div>

        {/* Sub detail */}
        {subDetail && (
          <p className="mt-2 text-xs text-[var(--color-text-muted)]">{subDetail}</p>
        )}
      </div>
    </motion.div>
  );
}

/**
 * Fingerprint badges component
 */
function FingerprintBadges({ successful, failed }: { successful: string[]; failed: string[] }) {
  return (
    <div className="flex flex-wrap gap-1 mt-2">
      {successful.map((fp) => {
        const info = FINGERPRINT_INFO[fp] || { name: fp, description: '' };
        return (
          <span
            key={fp}
            title={info.description}
            className={cn(
              'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium',
              'bg-yellow-500/10 text-amber-600 dark:text-yellow-400',
              'border border-yellow-500/20'
            )}
          >
            {info.name}
          </span>
        );
      })}
      {failed.map((fp) => {
        const info = FINGERPRINT_INFO[fp] || { name: fp, description: '' };
        return (
          <span
            key={fp}
            title={`Failed: ${info.description}`}
            className={cn(
              'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium',
              'bg-red-500/10 text-red-600 dark:text-red-400',
              'border border-red-500/20 line-through'
            )}
          >
            {info.name}
          </span>
        );
      })}
    </div>
  );
}

/**
 * Displays ML-readiness score with radial chart, breakdown bars, and informative tooltips.
 */
export function MLReadinessScore({ result, breakdownOnly = false }: MLReadinessScoreProps) {
  const [showFailedDescriptors, setShowFailedDescriptors] = useState(false);
  const [showFingerprintDetails, setShowFingerprintDetails] = useState(false);
  const { score, breakdown, interpretation, failed_descriptors } = result;

  // Calculate total descriptors
  const totalDescriptors = breakdown.descriptors_total +
    (breakdown.autocorr2d_total || 0) +
    (breakdown.mqn_total || 0);
  const successfulDescriptors = breakdown.descriptors_successful +
    (breakdown.autocorr2d_successful || 0) +
    (breakdown.mqn_successful || 0);

  // Combined descriptor score
  const combinedDescriptorScore = breakdown.descriptors_score +
    (breakdown.additional_descriptors_score || 0);
  const combinedDescriptorMax = breakdown.descriptors_max +
    (breakdown.additional_descriptors_max || 0);

  // Calculation explanation
  const calculation = `Score = Descriptors (${combinedDescriptorMax}pts) + Fingerprints (${breakdown.fingerprints_max}pts) + Size (${breakdown.size_max}pts)
= ${combinedDescriptorScore.toFixed(0)} + ${breakdown.fingerprints_score.toFixed(0)} + ${breakdown.size_score.toFixed(0)} = ${score}`;

  if (breakdownOnly) {
    return (
      <div className="space-y-4">
        {/* Interpretation banner */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className={cn(
            'relative overflow-hidden rounded-2xl p-4',
            'bg-gradient-to-r from-[var(--color-primary)]/5 via-[var(--color-accent)]/5 to-[var(--color-primary)]/5',
            'border border-[var(--color-primary)]/10'
          )}
        >
          <div className="flex items-start gap-3">
            <div className="w-8 h-8 rounded-lg bg-[var(--color-primary)]/10 flex items-center justify-center flex-shrink-0">
              <Sparkles className="w-4 h-4 text-[var(--color-primary)]" />
            </div>
            <div>
              <div className="flex items-center gap-1.5 mb-1">
                <h4 className="text-sm font-semibold text-[var(--color-text-primary)]">ML Readiness Analysis</h4>
                <InfoTooltip
                  title="What is ML Readiness?"
                  content={
                    <div className="text-xs space-y-2">
                      <p>ML Readiness measures how suitable a molecule is for machine learning applications in cheminformatics.</p>
                      <p className="text-white/80">A high ML Readiness score indicates:</p>
                      <ul className="list-disc list-inside space-y-1 text-white/70">
                        <li><strong>Descriptor Coverage:</strong> Most molecular descriptors (physical, topological, electronic properties) can be calculated without errors</li>
                        <li><strong>Fingerprint Generation:</strong> Multiple fingerprint types (Morgan, MACCS, etc.) can be successfully generated for similarity searches and model training</li>
                        <li><strong>Appropriate Size:</strong> The molecule is within typical size ranges for drug-like or lead-like compounds</li>
                      </ul>
                      <p className="text-white/60 mt-2">Molecules with low ML Readiness may cause issues in QSAR/QSPR models, virtual screening, or property prediction pipelines.</p>
                    </div>
                  }
                />
              </div>
              <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed">{interpretation}</p>
            </div>
          </div>
        </motion.div>

        {/* Breakdown cards grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {/* Standard Descriptors Card */}
          <BreakdownCard
            icon={<Cpu className="w-5 h-5" />}
            label="Standard Descriptors"
            score={breakdown.descriptors_score}
            maxScore={breakdown.descriptors_max}
            detail={`${breakdown.descriptors_successful}/${breakdown.descriptors_total} calculated`}
            subDetail="Physical, topological, and functional group descriptors"
            delay={0.1}
            tooltip={
              <InfoTooltip
                title="Standard Descriptors (217)"
                content={
                  <div className="text-xs space-y-1">
                    <p>RDKit's CalcMolDescriptors including:</p>
                    <ul className="list-disc list-inside text-white/70">
                      <li>Physical properties (MW, LogP, TPSA)</li>
                      <li>Topological indices (Chi, Kappa)</li>
                      <li>85 functional group counts</li>
                    </ul>
                  </div>
                }
              />
            }
          />

          {/* Additional Descriptors Card */}
          {(breakdown.additional_descriptors_max || 0) > 0 && (
            <BreakdownCard
              icon={<Layers className="w-5 h-5" />}
              label="Additional Descriptors"
              score={breakdown.additional_descriptors_score || 0}
              maxScore={breakdown.additional_descriptors_max || 5}
              detail={`${(breakdown.autocorr2d_successful || 0) + (breakdown.mqn_successful || 0)}/${(breakdown.autocorr2d_total || 192) + (breakdown.mqn_total || 42)} calculated`}
              subDetail={`AUTOCORR2D: ${breakdown.autocorr2d_successful || 0}/${breakdown.autocorr2d_total || 192} | MQN: ${breakdown.mqn_successful || 0}/${breakdown.mqn_total || 42}`}
              delay={0.15}
              tooltip={
                <InfoTooltip
                  title="Additional 2D Descriptors (234)"
                  content={
                    <div className="text-xs space-y-1">
                      <p><strong>AUTOCORR2D (192):</strong> 2D autocorrelation descriptors</p>
                      <p><strong>MQN (42):</strong> Molecular Quantum Numbers - atom and bond type counts</p>
                    </div>
                  }
                />
              }
            />
          )}

          {/* Fingerprints Card */}
          <BreakdownCard
            icon={<Fingerprint className="w-5 h-5" />}
            label="Fingerprints"
            score={breakdown.fingerprints_score}
            maxScore={breakdown.fingerprints_max}
            detail={`${breakdown.fingerprints_successful.length}/7 types generated`}
            delay={0.2}
            tooltip={
              <InfoTooltip
                title="Molecular Fingerprints (7 types)"
                content={
                  <div className="text-xs space-y-1">
                    <ul className="list-disc list-inside text-white/70">
                      <li><strong>Morgan (ECFP):</strong> Circular, atom connectivity</li>
                      <li><strong>Morgan Features (FCFP):</strong> Pharmacophore-aware</li>
                      <li><strong>MACCS:</strong> 166 structural keys</li>
                      <li><strong>Atom Pair:</strong> Atom pairs + distances</li>
                      <li><strong>Topological Torsion:</strong> Torsion patterns</li>
                      <li><strong>RDKit:</strong> Daylight-like paths</li>
                      <li><strong>Avalon:</strong> Fast substructure FP</li>
                    </ul>
                  </div>
                }
              />
            }
          />

          {/* Size Card */}
          <BreakdownCard
            icon={<Scale className="w-5 h-5" />}
            label="Size"
            score={breakdown.size_score}
            maxScore={breakdown.size_max}
            detail={breakdown.size_category}
            subDetail={breakdown.molecular_weight !== null ? `MW: ${breakdown.molecular_weight.toFixed(1)} Da | Atoms: ${breakdown.num_atoms}` : undefined}
            delay={0.25}
          />
        </div>

        {/* Fingerprint badges (expandable) */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
        >
          <button
            onClick={() => setShowFingerprintDetails(!showFingerprintDetails)}
            className={cn(
              'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl',
              'bg-[var(--color-surface-elevated)] border border-[var(--color-border)]',
              'text-sm text-[var(--color-text-secondary)]',
              'hover:bg-[var(--color-surface-sunken)] transition-colors'
            )}
          >
            <div className="flex items-center gap-2">
              <Database className="w-4 h-4" />
              <span>View fingerprint types ({breakdown.fingerprints_successful.length} successful)</span>
            </div>
            <ChevronRight className={cn(
              'w-4 h-4 transition-transform',
              showFingerprintDetails && 'rotate-90'
            )} />
          </button>
          {showFingerprintDetails && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="mt-2 p-3 bg-[var(--color-surface-sunken)] rounded-xl"
            >
              <FingerprintBadges
                successful={breakdown.fingerprints_successful}
                failed={breakdown.fingerprints_failed}
              />
            </motion.div>
          )}
        </motion.div>

        {/* Failed descriptors (collapsible) */}
        {failed_descriptors.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
          >
            <button
              onClick={() => setShowFailedDescriptors(!showFailedDescriptors)}
              className={cn(
                'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl',
                'bg-amber-500/5 border border-amber-500/10',
                'text-sm text-amber-600 dark:text-amber-400',
                'hover:bg-amber-500/10 transition-colors'
              )}
            >
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4" />
                <span>{failed_descriptors.length} descriptors could not be calculated</span>
              </div>
              <ChevronRight className={cn(
                'w-4 h-4 transition-transform',
                showFailedDescriptors && 'rotate-90'
              )} />
            </button>
            {showFailedDescriptors && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                className="mt-2 p-3 bg-[var(--color-surface-sunken)] rounded-xl text-xs text-[var(--color-text-muted)] font-mono max-h-32 overflow-y-auto"
              >
                {failed_descriptors.join(', ')}
              </motion.div>
            )}
          </motion.div>
        )}

        {/* Total descriptors summary */}
        <div className="text-center text-xs text-[var(--color-text-muted)] pt-2">
          Total: {successfulDescriptors}/{totalDescriptors} descriptors | 7 fingerprint types
        </div>
      </div>
    );
  }

  // Full view with header
  return (
    <div className="card-chem p-6">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="section-header-icon">
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
            </svg>
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <h3 className="text-lg font-semibold text-chem-dark">ML-Readiness Score</h3>
              <InfoTooltip
                title="What is ML Readiness?"
                content={
                  <div className="text-xs space-y-2">
                    <p>ML Readiness measures how suitable a molecule is for machine learning applications in cheminformatics.</p>
                    <p className="text-white/80">A high ML Readiness score indicates:</p>
                    <ul className="list-disc list-inside space-y-1 text-white/70">
                      <li><strong>Descriptor Coverage:</strong> Most molecular descriptors can be calculated without errors</li>
                      <li><strong>Fingerprint Generation:</strong> Multiple fingerprint types can be successfully generated</li>
                      <li><strong>Appropriate Size:</strong> The molecule is within typical size ranges</li>
                    </ul>
                    <p className="text-white/60 mt-2">Low scores may cause issues in QSAR models, virtual screening, or property prediction.</p>
                  </div>
                }
              />
            </div>
            <p className="text-sm text-chem-dark/50">Suitability for machine learning models</p>
          </div>
        </div>

        {/* Main Score */}
        <ScoreChart
          score={score}
          label="ML-Readiness"
          size={120}
          calculation={calculation}
          interpretation={interpretation}
          compact
        />
      </div>

      {/* Interpretation */}
      <div className="bg-chem-primary/5 rounded-xl p-4 mb-6">
        <p className="text-sm text-chem-dark/80">{interpretation}</p>
      </div>

      {/* Score Breakdown */}
      <div className="space-y-4">
        <h4 className="text-sm font-semibold text-chem-dark/70 uppercase tracking-wide flex items-center gap-2">
          Score Breakdown
          <InfoTooltip
            title="How ML-Readiness is Calculated"
            content={
              <div className="space-y-2 text-xs">
                <p>The ML-Readiness score measures how suitable a molecule is for machine learning models.</p>
                <ul className="list-disc list-inside space-y-1 text-white/70">
                  <li>Standard Descriptors (35pts): 217 RDKit descriptors</li>
                  <li>Additional Descriptors (5pts): AUTOCORR2D (192) + MQN (42)</li>
                  <li>Fingerprints (40pts): 7 fingerprint types</li>
                  <li>Size (20pts): Molecular weight and atom count</li>
                </ul>
                <p className="text-white/50 mt-2">Total: 451 descriptors + 7 fingerprint types</p>
              </div>
            }
          />
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <BreakdownCard
            icon={<Cpu className="w-5 h-5" />}
            label="Standard Descriptors"
            score={breakdown.descriptors_score}
            maxScore={breakdown.descriptors_max}
            detail={`${breakdown.descriptors_successful}/${breakdown.descriptors_total}`}
            delay={0}
          />
          {(breakdown.additional_descriptors_max || 0) > 0 && (
            <BreakdownCard
              icon={<Layers className="w-5 h-5" />}
              label="Additional Descriptors"
              score={breakdown.additional_descriptors_score || 0}
              maxScore={breakdown.additional_descriptors_max || 5}
              detail={`AUTOCORR2D + MQN`}
              delay={0.05}
            />
          )}
          <BreakdownCard
            icon={<Fingerprint className="w-5 h-5" />}
            label="Fingerprints"
            score={breakdown.fingerprints_score}
            maxScore={breakdown.fingerprints_max}
            detail={`${breakdown.fingerprints_successful.length}/7 types`}
            delay={0.1}
          />
          <BreakdownCard
            icon={<Scale className="w-5 h-5" />}
            label="Size"
            score={breakdown.size_score}
            maxScore={breakdown.size_max}
            detail={breakdown.size_category}
            delay={0.15}
          />
        </div>

        {/* Fingerprint badges */}
        <div className="mt-4">
          <h5 className="text-xs font-medium text-chem-dark/60 mb-2">Fingerprint Types</h5>
          <FingerprintBadges
            successful={breakdown.fingerprints_successful}
            failed={breakdown.fingerprints_failed}
          />
        </div>
      </div>

      {/* Failed Descriptors */}
      {failed_descriptors.length > 0 && (
        <div className="mt-6 pt-4 border-t border-chem-dark/10">
          <button
            onClick={() => setShowFailedDescriptors(!showFailedDescriptors)}
            className="flex items-center gap-2 text-sm text-chem-dark/60 hover:text-chem-dark transition-colors"
          >
            <ChevronRight className={cn('w-4 h-4 transition-transform', showFailedDescriptors && 'rotate-90')} />
            <span>{failed_descriptors.length} descriptors could not be calculated</span>
          </button>
          {showFailedDescriptors && (
            <div className="mt-3 p-4 bg-chem-dark/5 rounded-lg text-xs text-chem-dark/60 font-mono max-h-32 overflow-y-auto">
              {failed_descriptors.join(', ')}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
