import { motion } from 'framer-motion';
import { cn } from '../../lib/utils';

type TileSize = '1x1' | '2x1' | '1x2' | '2x2';
type TileVariant = 'default' | 'glass' | 'gradient' | 'glow' | 'accent';

interface BentoTileProps {
  children: React.ReactNode;
  size?: TileSize;
  variant?: TileVariant;
  className?: string;
  hover?: boolean;
  featured?: boolean;
}

const sizeStyles: Record<TileSize, string> = {
  '1x1': 'col-span-1 row-span-1',
  '2x1': 'col-span-1 sm:col-span-2 row-span-1',
  '1x2': 'col-span-1 row-span-1 sm:row-span-2',
  '2x2': 'col-span-1 sm:col-span-2 row-span-1 sm:row-span-2',
};

const variantStyles: Record<TileVariant, string> = {
  default: 'card',
  glass: 'card-glass',
  gradient: 'card-gradient',
  glow: 'card-glow',
  accent: 'card-accent',
};

const staggerItem = {
  initial: { opacity: 0, y: 20 },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] },
  },
};

export function BentoTile({
  children,
  size = '1x1',
  variant = 'default',
  className,
  hover = true,
  featured = false,
}: BentoTileProps) {
  return (
    <motion.div
      className={cn(
        sizeStyles[size],
        variantStyles[variant],
        hover && variant === 'default' && 'card-hover',
        featured ? 'p-6 sm:p-8' : 'p-5 sm:p-6',
        className
      )}
      variants={staggerItem}
      whileHover={hover ? { y: -4, transition: { duration: 0.2 } } : undefined}
      whileTap={hover ? { scale: 0.98 } : undefined}
    >
      {children}
    </motion.div>
  );
}

interface TileHeaderProps {
  icon?: React.ReactNode;
  title: string;
  subtitle?: string;
  badge?: React.ReactNode;
  className?: string;
}

export function TileHeader({ icon, title, subtitle, badge, className }: TileHeaderProps) {
  return (
    <div className={cn('flex items-start justify-between mb-4', className)}>
      <div className="flex items-center gap-3">
        {icon && (
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-accent)]/10 flex items-center justify-center text-[var(--color-primary)]">
            {icon}
          </div>
        )}
        <div>
          <h4 className="font-semibold text-[var(--color-text-primary)] text-sm tracking-tight">
            {title}
          </h4>
          {subtitle && (
            <p className="text-xs text-[var(--color-text-muted)] mt-0.5">{subtitle}</p>
          )}
        </div>
      </div>
      {badge}
    </div>
  );
}

interface TileValueProps {
  value: string | number;
  label?: string;
  trend?: 'up' | 'down' | 'neutral';
  size?: 'sm' | 'md' | 'lg';
  glow?: boolean;
  className?: string;
}

export function TileValue({
  value,
  label,
  trend,
  size = 'md',
  glow = false,
  className,
}: TileValueProps) {
  const sizeClasses = { sm: 'text-2xl', md: 'text-4xl', lg: 'text-5xl' };

  return (
    <div className={className}>
      <div className="flex items-baseline gap-2">
        <span className={cn(sizeClasses[size], 'font-bold text-gradient tracking-tight', glow && 'text-glow')}>
          {value}
        </span>
        {trend && (
          <span className={cn(
            'text-sm font-medium',
            trend === 'up' && 'text-amber-500 dark:text-yellow-400',
            trend === 'down' && 'text-red-500',
            trend === 'neutral' && 'text-[var(--color-text-muted)]'
          )}>
            {trend === 'up' ? '↑' : trend === 'down' ? '↓' : '→'}
          </span>
        )}
      </div>
      {label && <p className="text-xs text-[var(--color-text-muted)] mt-1.5">{label}</p>}
    </div>
  );
}

interface AnimatedIconProps {
  children: React.ReactNode;
  pulse?: boolean;
  className?: string;
}

export function AnimatedIcon({ children, pulse, className }: AnimatedIconProps) {
  return (
    <motion.div
      className={cn(
        'w-10 h-10 rounded-xl flex items-center justify-center',
        'bg-gradient-to-br from-[var(--color-primary)]/15 to-[var(--color-accent)]/10',
        'text-[var(--color-primary)]',
        className
      )}
      animate={pulse ? { scale: [1, 1.05, 1], opacity: [1, 0.8, 1] } : undefined}
      transition={pulse ? { duration: 2, repeat: Infinity, ease: 'easeInOut' } : undefined}
    >
      {children}
    </motion.div>
  );
}
