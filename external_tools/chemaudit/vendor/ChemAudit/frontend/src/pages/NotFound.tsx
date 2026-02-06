import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Home, FlaskConical } from 'lucide-react';
import { cn } from '../lib/utils';

export function NotFound() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] }}
      className="min-h-[60vh] flex flex-col items-center justify-center text-center px-4"
    >
      {/* Flask icon with 404 */}
      <div className="relative mb-8">
        <motion.div
          initial={{ scale: 0.8, rotate: -10 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
          className="w-24 h-24 rounded-3xl bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-accent)]/20 flex items-center justify-center"
        >
          <FlaskConical className="w-12 h-12 text-[var(--color-primary)]" />
        </motion.div>
        <motion.span
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.4 }}
          className="absolute -bottom-2 -right-2 text-4xl font-bold text-gradient font-display"
        >
          404
        </motion.span>
      </div>

      {/* Title */}
      <h1 className="text-3xl sm:text-4xl font-bold text-[var(--color-text-primary)] mb-3 font-display">
        Page Not Found
      </h1>

      {/* Description */}
      <p className="text-[var(--color-text-secondary)] mb-8 max-w-md">
        The molecule you're looking for doesn't exist in this reaction vessel.
        Let's navigate back to familiar territory.
      </p>

      {/* Home button */}
      <Link
        to="/"
        className={cn(
          'inline-flex items-center gap-2 px-6 py-3 rounded-xl font-medium transition-all',
          'bg-[var(--color-primary)] text-white',
          'hover:opacity-90 active:scale-95',
          'shadow-lg shadow-[var(--color-primary)]/20'
        )}
      >
        <Home className="w-5 h-5" />
        Back to Home
      </Link>
    </motion.div>
  );
}
