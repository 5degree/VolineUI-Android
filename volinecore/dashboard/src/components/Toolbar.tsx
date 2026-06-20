import { useMemo, useCallback } from 'react'
import type { LogEntryData } from '../types/log'

interface ToolbarProps {
  logs: LogEntryData[]
  filteredCount: number
  isPaused: boolean
  sessions: string[]
  users: string[]
  onPause: () => void
  onResume: () => void
  onClear: () => void
}

export function Toolbar({
  logs,
  filteredCount,
  isPaused,
  onPause,
  onResume,
  onClear,
}: ToolbarProps) {
  const exportAsJson = useCallback(() => {
    const blob = new Blob([JSON.stringify(logs, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `voline-logs-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }, [logs])

  return (
    <div className="vld-flex vld-items-center vld-justify-between vld-px-4 vld-py-2 vld-bg-white vld-border-b vld-border-gray-200">
      <div className="vld-flex vld-items-center vld-gap-4 vld-text-sm vld-text-gray-600">
        <span>
          <strong className="vld-text-gray-800">{filteredCount}</strong>{' '}
          filtered / <strong className="vld-text-gray-800">{logs.length}</strong> total
        </span>
      </div>

      <div className="vld-flex vld-items-center vld-gap-2">
        <button
          onClick={isPaused ? onResume : onPause}
          className={`vld-px-3 vld-py-1.5 vld-text-sm vld-rounded vld-font-medium vld-transition-colors ${
            isPaused
              ? 'vld-bg-green-100 vld-text-green-700 hover:vld-bg-green-200 vld-border vld-border-green-300'
              : 'vld-bg-gray-100 vld-text-gray-600 hover:vld-bg-gray-200 vld-border vld-border-gray-300'
          }`}
        >
          {isPaused ? (
            <span className="vld-flex vld-items-center vld-gap-1">
              <svg className="vld-w-3.5 vld-h-3.5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
              Resume
            </span>
          ) : (
            <span className="vld-flex vld-items-center vld-gap-1">
              <svg className="vld-w-3.5 vld-h-3.5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </svg>
              Pause
            </span>
          )}
        </button>

        <button
          onClick={exportAsJson}
          className="vld-px-3 vld-py-1.5 vld-text-sm vld-rounded vld-font-medium vld-bg-gray-100 vld-text-gray-600 hover:vld-bg-gray-200 vld-border vld-border-gray-300 vld-transition-colors"
        >
          Export JSON
        </button>

        <button
          onClick={onClear}
          className="vld-px-3 vld-py-1.5 vld-text-sm vld-rounded vld-font-medium vld-text-red-600 hover:vld-bg-red-50 vld-border vld-border-red-200 vld-transition-colors"
        >
          Clear
        </button>
      </div>
    </div>
  )
}
