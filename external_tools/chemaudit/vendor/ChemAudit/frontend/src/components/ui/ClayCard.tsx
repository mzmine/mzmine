import { motion, type HTMLMotionProps } from 'framer-motion';
import { cn } from '../../lib/utils';
import { hoverLift } from '../../lib/motion';

type ClayCardVariant = 'default' | 'elevated' | 'accent' | 'flat';
type ClayCardSize = 'sm' | 'md' | 'lg';

interface ClayCardProps extends Omit<HTMLMotionProps<'div'>, 'children'> {
  children: React.ReactNode;
  variant?: ClayCardVariant;
  size?: ClayCardSize;
  hover?: boolean;
  className?: string;
}

const variantStyles: Record<ClayCardVariant, string> = {
  default: 'clay-card',
  elevated: 'clay-card-lg',
  accent: 'clay-card border-2 border-chem-accent-400/20 dark:border-chem-accent-400/10',
  flat: 'clay-surface',
};

const sizeStyles: Record<ClayCardSize, string> = {
  sm: 'clay-card-sm p-4',
  md: 'p-6',
  lg: 'clay-card-lg p-8',
};

/**
 * Claymorphic card component with soft 3D effect
 */
export function ClayCard({
  children,
  variant = 'default',
  size = 'md',
  hover = false,
  className,
  ...motionProps
}: ClayCardProps) {
  return (
    <motion.div
      className={cn(
        variantStyles[variant],
        sizeStyles[size],
        hover && 'clay-card-hover cursor-pointer',
        className
      )}
      {...(hover ? hoverLift : {})}
      {...motionProps}
    >
      {children}
    </motion.div>
  );
}

interface ClayCardHeaderProps {
  children: React.ReactNode;
  className?: string;
}

export function ClayCardHeader({ children, className }: ClayCardHeaderProps) {
  return (
    <div className={cn('mb-4', className)}>
      {children}
    </div>
  );
}

interface ClayCardTitleProps {
  children: React.ReactNode;
  className?: string;
}

export function ClayCardTitle({ children, className }: ClayCardTitleProps) {
  return (
    <h3 className={cn('text-lg font-semibold text-text-primary', className)}>
      {children}
    </h3>
  );
}

interface ClayCardDescriptionProps {
  children: React.ReactNode;
  className?: string;
}

export function ClayCardDescription({ children, className }: ClayCardDescriptionProps) {
  return (
    <p className={cn('text-sm text-text-secondary mt-1', className)}>
      {children}
    </p>
  );
}

interface ClayCardContentProps {
  children: React.ReactNode;
  className?: string;
}

export function ClayCardContent({ children, className }: ClayCardContentProps) {
  return (
    <div className={cn(className)}>
      {children}
    </div>
  );
}

interface ClayCardFooterProps {
  children: React.ReactNode;
  className?: string;
}

export function ClayCardFooter({ children, className }: ClayCardFooterProps) {
  return (
    <div className={cn('mt-4 pt-4 border-t border-chem-primary-600/10 dark:border-chem-primary-400/10', className)}>
      {children}
    </div>
  );
}
