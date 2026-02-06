import { motion, AnimatePresence } from 'framer-motion';
import { useEffect, useState } from 'react';
import { useThemeContext } from '../../contexts/ThemeContext';

interface SplashScreenProps {
  isVisible: boolean;
  onComplete: () => void;
}

/**
 * Stunning splash screen with logo reveal and molecular animations
 */
export function SplashScreen({ isVisible, onComplete }: SplashScreenProps) {
  const { resolvedTheme } = useThemeContext();
  const isDark = resolvedTheme === 'dark';
  const [phase, setPhase] = useState<'enter' | 'reveal' | 'exit'>('enter');

  useEffect(() => {
    if (isVisible) {
      setPhase('enter');
      const revealTimer = setTimeout(() => setPhase('reveal'), 100);
      const exitTimer = setTimeout(() => setPhase('exit'), 1200);
      const completeTimer = setTimeout(() => {
        onComplete();
        setPhase('enter');
      }, 1600);

      return () => {
        clearTimeout(revealTimer);
        clearTimeout(exitTimer);
        clearTimeout(completeTimer);
      };
    }
  }, [isVisible, onComplete]);

  // Theme-aware colors
  const colors = isDark
    ? {
        bg: '#0a0a0f',
        bgGradient: 'from-slate-950 via-slate-900 to-slate-950',
        primary: '#dc2626',
        primaryGlow: 'rgba(220, 38, 38, 0.4)',
        accent: '#f59e0b',
        accentGlow: 'rgba(245, 158, 11, 0.3)',
        text: '#ffffff',
        textMuted: 'rgba(255,255,255,0.5)',
      }
    : {
        bg: '#fafaf9',
        bgGradient: 'from-stone-50 via-amber-50/20 to-rose-50/20',
        primary: '#dc2626',
        primaryGlow: 'rgba(220, 38, 38, 0.25)',
        accent: '#d97706',
        accentGlow: 'rgba(217, 119, 6, 0.2)',
        text: '#1a1a1a',
        textMuted: 'rgba(0,0,0,0.45)',
      };

  // Floating particles configuration
  const particles = Array.from({ length: 6 }, (_, i) => ({
    id: i,
    size: 4 + Math.random() * 4,
    x: 20 + Math.random() * 60,
    y: 20 + Math.random() * 60,
    duration: 3 + Math.random() * 2,
    delay: Math.random() * 0.5,
  }));

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center overflow-hidden"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.3 }}
        >
          {/* Background */}
          <div
            className={`absolute inset-0 bg-gradient-to-br ${colors.bgGradient}`}
            style={{ backgroundColor: colors.bg }}
          />

          {/* Animated gradient mesh */}
          <div className="absolute inset-0 overflow-hidden">
            <motion.div
              className="absolute w-[600px] h-[600px] rounded-full blur-[100px]"
              style={{
                background: `radial-gradient(circle, ${colors.primaryGlow} 0%, transparent 70%)`,
                left: '50%',
                top: '50%',
                transform: 'translate(-50%, -50%)',
              }}
              animate={{
                scale: phase === 'exit' ? [1, 1.5] : [0.8, 1, 0.8],
                opacity: phase === 'exit' ? [1, 0] : [0.6, 1, 0.6],
              }}
              transition={{
                duration: phase === 'exit' ? 0.4 : 3,
                repeat: phase === 'exit' ? 0 : Infinity,
                ease: 'easeInOut',
              }}
            />
            <motion.div
              className="absolute w-[400px] h-[400px] rounded-full blur-[80px]"
              style={{
                background: `radial-gradient(circle, ${colors.accentGlow} 0%, transparent 70%)`,
                left: '60%',
                top: '40%',
              }}
              animate={{
                scale: phase === 'exit' ? [1, 1.3] : [1, 1.2, 1],
                opacity: phase === 'exit' ? [0.8, 0] : [0.4, 0.7, 0.4],
                x: phase === 'exit' ? 0 : [0, 30, 0],
                y: phase === 'exit' ? 0 : [0, -20, 0],
              }}
              transition={{
                duration: phase === 'exit' ? 0.4 : 4,
                repeat: phase === 'exit' ? 0 : Infinity,
                ease: 'easeInOut',
                delay: 0.5,
              }}
            />
          </div>

          {/* Floating particles */}
          {particles.map((particle) => (
            <motion.div
              key={particle.id}
              className="absolute rounded-full"
              style={{
                width: particle.size,
                height: particle.size,
                background: particle.id % 2 === 0 ? colors.primary : colors.accent,
                left: `${particle.x}%`,
                top: `${particle.y}%`,
                filter: 'blur(0.5px)',
              }}
              initial={{ opacity: 0, scale: 0 }}
              animate={{
                opacity: phase === 'exit' ? 0 : [0, 0.6, 0],
                scale: phase === 'exit' ? 0 : [0, 1, 0],
                y: phase === 'exit' ? 0 : [0, -40, -80],
              }}
              transition={{
                duration: particle.duration,
                repeat: phase === 'exit' ? 0 : Infinity,
                delay: particle.delay,
                ease: 'easeOut',
              }}
            />
          ))}

          {/* Center content */}
          <div className="relative flex flex-col items-center">
            {/* Outer rotating ring */}
            <motion.div
              className="absolute w-44 h-44"
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{
                opacity: phase === 'exit' ? 0 : 0.4,
                scale: phase === 'exit' ? 1.5 : 1,
                rotate: 360,
              }}
              transition={{
                opacity: { duration: 0.3 },
                scale: { duration: phase === 'exit' ? 0.3 : 0.5 },
                rotate: { duration: 8, repeat: Infinity, ease: 'linear' },
              }}
            >
              <svg viewBox="0 0 176 176" className="w-full h-full">
                <defs>
                  <linearGradient id="outerRingGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor={colors.primary} stopOpacity="0.6" />
                    <stop offset="50%" stopColor={colors.accent} stopOpacity="0.4" />
                    <stop offset="100%" stopColor={colors.primary} stopOpacity="0.6" />
                  </linearGradient>
                </defs>
                <circle
                  cx="88"
                  cy="88"
                  r="84"
                  fill="none"
                  stroke="url(#outerRingGrad)"
                  strokeWidth="1.5"
                  strokeDasharray="8 12"
                  strokeLinecap="round"
                />
              </svg>
            </motion.div>

            {/* Inner pulsing ring */}
            <motion.div
              className="absolute w-32 h-32"
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{
                opacity: phase === 'exit' ? 0 : [0.3, 0.6, 0.3],
                scale: phase === 'exit' ? 1.3 : [0.95, 1.05, 0.95],
              }}
              transition={{
                duration: phase === 'exit' ? 0.3 : 2,
                repeat: phase === 'exit' ? 0 : Infinity,
                ease: 'easeInOut',
              }}
            >
              <svg viewBox="0 0 128 128" className="w-full h-full">
                <circle
                  cx="64"
                  cy="64"
                  r="60"
                  fill="none"
                  stroke={colors.primary}
                  strokeWidth="2"
                  strokeOpacity="0.3"
                />
              </svg>
            </motion.div>

            {/* Logo with glow - NO background */}
            <motion.div
              className="relative z-10"
              initial={{ scale: 0, opacity: 0 }}
              animate={{
                scale: phase === 'exit' ? [1, 1.1, 0] : [0, 1.1, 1],
                opacity: phase === 'exit' ? [1, 0] : 1,
              }}
              transition={{
                duration: phase === 'exit' ? 0.3 : 0.6,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              {/* Glow behind logo */}
              <div
                className="absolute inset-0 blur-2xl scale-150"
                style={{
                  background: `radial-gradient(circle, ${colors.primaryGlow} 0%, transparent 70%)`,
                }}
              />
              <img
                src="/logo.png"
                alt="ChemAudit"
                className="relative w-24 h-24 object-contain drop-shadow-2xl"
                style={{
                  filter: isDark
                    ? `drop-shadow(0 0 20px ${colors.primaryGlow})`
                    : `drop-shadow(0 4px 12px rgba(0,0,0,0.15))`,
                }}
              />
            </motion.div>

            {/* Brand name */}
            <motion.div
              className="mt-6 text-center"
              initial={{ opacity: 0, y: 20 }}
              animate={{
                opacity: phase === 'exit' ? 0 : 1,
                y: phase === 'exit' ? -15 : 0,
              }}
              transition={{
                delay: phase === 'exit' ? 0 : 0.3,
                duration: 0.4,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              <h1 className="text-3xl font-bold tracking-tight">
                <span style={{ color: colors.primary }} className="font-extrabold">
                  Chem
                </span>
                <span style={{ color: colors.text }} className="font-semibold">
                  Audit
                </span>
              </h1>

              {/* Animated underline */}
              <motion.div
                className="h-0.5 mt-3 mx-auto rounded-full"
                style={{
                  background: `linear-gradient(90deg, transparent, ${colors.primary}, ${colors.accent}, transparent)`,
                }}
                initial={{ width: 0, opacity: 0 }}
                animate={{
                  width: phase === 'exit' ? 0 : 120,
                  opacity: phase === 'exit' ? 0 : 1,
                }}
                transition={{
                  delay: phase === 'exit' ? 0 : 0.45,
                  duration: 0.4,
                  ease: [0.22, 1, 0.36, 1],
                }}
              />

              {/* Tagline */}
              <motion.p
                className="text-xs mt-3 tracking-[0.2em] uppercase font-medium"
                style={{ color: colors.textMuted }}
                initial={{ opacity: 0 }}
                animate={{ opacity: phase === 'exit' ? 0 : 1 }}
                transition={{
                  delay: phase === 'exit' ? 0 : 0.55,
                  duration: 0.3,
                }}
              >
                Chemical Structure Validation
              </motion.p>
            </motion.div>

            {/* Loading dots */}
            <motion.div
              className="flex items-center gap-1.5 mt-8"
              initial={{ opacity: 0 }}
              animate={{ opacity: phase === 'exit' ? 0 : 1 }}
              transition={{ delay: phase === 'exit' ? 0 : 0.6, duration: 0.3 }}
            >
              {[0, 1, 2].map((i) => (
                <motion.div
                  key={i}
                  className="w-1.5 h-1.5 rounded-full"
                  style={{ background: colors.primary }}
                  animate={{
                    scale: [1, 1.4, 1],
                    opacity: [0.4, 1, 0.4],
                  }}
                  transition={{
                    duration: 0.8,
                    repeat: Infinity,
                    delay: i * 0.15,
                    ease: 'easeInOut',
                  }}
                />
              ))}
            </motion.div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
