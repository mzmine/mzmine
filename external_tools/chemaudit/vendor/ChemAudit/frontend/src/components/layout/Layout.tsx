import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Header } from './Header';
import { cn } from '../../lib/utils';

interface LayoutProps {
  children: React.ReactNode;
}

// Refined ambient orb configurations
const orbConfigs = [
  {
    position: '-top-40 -right-40',
    size: 'w-[600px] h-[600px]',
    color: 'bg-[var(--color-primary)]',
    blur: 'blur-[120px]',
    scale: [1, 1.08, 1],
    opacity: [0.06, 0.1, 0.06],
    duration: 10,
    delay: 0,
  },
  {
    position: 'top-1/4 -left-40',
    size: 'w-[500px] h-[500px]',
    color: 'bg-[var(--color-accent)]',
    blur: 'blur-[100px]',
    scale: [1, 1.12, 1],
    opacity: [0.04, 0.08, 0.04],
    duration: 12,
    delay: 3,
  },
  {
    position: '-bottom-32 right-1/4',
    size: 'w-[450px] h-[450px]',
    color: 'bg-[var(--color-secondary)]',
    blur: 'blur-[90px]',
    scale: [1, 1.06, 1],
    opacity: [0.03, 0.06, 0.03],
    duration: 14,
    delay: 6,
  },
];

// Steam animation configurations for coffee cup
const steamConfigs = [
  { x: -2, duration: 2.0, delay: 0 },
  { x: 0, duration: 2.2, delay: 0.5 },
  { x: 2, duration: 1.8, delay: 0.3 },
];

/**
 * Premium layout wrapper with ambient gradient background and refined footer
 */
export function Layout({ children }: LayoutProps) {
  return (
    <div className="min-h-screen flex flex-col bg-[var(--color-surface)]">
      {/* Ambient gradient orbs */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        {orbConfigs.map((orb, i) => (
          <motion.div
            key={i}
            className={cn('absolute rounded-full', orb.position, orb.size, orb.color, orb.blur)}
            animate={{ scale: orb.scale, opacity: orb.opacity }}
            transition={{ duration: orb.duration, repeat: Infinity, ease: 'easeInOut', delay: orb.delay }}
          />
        ))}
      </div>

      {/* Premium grid pattern overlay */}
      <div
        className="fixed inset-0 pointer-events-none opacity-[0.015] dark:opacity-[0.025]"
        style={{
          backgroundImage: `
            linear-gradient(var(--color-text-primary) 1px, transparent 1px),
            linear-gradient(90deg, var(--color-text-primary) 1px, transparent 1px)
          `,
          backgroundSize: '80px 80px',
        }}
      />

      {/* Header - sticky at top */}
      <Header />

      {/* Main content - grows to push footer down */}
      <motion.main
        className="relative flex-1 py-10 z-0"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] }}
      >
        {children}
      </motion.main>

      {/* Premium Footer */}
      <footer className="relative mt-auto">
        {/* Gradient separator */}
        <div className="h-px bg-gradient-to-r from-transparent via-[var(--color-border-strong)] to-transparent" />

        {/* Footer content */}
        <div className="bg-[var(--color-surface-sunken)]/60 dark:bg-[var(--color-surface-elevated)]/40 backdrop-blur-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
            {/* Two-column layout: Attribution left, Copyright + GitHub right */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-[var(--color-text-muted)]">

              {/* Left side - Attribution */}
              <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-start">
                <span className="font-medium">Made with</span>
                {/* Animated Coffee Cup with Steam */}
                <div className="relative flex items-center justify-center">
                  {/* Steam/Vapor - positioned closer to cup */}
                  <div className="absolute -top-2.5 left-1/2 -translate-x-1/2 flex gap-[1px]">
                    {steamConfigs.map((steam, i) => (
                      <motion.svg
                        key={i}
                        width="5"
                        height="10"
                        viewBox="0 0 5 10"
                        className="text-[var(--color-text-muted)]"
                        style={{ marginLeft: steam.x }}
                        animate={{
                          opacity: [0, 0.6, 0.3, 0],
                          y: [0, -3, -6, -10],
                          x: [0, 0.5, -0.5, 0],
                        }}
                        transition={{
                          duration: steam.duration,
                          repeat: Infinity,
                          ease: 'easeOut',
                          delay: steam.delay,
                        }}
                      >
                        <path
                          d="M2.5 10C2.5 10 1 7 1 5C1 3 2.5 1 2.5 0C2.5 1 4 3 4 5C4 7 2.5 10 2.5 10Z"
                          fill="currentColor"
                          fillOpacity="0.6"
                        />
                      </motion.svg>
                    ))}
                  </div>
                  {/* Coffee cup icon with subtle animation */}
                  <motion.svg
                    className="w-6 h-6 text-[var(--color-primary)]"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    animate={{ rotate: [-0.5, 0.5, -0.5] }}
                    transition={{
                      duration: 3,
                      repeat: Infinity,
                      ease: 'easeInOut'
                    }}
                  >
                    <path d="M17 8h1a4 4 0 1 1 0 8h-1" />
                    <path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z" />
                  </motion.svg>
                </div>
                <span className="font-medium">by</span>
                <motion.a
                  href="https://kohulanr.com"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="relative font-semibold text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] transition-colors duration-200"
                  whileHover={{
                    scale: 1.05,
                    y: -1,
                  }}
                  whileTap={{ scale: 0.98 }}
                >
                  <motion.span
                    className="relative z-10"
                    whileHover={{
                      textShadow: '0 0 8px var(--color-primary)',
                    }}
                  >
                    Kohulan.R
                  </motion.span>
                </motion.a>
                <span>at</span>
                <a
                  href="https://www.uni-jena.de"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] transition-colors duration-200"
                >
                  Friedrich Schiller University Jena
                </a>
              </div>

              {/* Right side - Copyright + Privacy + GitHub grouped */}
              <div className="flex items-center gap-3 sm:gap-4">
                {/* Copyright */}
                <span className="text-xs text-[var(--color-text-muted)]">
                  &copy; {new Date().getFullYear()} ChemAudit
                </span>

                {/* Subtle separator dot */}
                <span className="w-1 h-1 rounded-full bg-[var(--color-border-strong)] hidden sm:block" />

                {/* Privacy link */}
                <Link
                  to="/privacy"
                  className="text-xs text-[var(--color-text-muted)] hover:text-[var(--color-primary)] transition-colors duration-200"
                >
                  Privacy
                </Link>

                {/* Subtle separator dot */}
                <span className="w-1 h-1 rounded-full bg-[var(--color-border-strong)]" />

                {/* GitHub link */}
                <motion.a
                  href="https://github.com/Kohulan/ChemAudit"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group flex items-center gap-2 text-[var(--color-text-muted)] hover:text-[var(--color-primary)] transition-colors duration-200 font-medium"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  <motion.div
                    className="relative"
                    whileHover={{
                      rotate: [0, -12, 12, -8, 8, 0],
                    }}
                    transition={{
                      duration: 0.6,
                      ease: 'easeInOut',
                    }}
                  >
                    <svg
                      className="w-5 h-5 transition-transform duration-200 group-hover:scale-110"
                      viewBox="0 0 24 24"
                      fill="currentColor"
                    >
                      <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                    </svg>
                    {/* Subtle glow effect on hover */}
                    <motion.div
                      className="absolute inset-0 rounded-full bg-[var(--color-primary)] opacity-0 blur-md -z-10"
                      whileHover={{ opacity: 0.3, scale: 1.5 }}
                      transition={{ duration: 0.2 }}
                    />
                  </motion.div>
                  <span className="hidden sm:inline">GitHub</span>
                </motion.a>
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

