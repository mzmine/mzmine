import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  FlaskConical,
  Droplets,
  Box,
  Brain,
  Activity,
  ChevronDown,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import type { ADMETResult } from '../../types/scoring';
import { InfoTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';

interface ADMETScoreProps {
  result: ADMETResult;
}

/**
 * Get color based on classification
 */
function getClassColor(classification: string): { bg: string; text: string; border: string } {
  switch (classification) {
    case 'easy':
    case 'highly_soluble':
    case 'soluble':
    case '3d':
      return {
        bg: 'bg-emerald-500/10',
        text: 'text-emerald-600 dark:text-emerald-400',
        border: 'border-emerald-500/20'
      };
    case 'moderate':
      return {
        bg: 'bg-amber-500/10',
        text: 'text-amber-600 dark:text-amber-400',
        border: 'border-amber-500/20'
      };
    case 'difficult':
    case 'poor':
    case 'insoluble':
    case 'flat':
      return {
        bg: 'bg-red-500/10',
        text: 'text-red-600 dark:text-red-400',
        border: 'border-red-500/20'
      };
    default:
      return {
        bg: 'bg-slate-500/10',
        text: 'text-slate-600 dark:text-slate-400',
        border: 'border-slate-500/20'
      };
  }
}

/**
 * Metric card component
 */
function MetricCard({
  icon,
  title,
  value,
  unit,
  classification,
  description,
  tooltip,
  delay = 0
}: {
  icon: React.ReactNode;
  title: string;
  value: number | string;
  unit?: string;
  classification: string;
  description: string;
  tooltip: React.ReactNode;
  delay?: number;
}) {
  const color = getClassColor(classification);

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className={cn(
        'rounded-xl p-3',
        'bg-[var(--color-surface-sunken)]',
        'border',
        color.border
      )}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <div className={cn('w-8 h-8 rounded-lg flex items-center justify-center', color.bg, color.text)}>
            {icon}
          </div>
          <div>
            <div className="flex items-center gap-1">
              <span className="text-sm font-medium text-[var(--color-text-primary)]">{title}</span>
              <InfoTooltip title={title} content={tooltip} />
            </div>
          </div>
        </div>
        <div className="text-right">
          <div className={cn('text-xl font-bold', color.text)}>
            {typeof value === 'number' ? value.toFixed(2) : value}
            {unit && <span className="text-xs font-normal ml-1">{unit}</span>}
          </div>
          <span className={cn('text-xs px-2 py-0.5 rounded-full', color.bg, color.text)}>
            {classification}
          </span>
        </div>
      </div>
      <p className="text-xs text-[var(--color-text-muted)]">{description}</p>
    </motion.div>
  );
}

/**
 * Rule card component for Pfizer/GSK rules
 */
function RuleCard({
  name,
  passed,
  values,
  description,
  tooltip,
  delay = 0
}: {
  name: string;
  passed: boolean;
  values: string;
  description: string;
  tooltip: React.ReactNode;
  delay?: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className={cn(
        'rounded-lg p-2',
        'bg-[var(--color-surface-sunken)]',
        'border',
        passed ? 'border-emerald-500/20' : 'border-amber-500/20'
      )}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {passed ? (
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
          ) : (
            <XCircle className="w-3.5 h-3.5 text-amber-500" />
          )}
          <span className="text-xs font-medium text-[var(--color-text-primary)]">{name}</span>
          <InfoTooltip title={name} content={tooltip} />
        </div>
        <span className={cn(
          'text-xs font-medium',
          passed ? 'text-emerald-500' : 'text-amber-500'
        )}>
          {passed ? 'Pass' : 'Fail'}
        </span>
      </div>
      <p className="text-xs text-[var(--color-text-muted)] mt-1">{values}</p>
      {description && (
        <p className="text-xs text-[var(--color-text-muted)] mt-1 italic">{description}</p>
      )}
    </motion.div>
  );
}

/**
 * Displays ADMET predictions including SA Score, Solubility, Complexity, CNS MPO, and safety rules.
 */
export function ADMETScore({ result }: ADMETScoreProps) {
  const [showBioavailability, setShowBioavailability] = useState(false);
  const [showRules, setShowRules] = useState(false);
  const {
    synthetic_accessibility,
    solubility,
    complexity,
    cns_mpo,
    bioavailability,
    pfizer_rule,
    gsk_rule,
    golden_triangle,
    molar_refractivity,
    interpretation
  } = result;

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
          'bg-cyan-500/10 text-cyan-500'
        )}>
          <Activity className="w-5 h-5" />
        </div>
        <div>
          <h3 className="font-semibold text-[var(--color-text-primary)]">ADMET Predictions</h3>
          <p className="text-xs text-[var(--color-text-muted)]">Absorption, distribution, metabolism, excretion, toxicity</p>
        </div>
      </div>

      {/* Metrics grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Synthetic Accessibility */}
        <MetricCard
          icon={<FlaskConical className="w-4 h-4" />}
          title="Synthetic Accessibility"
          value={synthetic_accessibility.score}
          classification={synthetic_accessibility.classification}
          description={synthetic_accessibility.interpretation}
          delay={0.1}
          tooltip={
            <div className="text-xs">
              <p>Estimates ease of chemical synthesis.</p>
              <ul className="mt-1 text-white/70">
                <li>1-4: Easy to synthesize</li>
                <li>4-6: Moderate difficulty</li>
                <li>6-10: Difficult synthesis</li>
              </ul>
            </div>
          }
        />

        {/* Solubility */}
        <MetricCard
          icon={<Droplets className="w-4 h-4" />}
          title="Solubility (ESOL)"
          value={solubility.log_s}
          unit="LogS"
          classification={solubility.classification}
          description={`${solubility.solubility_mg_ml.toFixed(4)} mg/mL`}
          delay={0.15}
          tooltip={
            <div className="text-xs">
              <p>Estimated aqueous solubility (Delaney ESOL).</p>
              <ul className="mt-1 text-white/70">
                <li>&gt;-1: Highly soluble</li>
                <li>-1 to -3: Soluble</li>
                <li>-3 to -4: Moderate</li>
                <li>&lt;-4: Poorly soluble</li>
              </ul>
            </div>
          }
        />

        {/* Complexity (Fsp3) */}
        <MetricCard
          icon={<Box className="w-4 h-4" />}
          title="3D Complexity (Fsp3)"
          value={complexity.fsp3}
          classification={complexity.classification}
          description={`${complexity.num_stereocenters} stereocenters, ${complexity.num_rings} rings`}
          delay={0.2}
          tooltip={
            <div className="text-xs">
              <p>Fraction of sp3 carbons - indicates 3D character.</p>
              <ul className="mt-1 text-white/70">
                <li>&gt;0.42: Good 3D (clinical success)</li>
                <li>0.25-0.42: Moderate</li>
                <li>&lt;0.25: Flat molecule</li>
              </ul>
            </div>
          }
        />

        {/* CNS MPO */}
        {cns_mpo && (
          <MetricCard
            icon={<Brain className="w-4 h-4" />}
            title="CNS MPO"
            value={cns_mpo.score}
            unit="/6"
            classification={cns_mpo.cns_penetrant ? '3d' : 'flat'}
            description={cns_mpo.interpretation}
            delay={0.25}
            tooltip={
              <div className="text-xs">
                <p>CNS Multiparameter Optimization score (Pfizer).</p>
                <ul className="mt-1 text-white/70">
                  <li>&ge;4: Good CNS penetration</li>
                  <li>3-4: Moderate</li>
                  <li>&lt;3: Poor CNS penetration</li>
                </ul>
              </div>
            }
          />
        )}
      </div>

      {/* Safety Rules Section */}
      {(pfizer_rule || gsk_rule || golden_triangle) && (
        <div className="mt-4">
          <button
            onClick={() => setShowRules(!showRules)}
            className={cn(
              'w-full flex items-center justify-between px-3 py-2 rounded-xl',
              'bg-[var(--color-surface-sunken)]',
              'text-sm text-[var(--color-text-secondary)]',
              'hover:bg-[var(--color-surface-elevated)] transition-colors'
            )}
          >
            <div className="flex items-center gap-3">
              <span>Safety Rules</span>
              <div className="flex items-center gap-2">
                {pfizer_rule && (
                  <span className={cn(
                    'inline-flex items-center gap-1 text-xs',
                    pfizer_rule.passed ? 'text-emerald-500' : 'text-amber-500'
                  )}>
                    {pfizer_rule.passed ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                    Pfizer
                  </span>
                )}
                {gsk_rule && (
                  <span className={cn(
                    'inline-flex items-center gap-1 text-xs',
                    gsk_rule.passed ? 'text-emerald-500' : 'text-amber-500'
                  )}>
                    {gsk_rule.passed ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                    GSK
                  </span>
                )}
                {golden_triangle && (
                  <span className={cn(
                    'inline-flex items-center gap-1 text-xs',
                    golden_triangle.in_golden_triangle ? 'text-emerald-500' : 'text-amber-500'
                  )}>
                    {golden_triangle.in_golden_triangle ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                    Triangle
                  </span>
                )}
              </div>
            </div>
            <ChevronDown className={cn(
              'w-4 h-4 transition-transform',
              showRules && 'rotate-180'
            )} />
          </button>

          {showRules && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="mt-2 space-y-2"
            >
              {pfizer_rule && (
                <RuleCard
                  name="Pfizer 3/75 Rule"
                  passed={pfizer_rule.passed}
                  values={`LogP: ${pfizer_rule.logp.toFixed(1)}, TPSA: ${pfizer_rule.tpsa.toFixed(0)}`}
                  description={pfizer_rule.interpretation}
                  delay={0}
                  tooltip={
                    <div className="text-xs">
                      <p>Compounds with LogP &gt; 3 AND TPSA &lt; 75 have higher toxicity risk.</p>
                      <p className="mt-1 text-white/70">Based on Pfizer's analysis of compound promiscuity.</p>
                    </div>
                  }
                />
              )}
              {gsk_rule && (
                <RuleCard
                  name="GSK 4/400 Rule"
                  passed={gsk_rule.passed}
                  values={`MW: ${gsk_rule.mw.toFixed(0)}, LogP: ${gsk_rule.logp.toFixed(1)}`}
                  description={gsk_rule.interpretation}
                  delay={0.05}
                  tooltip={
                    <div className="text-xs">
                      <p>Compounds with MW &le; 400 AND LogP &le; 4 have better outcomes.</p>
                      <p className="mt-1 text-white/70">Based on GSK's analysis of ADMET properties.</p>
                    </div>
                  }
                />
              )}
              {golden_triangle && (
                <RuleCard
                  name="Golden Triangle"
                  passed={golden_triangle.in_golden_triangle}
                  values={`MW: ${golden_triangle.mw.toFixed(0)}, LogD: ${golden_triangle.logd.toFixed(1)}`}
                  description={golden_triangle.interpretation}
                  delay={0.1}
                  tooltip={
                    <div className="text-xs">
                      <p>Abbott's Golden Triangle: MW 200-450, LogD -0.5 to 5.</p>
                      <p className="mt-1 text-white/70">Compounds in this range have favorable permeability and metabolic stability.</p>
                    </div>
                  }
                />
              )}
              {molar_refractivity !== null && molar_refractivity !== undefined && (
                <div className="text-xs text-[var(--color-text-muted)] px-2">
                  Molar Refractivity: {molar_refractivity.toFixed(1)}
                </div>
              )}
            </motion.div>
          )}
        </div>
      )}

      {/* Bioavailability Summary */}
      <div className="mt-4">
        <button
          onClick={() => setShowBioavailability(!showBioavailability)}
          className={cn(
            'w-full flex items-center justify-between px-3 py-2 rounded-xl',
            'bg-[var(--color-surface-sunken)]',
            'text-sm text-[var(--color-text-secondary)]',
            'hover:bg-[var(--color-surface-elevated)] transition-colors'
          )}
        >
          <div className="flex items-center gap-3">
            <span>Bioavailability Indicators</span>
            <div className="flex items-center gap-2">
              {bioavailability.oral_absorption_likely ? (
                <span className="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="w-3 h-3" /> Oral
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-xs text-red-600 dark:text-red-400">
                  <XCircle className="w-3 h-3" /> Oral
                </span>
              )}
              {bioavailability.cns_penetration_likely ? (
                <span className="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="w-3 h-3" /> CNS
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-xs text-red-600 dark:text-red-400">
                  <XCircle className="w-3 h-3" /> CNS
                </span>
              )}
            </div>
          </div>
          <ChevronDown className={cn(
            'w-4 h-4 transition-transform',
            showBioavailability && 'rotate-180'
          )} />
        </button>

        {showBioavailability && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-2 p-3 rounded-xl bg-[var(--color-surface-sunken)]"
          >
            <div className="grid grid-cols-3 gap-3 text-xs">
              <div>
                <span className="text-[var(--color-text-muted)]">MW</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.mw.toFixed(1)}</div>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)]">LogP</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.logp.toFixed(2)}</div>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)]">TPSA</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.tpsa.toFixed(1)}</div>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)]">RotBonds</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.rotatable_bonds}</div>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)]">HBD</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.hbd}</div>
              </div>
              <div>
                <span className="text-[var(--color-text-muted)]">HBA</span>
                <div className="font-medium text-[var(--color-text-primary)]">{bioavailability.hba}</div>
              </div>
            </div>
            <p className="mt-2 text-xs text-[var(--color-text-muted)]">
              {bioavailability.interpretation}
            </p>
          </motion.div>
        )}
      </div>

      {/* Overall Interpretation */}
      <div className={cn(
        'mt-4 p-3 rounded-xl text-xs',
        'bg-cyan-500/5 border border-cyan-500/10',
        'text-[var(--color-text-secondary)]'
      )}>
        {interpretation}
      </div>
    </motion.div>
  );
}
