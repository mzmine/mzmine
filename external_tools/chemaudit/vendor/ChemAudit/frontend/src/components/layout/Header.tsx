import { useState, useCallback } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Atom, Grid3X3, Info, BookOpen, ExternalLink } from 'lucide-react';
import { ThemeToggle } from '../ui/ThemeToggle';
import { SplashScreen } from '../ui/SplashScreen';
import { cn } from '../../lib/utils';
import { API_DOCS_URL } from '../../services/api';

/**
 * Premium header with glassmorphism, refined navigation, and gradient accents
 */
export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const [showSplash, setShowSplash] = useState(false);

  // Handle logo click - show splash if not on home page
  const handleLogoClick = useCallback((e: React.MouseEvent) => {
    // If already on home page, let NavLink handle it normally
    if (location.pathname === '/') {
      return;
    }

    // Prevent default navigation
    e.preventDefault();

    // Show splash screen
    setShowSplash(true);
  }, [location.pathname]);

  // Handle splash complete - navigate to home
  const handleSplashComplete = useCallback(() => {
    setShowSplash(false);
    navigate('/');
  }, [navigate]);

  return (
    <>
      {/* Splash Screen */}
      <SplashScreen isVisible={showSplash} onComplete={handleSplashComplete} />

      <header className="sticky top-0 z-50">
        {/* Glass background with enhanced blur */}
        <div
          className={cn(
            'absolute inset-0',
            'bg-[var(--color-surface-overlay)]',
            'backdrop-blur-xl backdrop-saturate-150',
            'border-b border-[var(--color-border)]'
          )}
        />

        {/* Gradient accent line at bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-[var(--color-primary)]/50 to-transparent" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-[72px]">
            {/* Logo and branding */}
            <NavLink
              to="/"
              onClick={handleLogoClick}
              className="flex items-center gap-3.5 group"
            >
              <motion.div
                className={cn(
                  'w-11 h-11 rounded-2xl flex items-center justify-center overflow-hidden',
                  'bg-gradient-to-br from-[var(--color-surface-elevated)] to-[var(--color-surface-sunken)]',
                  'border border-[var(--color-border)]',
                  'shadow-[0_2px_12px_var(--glow-soft)]',
                  'group-hover:shadow-[0_4px_20px_var(--glow-primary)]',
                  'group-hover:border-[var(--color-primary)]/30',
                  'transition-all duration-300'
                )}
                whileHover={{ scale: 1.05, rotate: -2 }}
                whileTap={{ scale: 0.95 }}
              >
                <img
                  src="/logo.png"
                  alt="ChemAudit Logo"
                  className="w-full h-full object-contain"
                />
              </motion.div>
              <div className="hidden sm:block">
                <h1 className="text-xl font-bold text-[var(--color-text-primary)] tracking-tight font-display">
                  <span className="font-extrabold text-[var(--color-primary)]">Chem</span>
                  <span className="font-semibold">Audit</span>
                </h1>
                <p className="text-[10px] text-[var(--color-text-muted)] -mt-0.5 tracking-widest uppercase font-medium">
                  Structure Validation Suite
                </p>
              </div>
            </NavLink>

          {/* Main Navigation */}
          <nav className="flex items-center gap-1.5">
            <HeaderNavLink to="/" icon={<Atom className="w-4 h-4" />}>
              Single Validation
            </HeaderNavLink>
            <HeaderNavLink to="/batch" icon={<Grid3X3 className="w-4 h-4" />}>
              Batch Validation
            </HeaderNavLink>
            <HeaderNavLink to="/about" icon={<Info className="w-4 h-4" />}>
              About
            </HeaderNavLink>

            {/* Divider */}
            <div className="w-px h-7 bg-[var(--color-border)] mx-3" />

            {/* External Link */}
            <ExternalNavLink
              href={API_DOCS_URL}
              icon={<BookOpen className="w-4 h-4" />}
            >
              API
            </ExternalNavLink>

            {/* Divider */}
            <div className="w-px h-7 bg-[var(--color-border)] mx-3" />

            {/* Theme Toggle */}
            <ThemeToggle />
          </nav>
        </div>
      </div>
    </header>
    </>
  );
}

interface HeaderNavLinkProps {
  to: string;
  children: React.ReactNode;
  icon?: React.ReactNode;
}

function HeaderNavLink({ to, children, icon }: HeaderNavLinkProps) {
  const location = useLocation();
  const isActive = to === '/' ? location.pathname === '/' : location.pathname.startsWith(to);

  return (
    <NavLink to={to}>
      <motion.div
        className={cn(
          'relative px-4 py-2.5 text-sm font-medium rounded-xl transition-all duration-200',
          'flex items-center gap-2',
          isActive
            ? 'text-[var(--color-primary)]'
            : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
        )}
        whileHover={{ scale: 1.03 }}
        whileTap={{ scale: 0.97 }}
      >
        {/* Active indicator background */}
        {isActive && (
          <motion.div
            layoutId="activeNavBg"
            className="absolute inset-0 rounded-xl bg-[var(--color-primary)]/10 border border-[var(--color-primary)]/20"
            initial={false}
            transition={{ type: 'spring', stiffness: 400, damping: 30 }}
          />
        )}
        {/* Hover background */}
        {!isActive && (
          <div className="absolute inset-0 rounded-xl bg-[var(--color-surface-sunken)] opacity-0 hover:opacity-100 transition-opacity duration-200" />
        )}
        <span className="relative z-10">{icon}</span>
        <span className="relative z-10 hidden sm:inline font-display">{children}</span>
      </motion.div>
    </NavLink>
  );
}

interface ExternalNavLinkProps {
  href: string;
  children: React.ReactNode;
  icon?: React.ReactNode;
}

function ExternalNavLink({ href, children, icon }: ExternalNavLinkProps) {
  return (
    <motion.a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={cn(
        'relative px-4 py-2.5 text-sm font-medium rounded-xl transition-all duration-200',
        'flex items-center gap-2',
        'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
      )}
      whileHover={{ scale: 1.03 }}
      whileTap={{ scale: 0.97 }}
    >
      <div className="absolute inset-0 rounded-xl bg-[var(--color-surface-sunken)] opacity-0 hover:opacity-100 transition-opacity duration-200" />
      <span className="relative z-10">{icon}</span>
      <span className="relative z-10 hidden sm:inline font-display">{children}</span>
      <ExternalLink className="relative z-10 w-3 h-3 opacity-40" />
    </motion.a>
  );
}
