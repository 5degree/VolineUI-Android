import { useMemo } from 'react'
import type { LogEntryData, LogLevelName } from '../types/log'

interface StatsPanelProps {
  logs: LogEntryData[]
}

const LEVELS: LogLevelName[] = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']

const LEVEL_STYLES: Record<string, string> = {
  DEBUG: 'vld-bg-gray-100 vld-text-gray-700 vld-border-gray-200',
  INFO: 'vld-bg-blue-100 vld-text-blue-700 vld-border-blue-200',
  WARNING: 'vld-bg-amber-100 vld-text-amber-700 vld-border-amber-200',
  ERROR: 'vld-bg-red-100 vld-text-red-700 vld-border-red-200',
  CRITICAL: 'vld-bg-red-200 vld-text-red-800 vld-border-red-300',
}

export function StatsPanel({ logs }: StatsPanelProps) {
  const counts = useMemo(() => {
    const map: Record<string, number> = {}
    for (const level of LEVELS) {
      map[level] = 0
    }
    for (const log of logs) {
      map[log.logLevel] = (map[log.logLevel] ?? 0) + 1
    }
    return map
  }, [logs])

  const total = logs.length

  return (
    <div className="vld-flex vld-items-center vld-gap-2 vld-px-4 vld-py-2 vld-bg-gray-50 vld-border-b vld-border-gray-200">
      {LEVELS.map((level) => (
        <div
          key={level}
          className={`vld-px-3 vld-py-1 vld-rounded-full vld-text-xs vld-font-medium vld-border ${LEVEL_STYLES[level]}`}
        >
          {level}: {counts[level]}
        </div>
      ))}
      <div className="vld-ml-auto vld-text-xs vld-text-gray-500 vld-font-medium">
        Total: {total}
      </div>
    </div>
  )
}
