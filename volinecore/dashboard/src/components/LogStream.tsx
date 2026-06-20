import type { LogEntryData, LogLevelName, LogType } from '../types/log'
import { useRef, useCallback } from 'react'
import { LogEntry } from './LogEntry'

interface LogStreamProps {
  logs: LogEntryData[]
  isLoading: boolean
  highlightedId?: string
}

export function LogStream({ logs, isLoading, highlightedId }: LogStreamProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  if (isLoading) {
    return (
      <div className="vld-flex vld-items-center vld-justify-center vld-py-12 vld-text-gray-500">
        <svg
          className="vld-animate-spin vld-h-6 vld-w-6 vld-mr-2"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="vld-opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="vld-opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
        Connecting to Firebase...
      </div>
    )
  }

  if (logs.length === 0) {
    return (
      <div className="vld-flex vld-items-center vld-justify-center vld-py-12 vld-text-gray-400">
        <div className="vld-text-center">
          <svg
            className="vld-mx-auto vld-h-12 vld-w-12 vld-mb-3"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <p className="vld-text-sm">No logs yet. Waiting for data...</p>
        </div>
      </div>
    )
  }

  return (
    <div ref={containerRef} className="vld-flex-1 vld-overflow-y-auto vld-bg-white vld-rounded-lg vld-shadow-sm vld-border vld-border-gray-200">
      <div className="vld-sticky vld-top-0 vld-bg-gray-100 vld-px-4 vld-py-1.5 vld-text-xs vld-text-gray-500 vld-border-b vld-border-gray-200 vld-font-medium">
        {logs.length} log{logs.length !== 1 ? 's' : ''}
      </div>
      {logs.map((log) => (
        <LogEntry
          key={log.id}
          log={log}
          isHighlighted={log.id === highlightedId}
        />
      ))}
    </div>
  )
}
