import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import dts from 'vite-plugin-dts'
import { resolve } from 'path'

const LIB_NAME = 'VolineLogDashboard'

export default defineConfig(({ mode }) => {
  const isLib = mode === 'lib'

  return {
    plugins: [
      react(),
      ...(isLib ? [dts({ rollupTypes: true })] : []),
    ],
    define: {
      __LIB_MODE__: JSON.stringify(isLib),
    },
    build: isLib
      ? {
          lib: {
            entry: resolve(__dirname, 'src/main.lib.tsx'),
            name: LIB_NAME,
            formats: ['es', 'umd'],
            fileName: (format) =>
              `voline-log-dashboard.${format}.${format === 'umd' ? 'cjs' : 'js'}`,
          },
          rollupOptions: {
            external: [], // bundle everything for zero-config script-tag usage
            output: {
              globals: {},
              assetFileNames: (assetInfo) => {
                if (assetInfo.name === 'style.css') return 'voline-log-dashboard.css'
                return assetInfo.name ?? 'assets/[name]-[hash][extname]'
              },
            },
          },
          cssCodeSplit: false,
          sourcemap: true,
        }
      : {
          outDir: 'dist',
          sourcemap: true,
        },
  }
})
