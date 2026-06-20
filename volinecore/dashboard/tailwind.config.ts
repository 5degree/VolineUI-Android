import type { Config } from 'tailwindcss'

export default {
  prefix: 'vld-',
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        debug: '#6b7280',
        info: '#3b82f6',
        warning: '#f59e0b',
        error: '#ef4444',
        critical: '#dc2626',
      },
    },
  },
  plugins: [],
} satisfies Config
