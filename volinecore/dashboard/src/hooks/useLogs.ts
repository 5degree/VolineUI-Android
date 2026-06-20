import { useState, useEffect, useCallback, useRef } from 'react'
import type { Database } from 'firebase/database'
import { subscribeToLogs } from '../firebase/realtime'
import type { LogEntryData } from '../types/log'

interface UseLogsOptions {
  limit?: number
}

interface UseLogsReturn {
  logs: LogEntryData[]
  isLoading: boolean
  error: Error | null
  sessions: string[]
  users: string[]
  tags: string[]
  isPaused: boolean
  pause: () => void
  resume: () => void
  clear: () => void
}

export function useLogs(
  db: Database | null,
  databaseRef: string,
  appId: string,
  options: UseLogsOptions = {}
): UseLogsReturn {
  const { limit = 1000 } = options
  const [logs, setLogs] = useState<LogEntryData[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)
  const [isPaused, setIsPaused] = useState(false)

  const bufferRef = useRef<LogEntryData[]>([])
  const unsubscribeRef = useRef<(() => void) | null>(null)
  const pauseBufferRef = useRef<LogEntryData[]>([])
  const isPausedRef = useRef(false)

  const flushBuffer = useCallback(() => {
    if (bufferRef.current.length === 0) return
    setLogs((prev) => {
      const combined = [...bufferRef.current, ...prev]
      bufferRef.current = []
      return combined.slice(0, limit)
    })
  }, [limit])

  useEffect(() => {
    if (!db) return

    setIsLoading(true)
    setError(null)

    const unsubscribe = subscribeToLogs(
      db,
      databaseRef,
      appId,
      (log) => {
        if (isPausedRef.current) {
          pauseBufferRef.current.unshift(log)
          if (pauseBufferRef.current.length > limit) {
            pauseBufferRef.current = pauseBufferRef.current.slice(0, limit)
          }
        } else {
          bufferRef.current.unshift(log)
        }
      },
      (logId) => {
        setLogs((prev) => prev.filter((l) => l.id !== logId))
      },
      limit
    )

    unsubscribeRef.current = unsubscribe

    // Flush buffer every 300ms for batched rendering
    const interval = setInterval(flushBuffer, 300)

    setIsLoading(false)

    return () => {
      unsubscribe()
      clearInterval(interval)
      unsubscribeRef.current = null
    }
  }, [db, databaseRef, appId, limit, flushBuffer])

  const pause = useCallback(() => {
    isPausedRef.current = true
    setIsPaused(true)
  }, [])

  const resume = useCallback(() => {
    isPausedRef.current = false
    // Flush buffered logs from pause period
    if (pauseBufferRef.current.length > 0) {
      setLogs((prev) => {
        const combined = [...pauseBufferRef.current, ...prev]
        pauseBufferRef.current = []
        return combined.slice(0, limit)
      })
    }
    setIsPaused(false)
  }, [limit])

  const clear = useCallback(() => {
    setLogs([])
    bufferRef.current = []
    pauseBufferRef.current = []
  }, [])

  // Derive distinct values
  const sessions = [...new Set(logs.map((l) => l.sessionId).filter(Boolean))]
  const users = [...new Set(logs.map((l) => l.userId).filter(Boolean) as string[])]
  const tags = [...new Set(logs.map((l) => l.tag).filter(Boolean))]

  return {
    logs,
    isLoading,
    error,
    sessions,
    users,
    tags,
    isPaused,
    pause,
    resume,
    clear,
  }
}
