/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Swiggy Core Palette
        brand: {
          50:  '#FFF3E8',
          100: '#FFE4C9',
          200: '#FFCFA3',
          300: '#FFB06D',
          400: '#FC8019', // ← THE Swiggy Orange
          500: '#E06D00',
          600: '#C45E00',
          700: '#9A4900',
          800: '#6F3500',
          900: '#452100',
        },
        surface: {
          primary:   '#0F0F14', // Deepest background
          secondary: '#1C1C2E', // Card/panel background  
          tertiary:  '#262640', // Elevated elements
          hover:     '#2E2E4A', // Hover states
          border:    '#3A3A5C', // Subtle borders
          glow:      '#FC801915', // Orange ambient glow
        },
        text: {
          primary:   '#F5F5F7',
          secondary: '#A0A0B8',
          muted:     '#6B6B82',
          inverse:   '#0F0F14',
        },
        status: {
          success: '#34D399',
          warning: '#FBBF24',
          error:   '#F87171',
          info:    '#60A5FA',
        },
        priority: {
          critical: '#EF4444',
          high:     '#F97316',
          medium:   '#EAB308',
          low:      '#22C55E',
        },
      },
      fontFamily: {
        sans: [
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'sans-serif',
        ],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '0.875rem' }],
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      boxShadow: {
        'glow-sm':  '0 0 15px rgba(252, 128, 25, 0.08)',
        'glow':     '0 0 30px rgba(252, 128, 25, 0.12)',
        'glow-lg':  '0 0 60px rgba(252, 128, 25, 0.18)',
        'card':     '0 2px 12px rgba(0, 0, 0, 0.4)',
        'card-hover': '0 8px 32px rgba(0, 0, 0, 0.5)',
        'float':    '0 20px 60px rgba(0, 0, 0, 0.6)',
      },
      animation: {
        'fade-in':    'fadeIn 0.3s ease-out',
        'slide-up':   'slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-down': 'slideDown 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
        'scale-in':   'scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
        'shimmer':    'shimmer 2s infinite linear',
        'pulse-dot':  'pulseDot 2s infinite ease-in-out',
        'bounce-in':  'bounceIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      keyframes: {
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%':   { opacity: '0', transform: 'translateY(-8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%':   { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%':   { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        pulseDot: {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%':      { opacity: '0.5', transform: 'scale(1.5)' },
        },
        bounceIn: {
          '0%':   { opacity: '0', transform: 'scale(0.3)' },
          '50%':  { transform: 'scale(1.05)' },
          '70%':  { transform: 'scale(0.9)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-brand':  'linear-gradient(135deg, #FC8019 0%, #E06D00 100%)',
        'shimmer-gradient': 'linear-gradient(90deg, transparent 0%, rgba(252,128,25,0.04) 50%, transparent 100%)',
      },
    },
  },
  plugins: [],
};
