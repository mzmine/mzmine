import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../lib/utils';

const badgeVariants = cva(
  'badge',
  {
    variants: {
      variant: {
        default: 'badge-primary',
        success: 'badge-success',
        warning: 'badge-warning',
        error: 'badge-error',
        info: 'badge-info',
        outline: 'bg-transparent border border-current',
      },
      size: {
        sm: 'px-2 py-0.5 text-[10px]',
        md: 'px-2.5 py-1 text-xs',
        lg: 'px-3 py-1.5 text-sm',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
);

interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  icon?: React.ReactNode;
  dot?: boolean;
}

/**
 * Premium badge component for status indicators and labels
 */
export function Badge({
  children,
  variant,
  size,
  icon,
  dot = false,
  className,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(badgeVariants({ variant, size }), dot && 'badge-dot', className)}
      {...props}
    >
      {icon && <span className="flex-shrink-0">{icon}</span>}
      {children}
    </span>
  );
}

/**
 * Pill-style badge for numeric values
 */
interface CountBadgeProps {
  count: number;
  max?: number;
  variant?: 'default' | 'success' | 'warning' | 'error';
  className?: string;
}

export function CountBadge({ count, max = 99, variant = 'default', className }: CountBadgeProps) {
  const displayCount = count > max ? `${max}+` : count;

  return (
    <Badge
      variant={variant}
      size="sm"
      className={cn('min-w-[1.25rem] justify-center tabular-nums', className)}
    >
      {displayCount}
    </Badge>
  );
}

/**
 * Status dot indicator with optional pulse animation
 */
interface StatusDotProps {
  status: 'success' | 'warning' | 'error' | 'info' | 'neutral';
  pulse?: boolean;
  className?: string;
}

const statusColors = {
  success: 'bg-status-success',
  warning: 'bg-status-warning',
  error: 'bg-status-error',
  info: 'bg-status-info',
  neutral: 'bg-[var(--color-text-muted)]',
};

export function StatusDot({ status, pulse = false, className }: StatusDotProps) {
  return (
    <span className={cn('relative inline-flex', className)}>
      <span
        className={cn(
          'w-2 h-2 rounded-full',
          statusColors[status]
        )}
      />
      {pulse && (
        <span
          className={cn(
            'absolute inline-flex w-full h-full rounded-full opacity-75 animate-ping',
            statusColors[status]
          )}
        />
      )}
    </span>
  );
}
