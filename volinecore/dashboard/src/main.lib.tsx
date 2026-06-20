import { createRoot } from 'react-dom/client'
import { createElement } from 'react'
import { App } from './App'
import { initFirebase } from './firebase/config'
import type { DashboardConfig } from './types/log'

export { VolineLogDashboard } from './components/VolineLogDashboard'
export type { VolineLogDashboardProps } from './components/VolineLogDashboard'

/**
 * Mount the dashboard into a DOM element.
 * Used in standalone HTML mode (UMD / script tag).
 */
export function mount(selector: string, config: DashboardConfig) {
  const el = document.querySelector(selector)
  if (!el) throw new Error(`VolineLogDashboard: Element "${selector}" not found`)

  const db = initFirebase(config)
  createRoot(el).render(createElement(App, { db, config }))
}
