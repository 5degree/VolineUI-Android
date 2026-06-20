import {
  ref,
  onChildAdded,
  onChildRemoved,
  off,
  limitToLast,
  query,
  Database,
  DataSnapshot,
} from 'firebase/database'
import type { LogEntryData } from '../types/log'

export type LogCallback = (log: LogEntryData) => void
export type RemoveCallback = (logId: string) => void

export function subscribeToLogs(
  db: Database,
  databaseRef: string,
  appId: string,
  onLog: LogCallback,
  onRemove?: RemoveCallback,
  maxEntries: number = 1000
): () => void {
  const logsRef = ref(db, `/${databaseRef}/${appId}`)
  const logsQuery = query(logsRef, limitToLast(maxEntries))

  const addedHandler = onChildAdded(logsQuery, (snapshot: DataSnapshot) => {
    const val = snapshot.val()
    if (val) {
      onLog(val as LogEntryData)
    }
  })

  let removeHandler: ReturnType<typeof onChildRemoved> | null = null
  if (onRemove) {
    removeHandler = onChildRemoved(logsQuery, (snapshot: DataSnapshot) => {
      if (snapshot.key) {
        onRemove(snapshot.key)
      }
    })
  }

  return () => {
    off(logsQuery, 'child_added', addedHandler)
    if (removeHandler) {
      off(logsQuery, 'child_removed', removeHandler)
    }
  }
}

export function subscribeToLogsByType(
  db: Database,
  databaseRef: string,
  appId: string,
  logType: string,
  onLog: LogCallback,
  maxEntries: number = 200
): () => void {
  const logsRef = ref(db, `/${databaseRef}/${appId}/${logType}`)
  const logsQuery = query(logsRef, limitToLast(maxEntries))

  const handler = onChildAdded(logsQuery, (snapshot: DataSnapshot) => {
    const val = snapshot.val()
    if (val) {
      onLog(val as LogEntryData)
    }
  })

  return () => {
    off(logsQuery, 'child_added', handler)
  }
}
