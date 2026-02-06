import { motion } from 'framer-motion';
import { cn } from '../../lib/utils';
import { staggerContainer } from '../../lib/motion';

interface BentoGridProps {
  children: React.ReactNode;
  className?: string;
  columns?: 2 | 3 | 4;
  gap?: 'sm' | 'md' | 'lg';
}

const gapStyles = {
  sm: 'gap-3',
  md: 'gap-4',
  lg: 'gap-6',
};

const columnStyles = {
  2: 'grid-cols-1 sm:grid-cols-2',
  3: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3',
  4: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4',
};

/**
 * Bento grid container for organizing tiles
 */
export function BentoGrid({
  children,
  className,
  columns = 4,
  gap = 'md',
}: BentoGridProps) {
  return (
    <motion.div
      className={cn(
        'grid',
        columnStyles[columns],
        gapStyles[gap],
        className
      )}
      variants={staggerContainer}
      initial="initial"
      animate="animate"
    >
      {children}
    </motion.div>
  );
}
