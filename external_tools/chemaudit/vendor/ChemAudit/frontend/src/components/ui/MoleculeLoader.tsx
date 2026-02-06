import { motion } from 'framer-motion';
import { cn } from '../../lib/utils';

interface MoleculeLoaderProps {
  size?: 'sm' | 'md' | 'lg';
  text?: string;
  className?: string;
}

const sizeConfig = {
  sm: { outer: 40, atom: 4, orbit: 16, text: 'text-xs' },
  md: { outer: 64, atom: 6, orbit: 24, text: 'text-sm' },
  lg: { outer: 80, atom: 8, orbit: 32, text: 'text-base' },
};

/**
 * Chemistry-themed loading animation with orbiting atoms
 */
export function MoleculeLoader({ size = 'md', text, className }: MoleculeLoaderProps) {
  const config = sizeConfig[size];

  return (
    <div className={cn('flex flex-col items-center justify-center gap-4', className)}>
      <div
        className="relative"
        style={{ width: config.outer, height: config.outer }}
      >
        {/* Central atom */}
        <motion.div
          className="absolute top-1/2 left-1/2 rounded-full bg-gradient-to-br from-chem-primary-400 to-chem-primary-600"
          style={{
            width: config.atom * 2,
            height: config.atom * 2,
            marginLeft: -config.atom,
            marginTop: -config.atom,
          }}
          animate={{
            scale: [1, 1.2, 1],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />

        {/* Orbit 1 */}
        <motion.div
          className="absolute top-1/2 left-1/2"
          style={{
            width: config.orbit * 2,
            height: config.orbit * 2,
            marginLeft: -config.orbit,
            marginTop: -config.orbit,
          }}
          animate={{ rotate: 360 }}
          transition={{
            duration: 3,
            repeat: Infinity,
            ease: 'linear',
          }}
        >
          <div
            className="absolute rounded-full bg-chem-accent-500"
            style={{
              width: config.atom,
              height: config.atom,
              top: 0,
              left: '50%',
              marginLeft: -config.atom / 2,
            }}
          />
        </motion.div>

        {/* Orbit 2 */}
        <motion.div
          className="absolute top-1/2 left-1/2"
          style={{
            width: config.outer - config.atom,
            height: config.outer - config.atom,
            marginLeft: -(config.outer - config.atom) / 2,
            marginTop: -(config.outer - config.atom) / 2,
          }}
          animate={{ rotate: -360 }}
          transition={{
            duration: 4,
            repeat: Infinity,
            ease: 'linear',
          }}
        >
          <div
            className="absolute rounded-full bg-chem-primary-400"
            style={{
              width: config.atom,
              height: config.atom,
              top: 0,
              left: '50%',
              marginLeft: -config.atom / 2,
            }}
          />
        </motion.div>

        {/* Orbit 3 - Tilted */}
        <motion.div
          className="absolute top-1/2 left-1/2"
          style={{
            width: config.orbit * 1.5,
            height: config.orbit * 1.5,
            marginLeft: -config.orbit * 0.75,
            marginTop: -config.orbit * 0.75,
            transform: 'rotateX(60deg)',
          }}
          animate={{ rotate: 360 }}
          transition={{
            duration: 2.5,
            repeat: Infinity,
            ease: 'linear',
          }}
        >
          <div
            className="absolute rounded-full bg-amber-400"
            style={{
              width: config.atom * 0.8,
              height: config.atom * 0.8,
              top: 0,
              left: '50%',
              marginLeft: -config.atom * 0.4,
            }}
          />
        </motion.div>

        {/* Orbit rings (decorative) */}
        <div
          className="absolute top-1/2 left-1/2 rounded-full border border-chem-primary-500/20 dark:border-chem-primary-400/20"
          style={{
            width: config.orbit * 2,
            height: config.orbit * 2,
            marginLeft: -config.orbit,
            marginTop: -config.orbit,
          }}
        />
        <div
          className="absolute top-1/2 left-1/2 rounded-full border border-chem-accent-500/20 dark:border-chem-accent-400/20"
          style={{
            width: config.outer - config.atom,
            height: config.outer - config.atom,
            marginLeft: -(config.outer - config.atom) / 2,
            marginTop: -(config.outer - config.atom) / 2,
          }}
        />
      </div>

      {text && (
        <motion.p
          className={cn(
            'font-medium text-chem-primary-600 dark:text-chem-primary-400',
            config.text
          )}
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        >
          {text}
        </motion.p>
      )}
    </div>
  );
}

/**
 * Simple spinner loader
 */
interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function Spinner({ size = 'md', className }: SpinnerProps) {
  const sizeStyles = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  };

  return (
    <div
      className={cn(
        'spinner',
        sizeStyles[size],
        className
      )}
    />
  );
}

/**
 * Page-level loading overlay
 */
interface PageLoaderProps {
  text?: string;
}

export function PageLoader({ text = 'Loading...' }: PageLoaderProps) {
  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <MoleculeLoader size="lg" text={text} />
    </div>
  );
}
