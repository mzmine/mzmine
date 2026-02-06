import { memo } from 'react';
import { cn } from '../../lib/utils';

interface ProcessingLogoProps {
  progress?: number;
  status?: 'pending' | 'processing' | 'complete' | 'failed' | 'cancelled';
  className?: string;
}

/**
 * Optimized logo for batch processing visualization.
 * Uses CSS animations instead of framer-motion for better performance.
 * Features progress ring and subtle glow effects.
 */
export const ProcessingLogo = memo(function ProcessingLogo({
  progress = 0,
  status = 'processing',
  className,
}: ProcessingLogoProps) {
  const isActive = status === 'processing' || status === 'pending';
  const isComplete = status === 'complete';
  const isFailed = status === 'failed' || status === 'cancelled';

  // Calculate stroke dashoffset for progress ring
  const circumference = 2 * Math.PI * 46;
  const strokeDashoffset = circumference * (1 - progress / 100);

  return (
    <div className={cn('relative flex items-center justify-center', className)}>
      {/* Outer glow - CSS only */}
      <div
        className={cn(
          'absolute inset-0 rounded-full transition-opacity duration-500',
          isActive && 'animate-pulse'
        )}
        style={{
          background: isComplete
            ? 'radial-gradient(circle, rgba(234,179,8,0.3) 0%, transparent 70%)'
            : isFailed
            ? 'radial-gradient(circle, rgba(239,68,68,0.3) 0%, transparent 70%)'
            : 'radial-gradient(circle, rgba(196,30,58,0.3) 0%, rgba(217,119,6,0.15) 50%, transparent 70%)',
        }}
      />

      {/* Single rotating ring - CSS animation */}
      {isActive && (
        <div
          className="absolute w-36 h-36 rounded-full border border-dashed border-[var(--color-primary)]/30 animate-spin"
          style={{ animationDuration: '8s' }}
        />
      )}

      {/* Progress ring */}
      <svg className="absolute w-32 h-32" viewBox="0 0 100 100">
        {/* Background ring */}
        <circle
          cx="50"
          cy="50"
          r="46"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          className="text-gray-200 dark:text-gray-700"
        />
        {/* Progress arc */}
        <circle
          cx="50"
          cy="50"
          r="46"
          fill="none"
          stroke="url(#progressGradient)"
          strokeWidth="4"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          className="transition-all duration-300 ease-out"
          style={{
            transformOrigin: 'center',
            transform: 'rotate(-90deg)',
          }}
        />
        <defs>
          <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#c41e3a" />
            <stop offset="50%" stopColor="#e11d48" />
            <stop offset="100%" stopColor="#d97706" />
          </linearGradient>
        </defs>
      </svg>

      {/* Logo container */}
      <div
        className={cn(
          'relative w-24 h-24 rounded-full overflow-hidden transition-shadow duration-500',
          isActive && 'animate-pulse'
        )}
        style={{
          boxShadow: isComplete
            ? '0 0 40px rgba(234,179,8,0.5), 0 0 60px rgba(234,179,8,0.2)'
            : isFailed
            ? '0 0 30px rgba(239,68,68,0.4), 0 0 50px rgba(239,68,68,0.2)'
            : '0 0 25px rgba(196,30,58,0.4), 0 0 50px rgba(217,119,6,0.2)',
        }}
      >
        {/* Inner glow overlay */}
        <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent pointer-events-none z-10" />

        {/* Logo image */}
        <img
          src="/logo.png"
          alt="ChemAudit"
          className="w-full h-full object-cover"
        />
      </div>

      {/* Success checkmark overlay */}
      {isComplete && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-24 h-24 rounded-full bg-gradient-to-br from-yellow-400/20 to-amber-500/20 flex items-center justify-center">
            <svg
              className="w-12 h-12 text-amber-500"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={3}
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>
      )}

      {/* Failed X overlay */}
      {isFailed && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-24 h-24 rounded-full bg-red-500/20 flex items-center justify-center">
            <svg
              className="w-12 h-12 text-red-500"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={3}
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
        </div>
      )}
    </div>
  );
});
