import { useState, useCallback, useMemo } from 'react'
import type { LogEntryData, LogFilters, LogLevelName, LogType } from '../types/log'

const ALL_LEVELS: LogLevelName[] = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
const ALL_TYPES: LogType[] = ['debug', 'info', 'warning', 'error', 'critical', 'api', 'event', 'crash']

interface UseFiltersReturn {
  filters: LogFilters
  filteredLogs: LogEntryData[]
  setLevels: (levels: LogLevelName[]) => void
  setTypes: (types: LogType[]) => void
  setTags: (tags: string[]) => void
  setSearch: (search: string) => void
  setDateRange: (from: number | null, to: number | null) => void
  setSessionId: (sessionId: string | null) => void
  setUserId: (userId: string | null) => void
  resetFilters: () => void
}

export function useFilters(logs: LogEntryData[]): UseFiltersReturn {
  const [filters, setFilters] = useState<LogFilters>({
    levels: ALL_LEVELS,
    types: ALL_TYPES,
    tags: [],
    search: '',
    fromDate: null,
    toDate: null,
    sessionId: null,
    userId: null,
  })

  const setLevels = useCallback((levels: LogLevelName[]) => {
    setFilters((prev) => ({ ...prev, levels }))
  }, [])

  const setTypes = useCallback((types: LogType[]) => {
    setFilters((prev) => ({ ...prev, types }))
  }, [])

  const setTags = useCallback((tags: string[]) => {
    setFilters((prev) => ({ ...prev, tags }))
  }, [])

  const setSearch = useCallback((search: string) => {
    setFilters((prev) => ({ ...prev, search }))
  }, [])

  const setDateRange = useCallback((fromDate: number | null, toDate: number | null) => {
    setFilters((prev) => ({ ...prev, fromDate, toDate }))
  }, [])

  const setSessionId = useCallback((sessionId: string | null) => {
    setFilters((prev) => ({ ...prev, sessionId }))
  }, [])

  const setUserId = useCallback((userId: string | null) => {
    setFilters((prev) => ({ ...prev, userId }))
  }, [])

  const resetFilters = useCallback(() => {
    setFilters({
      levels: ALL_LEVELS,
      types: ALL_TYPES,
      tags: [],
      search: '',
      fromDate: null,
      toDate: null,
      sessionId: null,
      userId: null,
    })
  }, [])

  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      if (!filters.levels.includes(log.logLevel)) return false
      if (!filters.types.includes(log.logType)) return false
      if (filters.tags.length > 0 && !filters.tags.includes(log.tag)) return false
      if (filters.sessionId && log.sessionId !== filters.sessionId) return false
      if (filters.userId && log.userId !== filters.userId) return false
      if (filters.fromDate && log.timestampMillis < filters.fromDate) return false
      if (filters.toDate && log.timestampMillis > filters.toDate) return false
      if (filters.search) {
        const q = filters.search.toLowerCase()
        const matchesMessage = log.message.toLowerCase().includes(q)
        const matchesTag = log.tag.toLowerCase().includes(q)
        const matchesExtras = log.extras
          ? JSON.stringify(log.extras).toLowerCase().includes(q)
          : false
        if (!matchesMessage && !matchesTag && !matchesExtras) return false
      }
      return true
    })
  }, [logs, filters])

  return {
    filters,
    filteredLogs,
    setLevels,
    setTypes,
    setTags,
    setSearch,
    setDateRange,
    setSessionId,
    setUserId,
    resetFilters,
  }
}
