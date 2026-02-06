import { forwardRef } from 'react';
import { motion, type HTMLMotionProps } from 'framer-motion';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../lib/utils';

/**
 * Claymorphism button styles
 *
 * Claymorphism creates a soft, 3D clay-like appearance using:
 * - Inner highlight shadow (top-left) for the puffy raised effect
 * - Inner dark shadow (bottom-right) for depth
 * - Outer shadow for floating effect
 * - Highly rounded corners for soft, organic feel
 */
const clayButtonVariants = cva(
  [
    'inline-flex items-center justify-center gap-2',
    'font-medium tracking-tight',
    'transition-all duration-200 ease-out',
    'disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none',
    'select-none',
  ].join(' '),
  {
    variants: {
      variant: {
        default: [
          'bg-[var(--color-surface-elevated)]',
          'text-[var(--color-text-primary)]',
          // Claymorphism shadows: inner light (top-left), inner dark (bottom-right), outer float
          'shadow-[inset_2px_2px_4px_rgba(255,255,255,0.7),inset_-2px_-2px_4px_rgba(0,0,0,0.08),0_4px_12px_rgba(0,0,0,0.1)]',
          'dark:shadow-[inset_2px_2px_4px_rgba(255,255,255,0.05),inset_-2px_-2px_4px_rgba(0,0,0,0.3),0_4px_12px_rgba(0,0,0,0.3)]',
          'hover:shadow-[inset_2px_2px_5px_rgba(255,255,255,0.8),inset_-2px_-2px_5px_rgba(0,0,0,0.1),0_6px_16px_rgba(0,0,0,0.12)]',
          'dark:hover:shadow-[inset_2px_2px_5px_rgba(255,255,255,0.08),inset_-2px_-2px_5px_rgba(0,0,0,0.35),0_6px_16px_rgba(0,0,0,0.35)]',
          'border border-[var(--color-border)]/30',
        ].join(' '),
        primary: [
          // Laboratory Crimson - the app's primary color
          'bg-gradient-to-br from-rose-500 via-red-600 to-rose-700',
          'dark:from-rose-600 dark:via-red-700 dark:to-rose-800',
          'text-white',
          // Primary claymorphism with crimson glow
          'shadow-[inset_2px_2px_6px_rgba(255,255,255,0.35),inset_-2px_-2px_6px_rgba(0,0,0,0.2),0_6px_20px_rgba(196,30,58,0.35)]',
          'hover:shadow-[inset_2px_2px_8px_rgba(255,255,255,0.45),inset_-2px_-2px_8px_rgba(0,0,0,0.25),0_8px_28px_rgba(196,30,58,0.45)]',
          'border border-rose-400/30',
          'dark:border-rose-500/20',
        ].join(' '),
        accent: [
          // Warm Amber - the app's accent color
          'bg-gradient-to-br from-amber-400 via-amber-500 to-orange-500',
          'dark:from-amber-500 dark:via-amber-600 dark:to-orange-600',
          'text-white',
          // Accent claymorphism with amber glow
          'shadow-[inset_2px_2px_6px_rgba(255,255,255,0.4),inset_-2px_-2px_6px_rgba(0,0,0,0.15),0_6px_20px_rgba(217,119,6,0.35)]',
          'hover:shadow-[inset_2px_2px_8px_rgba(255,255,255,0.5),inset_-2px_-2px_8px_rgba(0,0,0,0.2),0_8px_28px_rgba(217,119,6,0.45)]',
          'border border-amber-300/30',
          'dark:border-amber-400/20',
        ].join(' '),
        ghost: [
          'bg-transparent',
          'text-[var(--color-text-secondary)]',
          'hover:bg-[var(--color-surface-sunken)]',
          'hover:text-[var(--color-text-primary)]',
          // Subtle clay effect on hover only
          'hover:shadow-[inset_1px_1px_3px_rgba(255,255,255,0.5),inset_-1px_-1px_3px_rgba(0,0,0,0.05),0_2px_8px_rgba(0,0,0,0.05)]',
          'dark:hover:shadow-[inset_1px_1px_3px_rgba(255,255,255,0.03),inset_-1px_-1px_3px_rgba(0,0,0,0.2),0_2px_8px_rgba(0,0,0,0.2)]',
        ].join(' '),
        outline: [
          'bg-transparent',
          'text-[var(--color-primary)]',
          'border-2 border-[var(--color-primary)]/60',
          'hover:bg-[var(--color-primary)]/5',
          // Outline clay effect
          'shadow-[0_4px_12px_rgba(0,0,0,0.08)]',
          'hover:shadow-[inset_1px_1px_3px_rgba(255,255,255,0.3),inset_-1px_-1px_3px_rgba(0,0,0,0.05),0_6px_16px_rgba(0,0,0,0.1)]',
          'dark:shadow-[0_4px_12px_rgba(0,0,0,0.2)]',
          'dark:hover:shadow-[inset_1px_1px_3px_rgba(255,255,255,0.05),inset_-1px_-1px_3px_rgba(0,0,0,0.15),0_6px_16px_rgba(0,0,0,0.25)]',
        ].join(' '),
        danger: [
          'bg-gradient-to-br from-red-400 via-red-500 to-rose-600',
          'text-white',
          // Danger claymorphism with red glow
          'shadow-[inset_2px_2px_6px_rgba(255,255,255,0.35),inset_-2px_-2px_6px_rgba(0,0,0,0.2),0_6px_20px_rgba(239,68,68,0.35)]',
          'hover:shadow-[inset_2px_2px_8px_rgba(255,255,255,0.45),inset_-2px_-2px_8px_rgba(0,0,0,0.25),0_8px_28px_rgba(239,68,68,0.45)]',
          'border border-red-300/30',
        ].join(' '),
      },
      size: {
        sm: 'px-4 py-2 text-xs rounded-2xl min-h-[32px]',
        md: 'px-5 py-2.5 text-sm rounded-2xl min-h-[40px]',
        lg: 'px-7 py-3.5 text-base rounded-3xl min-h-[48px]',
        icon: 'p-2.5 rounded-2xl min-h-[40px] min-w-[40px]',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
);

interface ClayButtonProps
  extends Omit<HTMLMotionProps<'button'>, 'children'>,
    VariantProps<typeof clayButtonVariants> {
  children: React.ReactNode;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

/**
 * Claymorphism button with soft 3D clay-like appearance
 *
 * Features:
 * - Inner highlight and shadow for puffy 3D effect
 * - Outer shadow for floating appearance
 * - Squishy press animation
 * - Smooth hover transitions
 */
export const ClayButton = forwardRef<HTMLButtonElement, ClayButtonProps>(
  ({
    children,
    variant,
    size,
    loading = false,
    leftIcon,
    rightIcon,
    className,
    disabled,
    ...motionProps
  }, ref) => {
    const isDisabled = disabled || loading;

    return (
      <motion.button
        ref={ref}
        className={cn(clayButtonVariants({ variant, size }), className)}
        disabled={isDisabled}
        // Claymorphism hover: slight lift
        whileHover={!isDisabled ? {
          scale: 1.02,
          y: -2,
          transition: { duration: 0.2, ease: 'easeOut' }
        } : undefined}
        // Claymorphism press: squishy clay press effect
        whileTap={!isDisabled ? {
          scale: 0.96,
          y: 1,
          transition: { duration: 0.1, ease: 'easeIn' }
        } : undefined}
        {...motionProps}
      >
        {/* Loading spinner */}
        {loading ? (
          <motion.svg
            className="w-4 h-4"
            viewBox="0 0 24 24"
            fill="none"
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="3"
            />
            <path
              className="opacity-90"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </motion.svg>
        ) : leftIcon ? (
          <span className="flex-shrink-0 -ml-0.5">{leftIcon}</span>
        ) : null}

        <span className="truncate">{children}</span>

        {!loading && rightIcon && (
          <span className="flex-shrink-0 -mr-0.5">{rightIcon}</span>
        )}
      </motion.button>
    );
  }
);

ClayButton.displayName = 'ClayButton';
