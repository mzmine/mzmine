import { motion } from 'framer-motion';
import { Leaf, FlaskConical, Sparkles } from 'lucide-react';
import type { NPLikenessResult } from '../../types/scoring';
import { cn } from '../../lib/utils';
import { InfoTooltip } from '../ui/Tooltip';

interface NPLikenessScoreProps {
  result: NPLikenessResult;
}

/**
 * Get score category info based on NP-likeness score
 */
function getScoreCategory(score: number) {
  if (score >= 1.0) return {
    label: 'Natural Product-like',
    icon: Leaf,
    gradient: 'from-yellow-500 to-amber-400',
    bg: 'bg-yellow-500/10',
    text: 'text-amber-500 dark:text-yellow-400',
    border: 'border-yellow-500/20',
    glow: 'shadow-yellow-500/30',
    description: 'Structural features commonly found in natural products',
  };
  if (score >= -0.3) return {
    label: 'Mixed Character',
    icon: Sparkles,
    gradient: 'from-slate-500 to-slate-400',
    bg: 'bg-slate-500/10',
    text: 'text-slate-500',
    border: 'border-slate-500/20',
    glow: 'shadow-slate-500/30',
    description: 'Combines features from natural and synthetic compounds',
  };
  return {
    label: 'Synthetic-like',
    icon: FlaskConical,
    gradient: 'from-amber-500 to-orange-400',
    bg: 'bg-amber-500/10',
    text: 'text-amber-500',
    border: 'border-amber-500/20',
    glow: 'shadow-amber-500/30',
    description: 'Features more common in synthetic compounds',
  };
}

/**
 * Displays NP-likeness score with sleek visual scale and animations.
 */
export function NPLikenessScore({ result }: NPLikenessScoreProps) {
  const { score, interpretation, caveats } = result;

  // Scale is -5 to +5, position as percentage (0-100)
  const markerPosition = ((score + 5) / 10) * 100;
  const clampedPosition = Math.max(2, Math.min(98, markerPosition));

  const category = getScoreCategory(score);
  const IconComponent = category.icon;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className={cn(
        'relative overflow-hidden rounded-2xl p-5',
        'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface)]',
        'border border-[var(--color-border)]'
      )}
    >
      {/* Background glow effect */}
      <div
        className={cn(
          'absolute -top-20 -right-20 w-40 h-40 rounded-full blur-3xl opacity-20',
          category.bg
        )}
      />

      <div className="relative">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className={cn(
              'w-10 h-10 rounded-xl flex items-center justify-center',
              category.bg, category.text
            )}>
              <IconComponent className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h4 className="font-semibold text-[var(--color-text-primary)] text-sm">
                  NP-Likeness
                </h4>
                <InfoTooltip
                  title="Natural Product Likeness Score"
                  content={
                    <div className="text-xs space-y-2">
                      <p>Measures how similar a molecule's structure is to known natural products.</p>
                      <ul className="list-disc list-inside space-y-1 text-white/80">
                        <li><strong>Score ≥ 1.0:</strong> Natural product-like</li>
                        <li><strong>Score -0.3 to 1.0:</strong> Mixed character</li>
                        <li><strong>Score &lt; -0.3:</strong> Synthetic-like</li>
                      </ul>
                      <p className="text-white/60">Based on fragment analysis comparing to the COCONUT natural products database.</p>
                    </div>
                  }
                />
              </div>
              <p className="text-xs text-[var(--color-text-muted)]">
                Natural product similarity
              </p>
            </div>
          </div>

          {/* Score badge */}
          <div className={cn(
            'px-3 py-1.5 rounded-lg',
            category.bg, category.border, 'border'
          )}>
            <span className={cn('text-lg font-bold', category.text)}>
              {score >= 0 ? '+' : ''}{score.toFixed(2)}
            </span>
          </div>
        </div>

        {/* Visual Scale */}
        <div className="mb-4">
          <div className="relative h-10">
            {/* Scale track background */}
            <div className="absolute inset-x-0 top-1/2 -translate-y-1/2 h-3 rounded-full overflow-hidden bg-[var(--color-surface-sunken)]">
              {/* Gradient fill */}
              <div
                className="absolute inset-0 opacity-80"
                style={{
                  background: 'linear-gradient(to right, #ea580c 0%, #f59e0b 20%, #94a3b8 50%, #fbbf24 80%, #eab308 100%)'
                }}
              />
            </div>

            {/* Center line indicator */}
            <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-0.5 h-5 bg-[var(--color-text-muted)]/30 rounded-full" />

            {/* Animated marker */}
            <motion.div
              initial={{ left: '50%' }}
              animate={{ left: `${clampedPosition}%` }}
              transition={{ duration: 0.8, ease: 'easeOut', delay: 0.2 }}
              className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2"
            >
              {/* Glow ring */}
              <div className={cn(
                'absolute inset-0 w-7 h-7 -m-0.5 rounded-full blur-sm opacity-60',
                `bg-gradient-to-r ${category.gradient}`
              )} />
              {/* Marker dot */}
              <div className={cn(
                'relative w-6 h-6 rounded-full border-2 border-white shadow-lg',
                `bg-gradient-to-br ${category.gradient}`,
                `shadow-lg ${category.glow}`
              )}>
                {/* Inner shine */}
                <div className="absolute top-1 left-1 w-2 h-2 rounded-full bg-white/40" />
              </div>
            </motion.div>
          </div>

          {/* Scale labels */}
          <div className="flex justify-between text-[10px] mt-2 px-1">
            <span className="text-amber-500 font-medium flex items-center gap-1">
              <FlaskConical className="w-3 h-3" />
              Synthetic
            </span>
            <span className="text-[var(--color-text-muted)]">Mixed</span>
            <span className="text-amber-500 dark:text-yellow-400 font-medium flex items-center gap-1">
              <Leaf className="w-3 h-3" />
              Natural
            </span>
          </div>
        </div>

        {/* Category label */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.3 }}
          className={cn(
            'flex items-center gap-2 px-3 py-2 rounded-lg mb-3',
            category.bg, category.border, 'border'
          )}
        >
          <IconComponent className={cn('w-4 h-4', category.text)} />
          <span className={cn('text-sm font-medium', category.text)}>
            {category.label}
          </span>
          <span className="text-xs text-[var(--color-text-muted)] ml-auto">
            {category.description}
          </span>
        </motion.div>

        {/* Interpretation */}
        <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed">
          {interpretation}
        </p>

        {/* Caveats */}
        {caveats.length > 0 && (
          <div className="mt-3 pt-3 border-t border-[var(--color-border)]">
            {caveats.map((caveat, index) => (
              <div
                key={index}
                className="flex items-start gap-2 text-[10px] text-amber-600 dark:text-amber-400"
              >
                <svg className="w-3 h-3 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L1 21h22L12 2zm0 3.17L20.12 19H3.88L12 5.17zM11 10v4h2v-4h-2zm0 6v2h2v-2h-2z" />
                </svg>
                <span>{caveat}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </motion.div>
  );
}
