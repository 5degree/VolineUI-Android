import { useMemo } from 'react'
import { App } from '../App'
import { initFirebase } from '../firebase/config'
import type { DashboardConfig } from '../types/log'

export interface VolineLogDashboardProps {
  firebaseConfig: DashboardConfig['firebaseConfig']
  appId: string
  databaseRef?: string
  className?: string
  style?: React.CSSProperties
}

export function VolineLogDashboard({
  firebaseConfig,
  appId,
  databaseRef,
  className,
  style,
}: VolineLogDashboardProps) {
  const config = useMemo<DashboardConfig>(
    () => ({ firebaseConfig, appId, databaseRef }),
    [firebaseConfig, appId, databaseRef]
  )

  const db = useMemo(() => initFirebase(config), [config])

  return (
    <div className={className} style={style}>
      <App db={db} config={config} />
    </div>
  )
}
