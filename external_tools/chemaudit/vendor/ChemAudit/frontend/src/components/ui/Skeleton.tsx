import { cn } from '../../lib/utils';

interface SkeletonProps {
  className?: string;
  variant?: 'text' | 'circular' | 'rectangular' | 'rounded';
  width?: string | number;
  height?: string | number;
  animation?: 'pulse' | 'wave' | 'none';
}

/**
 * Skeleton loading placeholder with clay effect
 */
export function Skeleton({
  className,
  variant = 'text',
  width,
  height,
  animation = 'wave',
}: SkeletonProps) {
  const variantStyles = {
    text: 'rounded',
    circular: 'rounded-full',
    rectangular: 'rounded-none',
    rounded: 'rounded-xl',
  };

  const animationStyles = {
    pulse: 'animate-pulse bg-chem-primary-600/10 dark:bg-chem-primary-400/10',
    wave: 'skeleton',
    none: 'bg-chem-primary-600/10 dark:bg-chem-primary-400/10',
  };

  return (
    <div
      className={cn(
        variantStyles[variant],
        animationStyles[animation],
        className
      )}
      style={{
        width: width,
        height: height || (variant === 'text' ? '1em' : undefined),
      }}
    />
  );
}

/**
 * Skeleton text block with multiple lines
 */
interface SkeletonTextProps {
  lines?: number;
  lastLineWidth?: string;
  className?: string;
}

export function SkeletonText({ lines = 3, lastLineWidth = '60%', className }: SkeletonTextProps) {
  return (
    <div className={cn('space-y-2', className)}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton
          key={i}
          variant="text"
          height={16}
          width={i === lines - 1 ? lastLineWidth : '100%'}
        />
      ))}
    </div>
  );
}

/**
 * Skeleton card placeholder
 */
interface SkeletonCardProps {
  showHeader?: boolean;
  showImage?: boolean;
  lines?: number;
  className?: string;
}

export function SkeletonCard({
  showHeader = true,
  showImage = false,
  lines = 3,
  className,
}: SkeletonCardProps) {
  return (
    <div className={cn('clay-card p-5 space-y-4', className)}>
      {showImage && (
        <Skeleton variant="rounded" height={160} className="w-full" />
      )}

      {showHeader && (
        <div className="flex items-center gap-3">
          <Skeleton variant="circular" width={40} height={40} />
          <div className="flex-1 space-y-2">
            <Skeleton variant="text" height={16} className="w-3/4" />
            <Skeleton variant="text" height={12} className="w-1/2" />
          </div>
        </div>
      )}

      <SkeletonText lines={lines} />
    </div>
  );
}

/**
 * Skeleton tile for bento grid
 */
interface SkeletonTileProps {
  size?: '1x1' | '2x1' | '1x2' | '2x2';
  className?: string;
}

export function SkeletonTile({ size = '1x1', className }: SkeletonTileProps) {
  const sizeStyles = {
    '1x1': 'col-span-1 row-span-1',
    '2x1': 'col-span-1 sm:col-span-2 row-span-1',
    '1x2': 'col-span-1 row-span-1 sm:row-span-2',
    '2x2': 'col-span-1 sm:col-span-2 row-span-1 sm:row-span-2',
  };

  return (
    <div className={cn('clay-card p-5', sizeStyles[size], className)}>
      <div className="space-y-4 h-full">
        <div className="flex items-center gap-3">
          <Skeleton variant="rounded" width={36} height={36} />
          <Skeleton variant="text" height={16} className="w-24" />
        </div>
        <Skeleton variant="text" height={32} className="w-16" />
        <Skeleton variant="text" height={12} className="w-full" />
      </div>
    </div>
  );
}
