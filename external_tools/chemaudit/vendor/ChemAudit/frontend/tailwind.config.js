/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // CSS variable-based colors for seamless theme switching
        surface: 'var(--color-surface)',
        'surface-elevated': 'var(--color-surface-elevated)',
        'surface-sunken': 'var(--color-surface-sunken)',
        'surface-overlay': 'var(--color-surface-overlay)',
        'text-primary': 'var(--color-text-primary)',
        'text-secondary': 'var(--color-text-secondary)',
        'text-muted': 'var(--color-text-muted)',

        // Primary palette (Laboratory Crimson)
        'chem-primary': {
          DEFAULT: 'var(--color-primary)',
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#c41e3a',
          700: '#9d1830',
          800: '#7f1d1d',
          900: '#450a0a',
          950: '#2a0505',
        },

        // Accent palette (Warm Amber)
        'chem-accent': {
          DEFAULT: 'var(--color-accent)',
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
          950: '#451a03',
        },

        // Secondary palette (Warm Rose)
        'chem-secondary': {
          DEFAULT: 'var(--color-secondary)',
          50: '#fff1f2',
          100: '#ffe4e6',
          200: '#fecdd3',
          300: '#fda4af',
          400: '#fb7185',
          500: '#f43f5e',
          600: '#e11d48',
          700: '#be123c',
          800: '#9f1239',
          900: '#881337',
          950: '#4c0519',
        },

        // Neutral palette (Warm Stone)
        'chem-dark': {
          DEFAULT: '#1a1815',
          50: '#fafaf9',
          100: '#f5f5f4',
          200: '#e7e5e4',
          300: '#d6d3d1',
          400: '#a8a29e',
          500: '#78716c',
          600: '#57534e',
          700: '#44403c',
          800: '#292524',
          900: '#1c1917',
          950: '#0c0a09',
        },

        // Status colors (warm palette)
        'status-success': {
          DEFAULT: '#fbbf24',
          light: '#fef3c7',
          dark: '#d97706',
        },
        'status-warning': {
          DEFAULT: '#f59e0b',
          light: '#fef3c7',
          dark: '#b45309',
        },
        'status-error': {
          DEFAULT: '#ef4444',
          light: '#fee2e2',
          dark: '#dc2626',
        },
        'status-info': {
          DEFAULT: 'var(--color-primary)',
          light: '#fee2e2',
          dark: '#c41e3a',
        },

        // Score colors (gold/bronze warm hierarchy)
        'score-excellent': '#fbbf24',
        'score-good': '#d97706',
        'score-fair': '#ea580c',
        'score-poor': '#dc2626',
      },

      fontFamily: {
        sans: ['Source Sans 3', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
        display: ['Outfit', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'monospace'],
      },

      fontSize: {
        '2xs': ['0.6875rem', { lineHeight: '1rem' }],
      },

      boxShadow: {
        'glow-sm': '0 0 15px var(--glow-primary)',
        'glow': '0 0 30px var(--glow-primary)',
        'glow-lg': '0 0 50px var(--glow-primary)',
        'glow-accent': '0 0 30px var(--glow-accent)',
        'inner-glow': 'inset 0 1px 0 rgba(255,255,255,0.1)',
      },

      borderRadius: {
        'xl': '1rem',
        '2xl': '1.25rem',
        '3xl': '1.75rem',
        '4xl': '2rem',
      },

      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-primary': 'var(--gradient-primary)',
        'gradient-accent': 'var(--gradient-accent)',
        'gradient-mesh': 'var(--gradient-mesh)',
        'gradient-hero': 'var(--gradient-hero)',
      },

      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'fade-in-up': 'fadeInUp 0.6s ease-out',
        'slide-up': 'slideUp 0.5s ease-out',
        'slide-down': 'slideDown 0.5s ease-out',
        'scale-in': 'scaleIn 0.4s ease-out',
        'glow-pulse': 'glowPulse 2.5s ease-in-out infinite',
        'float': 'float 5s ease-in-out infinite',
        'orbit': 'orbit 3s linear infinite',
        'orbit-reverse': 'orbit 3s linear infinite reverse',
        'shimmer': 'shimmer 2s linear infinite',
        'gradient': 'gradient-shift 3s ease infinite',
      },

      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.94)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        glowPulse: {
          '0%, 100%': { boxShadow: '0 0 20px var(--glow-primary)' },
          '50%': { boxShadow: '0 0 50px var(--glow-primary)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        orbit: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        'gradient-shift': {
          '0%': { backgroundPosition: '0% 50%' },
          '50%': { backgroundPosition: '100% 50%' },
          '100%': { backgroundPosition: '0% 50%' },
        },
      },

      gridTemplateColumns: {
        'bento': 'repeat(4, minmax(0, 1fr))',
        'bento-sm': 'repeat(2, minmax(0, 1fr))',
      },

      backdropBlur: {
        'xs': '2px',
      },

      transitionTimingFunction: {
        'bounce-in': 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
        'smooth': 'cubic-bezier(0.4, 0, 0.2, 1)',
      },

      spacing: {
        '18': '4.5rem',
        '22': '5.5rem',
      },
    },
  },
  plugins: [],
}
