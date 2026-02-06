import { useRef } from 'react';
import { motion, useScroll, useTransform, useInView } from 'framer-motion';
import {
  Building2,
  Mail,
  Globe,
  Github,
  Code2,
  Database,
  Server,
  Layout,
  TestTube,
  Heart,
  Coffee,
  ExternalLink,
  MapPin,
  Sparkles,
  Zap,
  Shield,
  BarChart3,
  Pill,
  Beaker,
  AlertTriangle,
  ShieldCheck,
  FlaskConical,
  Activity,
  Target,
  Brain,
} from 'lucide-react';
import { cn } from '../lib/utils';

// Floating molecule configurations for background
const moleculeConfigs = [
  { x: '10%', y: '15%', size: 60, delay: 0, duration: 20, opacity: 0.15 },
  { x: '85%', y: '25%', size: 80, delay: 2, duration: 25, opacity: 0.12 },
  { x: '75%', y: '60%', size: 50, delay: 4, duration: 18, opacity: 0.18 },
  { x: '15%', y: '70%', size: 70, delay: 1, duration: 22, opacity: 0.14 },
  { x: '50%', y: '85%', size: 45, delay: 3, duration: 16, opacity: 0.16 },
  { x: '90%', y: '80%', size: 55, delay: 5, duration: 19, opacity: 0.13 },
  { x: '5%', y: '45%', size: 65, delay: 2.5, duration: 21, opacity: 0.15 },
];

// Glowing orb configurations
const glowOrbConfigs = [
  { x: '20%', y: '30%', size: 300, color: 'var(--color-primary)', opacity: 0.08, blur: 100, duration: 8 },
  { x: '70%', y: '50%', size: 250, color: 'var(--color-accent)', opacity: 0.06, blur: 80, duration: 10 },
  { x: '40%', y: '80%', size: 200, color: 'var(--color-primary)', opacity: 0.05, blur: 60, duration: 12 },
];

/**
 * Stunning About page with claymorphism, animations, and visual effects
 */
export function AboutPage() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({ target: containerRef });
  const heroY = useTransform(scrollYProgress, [0, 1], [0, -100]);

  return (
    <div ref={containerRef} className="relative min-h-screen overflow-hidden">
      {/* Animated gradient mesh background */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        {/* Aurora gradient effect */}
        <motion.div
          className="absolute inset-0"
          style={{
            background: `
              radial-gradient(ellipse 80% 50% at 20% 40%, rgba(var(--color-primary-rgb, 220, 38, 38), 0.08) 0%, transparent 50%),
              radial-gradient(ellipse 60% 40% at 80% 60%, rgba(var(--color-accent-rgb, 234, 88, 12), 0.06) 0%, transparent 50%),
              radial-gradient(ellipse 50% 30% at 50% 90%, rgba(var(--color-primary-rgb, 220, 38, 38), 0.05) 0%, transparent 50%)
            `,
          }}
          animate={{
            opacity: [0.8, 1, 0.8],
          }}
          transition={{
            duration: 8,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />

        {/* Animated glowing orbs */}
        {glowOrbConfigs.map((orb, i) => (
          <motion.div
            key={i}
            className="absolute rounded-full"
            style={{
              left: orb.x,
              top: orb.y,
              width: orb.size,
              height: orb.size,
              background: orb.color,
              opacity: orb.opacity,
              filter: `blur(${orb.blur}px)`,
              transform: 'translate(-50%, -50%)',
            }}
            animate={{
              scale: [1, 1.2, 1],
              opacity: [orb.opacity, orb.opacity * 1.5, orb.opacity],
            }}
            transition={{
              duration: orb.duration,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        ))}

        {/* Floating molecules */}
        {moleculeConfigs.map((mol, i) => (
          <FloatingMolecule key={i} {...mol} />
        ))}
      </div>

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Hero Section with parallax */}
        <motion.div style={{ y: heroY }} className="mb-10">
          <HeroSection />
        </motion.div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
          {/* What is ChemAudit - Large card spanning 8 columns */}
          <AnimatedCard className="lg:col-span-8" delay={0.1}>
            <WhatIsChemAudit />
          </AnimatedCard>

          {/* Research Group - Tall card spanning 4 columns, 2 rows */}
          <AnimatedCard className="lg:col-span-4 lg:row-span-2" delay={0.2}>
            <ResearchGroup />
          </AnimatedCard>

          {/* Tech Stack - Wide card */}
          <AnimatedCard className="lg:col-span-8" delay={0.3}>
            <TechStack />
          </AnimatedCard>
        </div>

        {/* At a Glance Stats - Full width */}
        <AnimatedCard delay={0.35} className="mb-6">
          <QuickStats />
        </AnimatedCard>

        {/* Advanced Scoring Section - Full width */}
        <AnimatedCard delay={0.4} className="mb-6">
          <AdvancedScoring />
        </AnimatedCard>

        {/* Acknowledgments - Full width */}
        <AnimatedCard delay={0.5} className="mb-6">
          <Acknowledgments />
        </AnimatedCard>

        {/* Connect Section - Combined Source & Contact */}
        <AnimatedCard delay={0.6} className="mb-6">
          <ConnectSection />
        </AnimatedCard>

        {/* License Footer */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7, duration: 0.5 }}
          className={cn(
            'text-center py-8 px-6 rounded-2xl',
            'bg-gradient-to-r from-[var(--color-surface-sunken)]/50 via-transparent to-[var(--color-surface-sunken)]/50'
          )}
        >
          <div className="flex items-center justify-center gap-1.5 mb-2">
            <span className="text-sm font-medium text-[var(--color-text-secondary)]">
              Made with
            </span>
            <Coffee className="w-4 h-4 text-amber-600" />
            <span className="text-sm font-medium text-[var(--color-text-secondary)]">
              for the chemistry community
            </span>
          </div>
          <p className="text-xs text-[var(--color-text-muted)]">
            ChemAudit is open-source software released under the{' '}
            <a
              href="https://opensource.org/licenses/MIT"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[var(--color-primary)] hover:underline transition-colors"
            >
              MIT License
            </a>
          </p>
        </motion.div>
      </div>
    </div>
  );
}

// ============================================================================
// FLOATING MOLECULE COMPONENT
// ============================================================================

interface FloatingMoleculeProps {
  x: string;
  y: string;
  size: number;
  delay: number;
  duration: number;
  opacity: number;
}

function FloatingMolecule({ x, y, size, delay, duration, opacity }: FloatingMoleculeProps) {
  return (
    <motion.div
      className="absolute pointer-events-none"
      style={{ left: x, top: y }}
      initial={{ opacity: 0, scale: 0 }}
      animate={{
        opacity: [0, opacity, opacity, 0],
        scale: [0.8, 1, 1, 0.8],
        y: [0, -30, -60, -90],
        rotate: [0, 90, 180, 270],
      }}
      transition={{
        duration,
        delay,
        repeat: Infinity,
        ease: 'easeInOut',
      }}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 100 100"
        fill="none"
        className="text-[var(--color-primary)]"
      >
        {/* Benzene-like ring */}
        <circle cx="50" cy="50" r="25" stroke="currentColor" strokeWidth="2" opacity="0.5" />
        {/* Atoms */}
        <circle cx="50" cy="25" r="6" fill="currentColor" opacity="0.6" />
        <circle cx="71.65" cy="37.5" r="6" fill="currentColor" opacity="0.6" />
        <circle cx="71.65" cy="62.5" r="6" fill="currentColor" opacity="0.6" />
        <circle cx="50" cy="75" r="6" fill="currentColor" opacity="0.6" />
        <circle cx="28.35" cy="62.5" r="6" fill="currentColor" opacity="0.6" />
        <circle cx="28.35" cy="37.5" r="6" fill="currentColor" opacity="0.6" />
        {/* Bonds */}
        <line x1="50" y1="31" x2="50" y2="69" stroke="currentColor" strokeWidth="1.5" opacity="0.3" />
      </svg>
    </motion.div>
  );
}

// ============================================================================
// ANIMATED CARD WRAPPER
// ============================================================================

interface AnimatedCardProps {
  children: React.ReactNode;
  className?: string;
  delay?: number;
}

function AnimatedCard({ children, className, delay = 0 }: AnimatedCardProps) {
  const ref = useRef<HTMLDivElement>(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 40, scale: 0.95 }}
      animate={isInView ? { opacity: 1, y: 0, scale: 1 } : {}}
      transition={{
        duration: 0.6,
        delay,
        ease: [0.25, 0.46, 0.45, 0.94],
      }}
      className={className}
    >
      <ClayCard>{children}</ClayCard>
    </motion.div>
  );
}

// ============================================================================
// CLAYMORPHISM CARD
// ============================================================================

function ClayCard({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <motion.div
      className={cn(
        'relative h-full p-6 rounded-3xl overflow-hidden',
        // Claymorphism base
        'bg-[var(--color-surface-elevated)]',
        // Soft outer shadow for depth
        'shadow-[0_8px_32px_rgba(0,0,0,0.08),0_2px_8px_rgba(0,0,0,0.04)]',
        'dark:shadow-[0_8px_32px_rgba(0,0,0,0.3),0_2px_8px_rgba(0,0,0,0.2)]',
        // Inner highlight for 3D effect
        'before:absolute before:inset-0 before:rounded-3xl',
        'before:bg-gradient-to-br before:from-white/50 before:via-transparent before:to-transparent',
        'before:dark:from-white/10 before:dark:via-transparent before:dark:to-transparent',
        'before:pointer-events-none',
        // Border for definition
        'border border-[var(--color-border)]/50',
        // Hover effects
        'transition-all duration-300',
        'hover:shadow-[0_12px_48px_rgba(var(--color-primary-rgb,220,38,38),0.12),0_4px_16px_rgba(0,0,0,0.06)]',
        'hover:dark:shadow-[0_12px_48px_rgba(var(--color-primary-rgb,220,38,38),0.2),0_4px_16px_rgba(0,0,0,0.3)]',
        'hover:border-[var(--color-primary)]/30',
        'hover:-translate-y-1',
        className
      )}
      whileHover={{ scale: 1.01 }}
      transition={{ duration: 0.2 }}
    >
      <div className="relative z-10">{children}</div>
    </motion.div>
  );
}

// ============================================================================
// HERO SECTION
// ============================================================================

function HeroSection() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.8, ease: [0.25, 0.46, 0.45, 0.94] }}
      className="text-center"
    >
      {/* Logo with glow effect */}
      <motion.div
        className="relative inline-block mb-8"
        whileHover={{ scale: 1.05 }}
        transition={{ duration: 0.3 }}
      >
        {/* Glow behind logo */}
        <motion.div
          className="absolute inset-0 rounded-3xl blur-2xl"
          style={{
            background: 'linear-gradient(135deg, var(--color-primary), var(--color-accent))',
            opacity: 0.3,
          }}
          animate={{
            opacity: [0.2, 0.4, 0.2],
            scale: [1, 1.1, 1],
          }}
          transition={{
            duration: 4,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />
        <div className="relative w-28 h-28 rounded-3xl overflow-hidden shadow-2xl border-2 border-white/20">
          <img src="/logo.png" alt="ChemAudit Logo" className="w-full h-full object-contain" />
        </div>
      </motion.div>

      {/* Title with gradient */}
      <motion.h1
        className="text-5xl md:text-6xl font-bold mb-6 font-display"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2, duration: 0.6 }}
      >
        <span className="text-[var(--color-text-primary)]">
          About <span className="font-extrabold text-[var(--color-primary)]">Chem</span>Audit
        </span>
      </motion.h1>

      {/* Subtitle */}
      <motion.p
        className="text-lg md:text-xl text-[var(--color-text-secondary)] max-w-2xl mx-auto leading-relaxed"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3, duration: 0.6 }}
      >
        A comprehensive web-based chemical structure validation and quality assessment platform
        for cheminformatics workflows, drug discovery, and ML dataset curation.
      </motion.p>

      {/* Decorative line */}
      <motion.div
        className="mt-8 mx-auto w-24 h-1 rounded-full bg-gradient-to-r from-[var(--color-primary)] to-[var(--color-accent)]"
        initial={{ scaleX: 0 }}
        animate={{ scaleX: 1 }}
        transition={{ delay: 0.5, duration: 0.8, ease: 'easeOut' }}
      />
    </motion.div>
  );
}

// ============================================================================
// SECTION HEADER
// ============================================================================

function SectionHeader({ icon, title, isChemAudit }: { icon: React.ReactNode; title: string; isChemAudit?: boolean }) {
  return (
    <div className="flex items-center gap-3 mb-5">
      <motion.div
        className={cn(
          'p-2.5 rounded-2xl',
          'bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-accent)]/10',
          'text-[var(--color-primary)]',
          'shadow-inner'
        )}
        whileHover={{ rotate: [0, -10, 10, 0] }}
        transition={{ duration: 0.5 }}
      >
        {icon}
      </motion.div>
      <h2 className="text-xl font-bold text-[var(--color-text-primary)] font-display">
        {isChemAudit ? (
          <>What is <span className="font-extrabold text-[var(--color-primary)]">Chem</span>Audit?</>
        ) : (
          title
        )}
      </h2>
    </div>
  );
}

// ============================================================================
// WHAT IS CHEMAUDIT
// ============================================================================

function WhatIsChemAudit() {
  const features = [
    { icon: <Shield className="w-4 h-4" />, title: '15+ Validation Checks', desc: 'Comprehensive analysis' },
    { icon: <Zap className="w-4 h-4" />, title: 'ChEMBL Standardization', desc: 'Trusted pipeline' },
    { icon: <BarChart3 className="w-4 h-4" />, title: 'ML-Readiness Scoring', desc: 'Dataset quality' },
    { icon: <Pill className="w-4 h-4" />, title: 'Drug-Likeness Analysis', desc: 'Lipinski, QED & more' },
    { icon: <Activity className="w-4 h-4" />, title: 'ADMET Predictions', desc: 'Pharmacokinetics' },
    { icon: <Sparkles className="w-4 h-4" />, title: 'Batch Processing', desc: 'Millions of molecules' },
  ];

  return (
    <>
      <SectionHeader icon={<TestTube className="w-5 h-5" />} title="What is ChemAudit?" isChemAudit />
      <div className="space-y-4 text-[var(--color-text-secondary)]">
        <p className="leading-relaxed">
          ChemAudit is an open-source platform designed to validate, standardize, and assess
          the quality of chemical structures. It provides researchers and data scientists with
          powerful tools to ensure their molecular data is accurate and ready for downstream
          applications.
        </p>
        <p className="leading-relaxed">
          Whether you're preparing datasets for machine learning, curating compound libraries,
          or validating structures for publication, ChemAudit offers a comprehensive suite of
          validation checks, standardization pipelines, and quality scoring metrics.
        </p>

        {/* Feature grid with staggered animation */}
        <div className="grid grid-cols-2 md:grid-cols-3 gap-3 pt-4">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 + i * 0.1, duration: 0.4 }}
              className={cn(
                'p-4 rounded-2xl',
                'bg-[var(--color-surface-sunken)]',
                'border border-[var(--color-border)]/30',
                'hover:border-[var(--color-primary)]/30',
                'hover:bg-[var(--color-primary)]/5',
                'transition-all duration-300',
                'group'
              )}
            >
              <div className="flex items-center gap-2 mb-1">
                <span className="text-[var(--color-primary)] group-hover:scale-110 transition-transform">
                  {feature.icon}
                </span>
                <span className="font-semibold text-[var(--color-text-primary)] text-sm">
                  {feature.title}
                </span>
              </div>
              <p className="text-xs text-[var(--color-text-muted)]">{feature.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </>
  );
}

// ============================================================================
// RESEARCH GROUP
// ============================================================================

function ResearchGroup() {
  const mapUrl = 'https://www.google.com/maps/place/Lessingstra%C3%9Fe+8,+07743+Jena,+Germany';

  return (
    <div className="h-full flex flex-col">
      <SectionHeader icon={<Building2 className="w-5 h-5" />} title="Research Group" />

      {/* Logo */}
      <a
        href="http://cheminf.uni-jena.de/"
        target="_blank"
        rel="noopener noreferrer"
        className="block mb-4 group"
      >
        <div className="rounded-xl bg-white dark:bg-white/10 p-4 transition-shadow hover:shadow-md">
          <img
            src="/cheminf-logo.png"
            alt="Natural Products Cheminformatics"
            className="h-14 mx-auto object-contain"
          />
        </div>
      </a>

      {/* Title */}
      <h3 className="font-semibold text-[var(--color-text-primary)] text-center mb-3">
        Natural Products Cheminformatics
      </h3>

      {/* Description */}
      <p className="text-sm text-[var(--color-text-secondary)] mb-4">
        Research focus on chemical structure annotation, deep learning for chemical
        information mining, and development of open-source cheminformatics tools.
      </p>

      {/* Map - fills remaining space */}
      <a
        href={mapUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="relative block flex-1 min-h-[100px] rounded-xl overflow-hidden mb-4 border border-[var(--color-border)]/50 hover:border-[var(--color-primary)]/30 transition-colors"
      >
        <iframe
          src="https://www.openstreetmap.org/export/embed.html?bbox=11.5825%2C50.9245%2C11.5955%2C50.9305&layer=mapnik&marker=50.9275%2C11.589"
          className="absolute inset-0 w-full h-full border-0 pointer-events-none"
          title="Location Map"
        />
      </a>

      {/* Address & Links */}
      <div className="space-y-3 pt-4 border-t border-[var(--color-border)]/50">
        <div className="flex items-start gap-2 text-sm text-[var(--color-text-muted)]">
          <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0 text-[var(--color-primary)]" />
          <span>
            Friedrich Schiller University Jena<br />
            Lessingstr 8, 07743 Jena, Germany
          </span>
        </div>

        <div className="flex flex-col gap-2">
          <ExternalLinkButton href="http://cheminf.uni-jena.de/" icon={<Globe className="w-4 h-4" />}>
            Group Website
          </ExternalLinkButton>
          <ExternalLinkButton href="https://github.com/Steinbeck-Lab" icon={<Github className="w-4 h-4" />}>
            Steinbeck-Lab GitHub
          </ExternalLinkButton>
        </div>
      </div>
    </div>
  );
}

// ============================================================================
// TECH STACK
// ============================================================================

function TechStack() {
  const categories = [
    {
      icon: <Layout className="w-5 h-5" />,
      title: 'Frontend',
      items: ['React 18', 'TypeScript', 'Vite', 'Tailwind CSS', 'Framer Motion'],
      color: 'from-blue-500/20 to-cyan-500/10',
    },
    {
      icon: <Server className="w-5 h-5" />,
      title: 'Backend',
      items: ['Python 3.11+', 'FastAPI', 'Celery', 'Redis'],
      color: 'from-amber-500/20 to-yellow-500/10',
    },
    {
      icon: <TestTube className="w-5 h-5" />,
      title: 'Chemistry',
      items: ['RDKit', 'RDKit.js', 'MolVS', 'ChEMBL Pipeline'],
      color: 'from-purple-500/20 to-pink-500/10',
    },
    {
      icon: <Database className="w-5 h-5" />,
      title: 'Infrastructure',
      items: ['PostgreSQL', 'Docker', 'Nginx', 'Prometheus'],
      color: 'from-orange-500/20 to-amber-500/10',
    },
  ];

  return (
    <>
      <SectionHeader icon={<Code2 className="w-5 h-5" />} title="Technology Stack" />
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {categories.map((cat, i) => (
          <motion.div
            key={cat.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + i * 0.1, duration: 0.4 }}
            className={cn(
              'p-4 rounded-2xl',
              'bg-gradient-to-br',
              cat.color,
              'border border-[var(--color-border)]/20'
            )}
          >
            <div className="flex items-center gap-2 mb-3">
              <span className="text-[var(--color-primary)]">{cat.icon}</span>
              <span className="font-semibold text-sm text-[var(--color-text-primary)]">{cat.title}</span>
            </div>
            <ul className="space-y-1">
              {cat.items.map((item) => (
                <li key={item} className="text-xs text-[var(--color-text-muted)]">
                  {item}
                </li>
              ))}
            </ul>
          </motion.div>
        ))}
      </div>
    </>
  );
}

// ============================================================================
// CONNECT SECTION (Combined Source Code & Contact)
// ============================================================================

function ConnectSection() {
  const links = [
    {
      icon: <Github className="w-5 h-5" />,
      title: 'ChemAudit',
      description: 'Source Code on GitHub',
      href: 'https://github.com/Kohulan/ChemAudit',
      color: 'from-gray-600 to-gray-800',
      hoverColor: 'hover:border-gray-500/50',
    },
    {
      icon: <Github className="w-5 h-5" />,
      title: 'Steinbeck Lab',
      description: 'Organization GitHub',
      href: 'https://github.com/Steinbeck-Lab',
      color: 'from-purple-500 to-pink-500',
      hoverColor: 'hover:border-purple-500/50',
    },
    {
      icon: <Mail className="w-5 h-5" />,
      title: 'Email',
      description: 'kohulan.rajan@uni-jena.de',
      href: 'mailto:kohulan.rajan@uni-jena.de',
      color: 'from-blue-500 to-cyan-500',
      hoverColor: 'hover:border-blue-500/50',
    },
    {
      icon: <Globe className="w-5 h-5" />,
      title: 'Research Group',
      description: 'cheminf.uni-jena.de',
      href: 'http://cheminf.uni-jena.de/',
      color: 'from-emerald-500 to-teal-500',
      hoverColor: 'hover:border-emerald-500/50',
    },
  ];

  return (
    <>
      <SectionHeader icon={<Zap className="w-5 h-5" />} title="Connect With Us" />
      <p className="text-[var(--color-text-secondary)] mb-5">
        ChemAudit is open-source software. Contributions, bug reports, and feature requests are welcome!
      </p>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {links.map((link, i) => (
          <motion.a
            key={link.title}
            href={link.href}
            target={link.href.startsWith('mailto') ? undefined : '_blank'}
            rel={link.href.startsWith('mailto') ? undefined : 'noopener noreferrer'}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + i * 0.1, duration: 0.4 }}
            whileHover={{ scale: 1.03, y: -4 }}
            whileTap={{ scale: 0.98 }}
            className={cn(
              'relative overflow-hidden',
              'flex flex-col items-center text-center p-5 rounded-2xl',
              'bg-[var(--color-surface-sunken)]',
              'border border-[var(--color-border)]/30',
              link.hoverColor,
              'transition-all duration-300',
              'group'
            )}
          >
            {/* Background glow */}
            <div
              className={cn(
                'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300',
                'bg-gradient-to-br',
                link.color,
                'opacity-5'
              )}
            />

            {/* Icon */}
            <div
              className={cn(
                'relative w-12 h-12 rounded-2xl mb-3',
                'flex items-center justify-center',
                'bg-gradient-to-br',
                link.color,
                'text-white shadow-lg',
                'group-hover:scale-110 group-hover:shadow-xl transition-all duration-300'
              )}
            >
              {link.icon}
            </div>

            {/* Text */}
            <div className="relative">
              <div className="font-semibold text-sm text-[var(--color-text-primary)] mb-1">
                {link.title}
              </div>
              <div className="text-xs text-[var(--color-text-muted)] group-hover:text-[var(--color-text-secondary)] transition-colors">
                {link.description}
              </div>
            </div>

            {/* External link indicator */}
            <ExternalLink className="absolute top-3 right-3 w-3 h-3 text-[var(--color-text-muted)] opacity-0 group-hover:opacity-100 transition-opacity" />
          </motion.a>
        ))}
      </div>
    </>
  );
}

// ============================================================================
// QUICK STATS
// ============================================================================

function QuickStats() {
  const stats = [
    {
      label: 'Validation Checks',
      value: '15+',
      icon: <Shield className="w-4 h-4" />,
      color: 'from-blue-500 to-cyan-500',
      bgColor: 'from-blue-500/10 to-cyan-500/10',
    },
    {
      label: 'Scoring Modules',
      value: '6',
      icon: <FlaskConical className="w-4 h-4" />,
      color: 'from-purple-500 to-pink-500',
      bgColor: 'from-purple-500/10 to-pink-500/10',
    },
    {
      label: 'Safety Filters',
      value: '480+',
      icon: <ShieldCheck className="w-4 h-4" />,
      color: 'from-red-500 to-orange-500',
      bgColor: 'from-red-500/10 to-orange-500/10',
    },
    {
      label: 'Descriptors',
      value: '451',
      icon: <Brain className="w-4 h-4" />,
      color: 'from-emerald-500 to-teal-500',
      bgColor: 'from-emerald-500/10 to-teal-500/10',
    },
    {
      label: 'Export Formats',
      value: '5',
      icon: <Database className="w-4 h-4" />,
      color: 'from-amber-500 to-yellow-500',
      bgColor: 'from-amber-500/10 to-yellow-500/10',
    },
    {
      label: 'License',
      value: 'MIT',
      icon: <Heart className="w-4 h-4" />,
      color: 'from-rose-500 to-pink-500',
      bgColor: 'from-rose-500/10 to-pink-500/10',
    },
  ];

  return (
    <>
      <SectionHeader icon={<Sparkles className="w-5 h-5" />} title="At a Glance" />
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        {stats.map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{
              delay: 0.4 + i * 0.08,
              duration: 0.5,
              ease: [0.25, 0.46, 0.45, 0.94],
            }}
            whileHover={{ scale: 1.05, y: -4 }}
            className={cn(
              'relative overflow-hidden',
              'p-4 rounded-2xl',
              'bg-gradient-to-br',
              stat.bgColor,
              'border border-[var(--color-border)]/30',
              'hover:border-[var(--color-primary)]/40',
              'transition-colors duration-300',
              'group cursor-default'
            )}
          >
            {/* Decorative gradient orb */}
            <div
              className={cn(
                'absolute -top-6 -right-6 w-16 h-16 rounded-full blur-2xl opacity-40',
                'bg-gradient-to-br',
                stat.color,
                'group-hover:opacity-60 transition-opacity duration-300'
              )}
            />

            {/* Content */}
            <div className="relative">
              {/* Icon badge */}
              <div
                className={cn(
                  'inline-flex items-center justify-center',
                  'w-8 h-8 rounded-xl mb-3',
                  'bg-gradient-to-br',
                  stat.color,
                  'text-white shadow-lg',
                  'group-hover:scale-110 transition-transform duration-300'
                )}
              >
                {stat.icon}
              </div>

              {/* Value */}
              <div
                className={cn(
                  'text-3xl font-bold mb-1',
                  'bg-gradient-to-r bg-clip-text text-transparent',
                  stat.color
                )}
              >
                {stat.value}
              </div>

              {/* Label */}
              <div className="text-xs font-medium text-[var(--color-text-muted)] uppercase tracking-wide">
                {stat.label}
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </>
  );
}

// ============================================================================
// ADVANCED SCORING
// ============================================================================

function AdvancedScoring() {
  const scoringModules = [
    {
      icon: <Activity className="w-5 h-5" />,
      title: 'ADMET Predictions',
      description: 'Comprehensive pharmacokinetic profiling including synthetic accessibility, solubility (ESOL), CNS MPO score, molecular complexity, and bioavailability indicators.',
      features: ['Synthetic Accessibility', 'ESOL Solubility', 'CNS MPO Score', 'Pfizer 3/75 Rule', 'GSK 4/400 Rule', 'Golden Triangle'],
      color: 'from-emerald-500/20 to-teal-500/10',
    },
    {
      icon: <Pill className="w-5 h-5" />,
      title: 'Drug-Likeness',
      description: 'Multi-filter assessment using established pharmaceutical rules to predict oral bioavailability and drug-like properties.',
      features: ["Lipinski's Rule of Five", 'QED Score', 'Veber Rules', 'Rule of Three', 'Ghose Filter', 'Muegge Filter'],
      color: 'from-blue-500/20 to-indigo-500/10',
    },
    {
      icon: <Beaker className="w-5 h-5" />,
      title: 'Aggregator Likelihood',
      description: 'Predicts colloidal aggregation risk that causes false positives in high-throughput screening assays.',
      features: ['LogP Analysis', 'TPSA Assessment', 'Aromatic Stacking', 'Known Scaffolds', 'Size Analysis', 'Counter-screen Recommendations'],
      color: 'from-amber-500/20 to-orange-500/10',
    },
    {
      icon: <ShieldCheck className="w-5 h-5" />,
      title: 'Safety Filters',
      description: 'Structural alert screening using 480+ PAINS patterns and multiple ChEMBL sources to identify potentially problematic compounds.',
      features: ['PAINS (480 patterns)', 'Brenk Alerts', 'NIH Filters', 'ZINC Filters', 'ChEMBL Alerts (7 sources)', 'BMS/GSK/Dundee Rules'],
      color: 'from-red-500/20 to-rose-500/10',
    },
    {
      icon: <Brain className="w-5 h-5" />,
      title: 'ML-Readiness',
      description: 'Evaluates molecular suitability for machine learning with 451 descriptors and 7 fingerprint types for QSAR/QSPR models.',
      features: ['217 Standard Descriptors', 'AUTOCORR2D (192)', 'MQN (42)', '7 Fingerprint Types', 'Size Assessment', 'Dataset Quality Score'],
      color: 'from-purple-500/20 to-violet-500/10',
    },
    {
      icon: <Target className="w-5 h-5" />,
      title: 'NP-Likeness',
      description: 'Natural product-likeness scoring to assess similarity to natural product chemical space for drug discovery.',
      features: ['NP-likeness Score', 'Scaffold Analysis', 'Fragment Matching', 'Chemical Space', 'Lead-likeness', 'Bioactivity Potential'],
      color: 'from-lime-500/20 to-green-500/10',
    },
  ];

  return (
    <>
      <SectionHeader icon={<FlaskConical className="w-5 h-5" />} title="Advanced Molecular Scoring" />
      <p className="text-[var(--color-text-secondary)] mb-6">
        ChemAudit provides comprehensive molecular assessment through six specialized scoring modules,
        implementing industry-standard rules from Pfizer, GSK, Abbott, and academic research to evaluate
        compounds for drug discovery and ML applications.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {scoringModules.map((module, i) => (
          <motion.div
            key={module.title}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + i * 0.08, duration: 0.4 }}
            className={cn(
              'p-5 rounded-2xl',
              'bg-gradient-to-br',
              module.color,
              'border border-[var(--color-border)]/20',
              'hover:border-[var(--color-primary)]/30',
              'transition-all duration-300',
              'group'
            )}
          >
            <div className="flex items-center gap-3 mb-3">
              <div className={cn(
                'p-2 rounded-xl',
                'bg-[var(--color-surface-elevated)]',
                'text-[var(--color-primary)]',
                'group-hover:scale-110 transition-transform'
              )}>
                {module.icon}
              </div>
              <h3 className="font-semibold text-[var(--color-text-primary)]">{module.title}</h3>
            </div>
            <p className="text-sm text-[var(--color-text-secondary)] mb-3 leading-relaxed">
              {module.description}
            </p>
            <div className="flex flex-wrap gap-1.5">
              {module.features.map((feature) => (
                <span
                  key={feature}
                  className={cn(
                    'inline-flex items-center px-2 py-0.5 rounded-md text-xs',
                    'bg-[var(--color-surface-elevated)]/80',
                    'text-[var(--color-text-muted)]',
                    'border border-[var(--color-border)]/30'
                  )}
                >
                  {feature}
                </span>
              ))}
            </div>
          </motion.div>
        ))}
      </div>

      {/* Industrial Rules Highlight */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.8, duration: 0.4 }}
        className={cn(
          'mt-6 p-4 rounded-2xl',
          'bg-gradient-to-r from-[var(--color-primary)]/5 via-[var(--color-accent)]/5 to-[var(--color-primary)]/5',
          'border border-[var(--color-primary)]/10'
        )}
      >
        <div className="flex items-start gap-3">
          <div className="p-2 rounded-lg bg-[var(--color-primary)]/10">
            <AlertTriangle className="w-4 h-4 text-[var(--color-primary)]" />
          </div>
          <div>
            <h4 className="font-semibold text-sm text-[var(--color-text-primary)] mb-1">
              Industry-Standard Rules & Filters
            </h4>
            <p className="text-sm text-[var(--color-text-secondary)]">
              Implements validated pharmaceutical guidelines including Lipinski's Rule of Five,
              Pfizer's 3/75 and CNS MPO rules, GSK's 4/400 rule, Abbott's Golden Triangle,
              and comprehensive PAINS filtering with 480+ structural alert patterns from ChEMBL,
              NIH, and major pharmaceutical companies.
            </p>
          </div>
        </div>
      </motion.div>
    </>
  );
}

// ============================================================================
// ACKNOWLEDGMENTS
// ============================================================================

function Acknowledgments() {
  const acknowledgments = [
    {
      name: 'RDKit',
      description: 'Open-source cheminformatics toolkit powering molecular operations',
      href: 'https://www.rdkit.org/',
      logo: 'https://www.rdkit.org/Images/logo.png',
      color: 'from-blue-600 to-blue-800',
    },
    {
      name: 'ChEMBL',
      description: 'Bioactivity database for drug discovery from EMBL-EBI',
      href: 'https://www.ebi.ac.uk/chembl/',
      logo: 'https://cfde-gene-pages.cloud/logos/chEMBL_logo.png',
      color: 'from-teal-600 to-cyan-700',
    },
    {
      name: 'PubChem',
      description: 'World\'s largest collection of freely accessible chemical information',
      href: 'https://pubchem.ncbi.nlm.nih.gov/',
      logo: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/PubChem_logo.svg/1280px-PubChem_logo.svg.png',
      color: 'from-blue-500 to-indigo-600',
    },
    {
      name: 'COCONUT',
      description: 'Collection of Open Natural Products database',
      href: 'https://coconut.naturalproducts.net/',
      logo: 'https://raw.githubusercontent.com/Steinbeck-Lab/coconut/main/public/img/logo.svg',
      color: 'from-green-600 to-emerald-700',
    },
  ];

  const additionalThanks = [
    { name: 'FastAPI', href: 'https://fastapi.tiangolo.com/' },
    { name: 'React', href: 'https://react.dev/' },
    { name: 'Tailwind CSS', href: 'https://tailwindcss.com/' },
    { name: 'Framer Motion', href: 'https://www.framer.com/motion/' },
    { name: 'MolVS', href: 'https://github.com/mcs07/MolVS' },
    { name: 'Celery', href: 'https://docs.celeryq.dev/' },
  ];

  return (
    <>
      <SectionHeader icon={<Heart className="w-5 h-5 text-red-500" />} title="Acknowledgments" />
      <p className="text-[var(--color-text-secondary)] mb-6">
        ChemAudit is built upon the shoulders of giants. We gratefully acknowledge these
        amazing open-source projects and communities:
      </p>

      {/* Main acknowledgments with logos */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-5">
        {acknowledgments.map((ack, i) => (
          <motion.a
            key={ack.name}
            href={ack.href}
            target="_blank"
            rel="noopener noreferrer"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + i * 0.1, duration: 0.4 }}
            whileHover={{ scale: 1.03, y: -4 }}
            className={cn(
              'relative overflow-hidden',
              'flex flex-col p-4 rounded-2xl',
              'bg-[var(--color-surface-sunken)]',
              'border border-[var(--color-border)]/30',
              'hover:border-[var(--color-primary)]/40',
              'hover:shadow-lg hover:shadow-[var(--color-primary)]/10',
              'transition-all duration-300',
              'group'
            )}
          >
            {/* Logo container with gradient fallback */}
            <div
              className={cn(
                'relative w-full h-14 rounded-xl mb-3 flex items-center justify-center',
                'bg-white dark:bg-white/95',
                'overflow-hidden',
                'group-hover:shadow-md transition-shadow duration-300'
              )}
            >
              <img
                src={ack.logo}
                alt={`${ack.name} logo`}
                className="max-h-10 max-w-[85%] object-contain group-hover:scale-105 transition-transform duration-300"
                onError={(e) => {
                  // Fallback to styled gradient text
                  const target = e.target as HTMLImageElement;
                  const parent = target.parentElement as HTMLDivElement;
                  target.style.display = 'none';
                  parent.className = cn(
                    parent.className.replace('bg-white dark:bg-white/95', ''),
                    'bg-gradient-to-br',
                    ack.color
                  );
                  parent.innerHTML = `<span class="text-lg font-bold text-white drop-shadow-sm">${ack.name}</span>`;
                }}
              />
            </div>

            {/* Text content */}
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h4 className="font-semibold text-sm text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors">
                  {ack.name}
                </h4>
                <ExternalLink className="w-3 h-3 text-[var(--color-text-muted)] opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
              <p className="text-xs text-[var(--color-text-muted)] leading-relaxed">
                {ack.description}
              </p>
            </div>
          </motion.a>
        ))}
      </div>

      {/* Additional thanks */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.7, duration: 0.4 }}
        className={cn(
          'p-4 rounded-2xl',
          'bg-gradient-to-r from-[var(--color-primary)]/5 via-transparent to-[var(--color-accent)]/5',
          'border border-[var(--color-border)]/20'
        )}
      >
        <p className="text-sm text-[var(--color-text-secondary)] mb-3">
          Also powered by:
        </p>
        <div className="flex flex-wrap gap-2">
          {additionalThanks.map((item) => (
            <motion.a
              key={item.name}
              href={item.href}
              target="_blank"
              rel="noopener noreferrer"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className={cn(
                'inline-flex items-center gap-1 px-3 py-1.5 rounded-full',
                'bg-[var(--color-surface-elevated)]',
                'border border-[var(--color-border)]/30',
                'text-xs font-medium text-[var(--color-text-secondary)]',
                'hover:border-[var(--color-primary)]/40',
                'hover:text-[var(--color-primary)]',
                'transition-all duration-200'
              )}
            >
              {item.name}
              <ExternalLink className="w-2.5 h-2.5 opacity-50" />
            </motion.a>
          ))}
        </div>
      </motion.div>
    </>
  );
}

// ============================================================================
// EXTERNAL LINK BUTTON
// ============================================================================

interface ExternalLinkButtonProps {
  href: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}

function ExternalLinkButton({ href, icon, children }: ExternalLinkButtonProps) {
  return (
    <motion.a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={cn(
        'inline-flex items-center gap-2 text-sm',
        'text-[var(--color-primary)] hover:text-[var(--color-accent)]',
        'transition-colors'
      )}
      whileHover={{ x: 4 }}
    >
      {icon}
      {children}
      <ExternalLink className="w-3 h-3" />
    </motion.a>
  );
}

export default AboutPage;
