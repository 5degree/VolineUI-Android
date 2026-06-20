import { useState } from 'react'
import clsx from 'clsx'
import type { LogEntryData } from '../types/log'
import { LevelBadge } from './LevelBadge'
import { ApiLogDetail } from './ApiLogDetail'
import { CrashDetail } from './CrashDetail'
import { formatTimestamp, formatTimestampShort } from '../utils'

interface LogEntryProps {
  log: LogEntryData
  isHighlighted?: boolean
}

export function LogEntry({ log, isHighlighted }: LogEntryProps) {
  const [expanded, setExpanded] = useState(false)
  const [showDeviceInfo, setShowDeviceInfo] = useState(false)

  const isApi = log.logType === 'api'
  const isCrash = log.logType === 'crash'

  return (
    <div
      className={clsx(
        'vld-border-b vld-border-gray-200 vld-transition-colors vld-cursor-pointer',
        expanded ? 'vld-bg-gray-50' : 'vld-bg-white hover:vld-bg-gray-50',
        isHighlighted && 'vld-bg-yellow-50'
      )}
      onClick={() => setExpanded(!expanded)}
    >
      {/* Collapsed row */}
      <div className="vld-flex vld-items-center vld-gap-3 vld-px-4 vld-py-2 vld-text-sm">
        <LevelBadge level={log.logLevel} />
        <span className="vld-text-gray-500 vld-font-mono vld-text-xs vld-whitespace-nowrap vld-w-20 vld-shrink-0">
          {formatTimestampShort(log.timestamp)}
        </span>
        <span className="vld-font-medium vld-text-gray-700 vld-w-24 vld-shrink-0 vld-truncate">
          {log.logType === 'api' ? '🌐' : log.logType === 'crash' ? '💥' : ''}{' '}
          {log.tag}
        </span>
        <span className="vld-text-gray-600 vld-flex-1 vld-truncate">
          {log.message}
        </span>
        {log.userId && (
          <span className="vld-text-xs vld-text-gray-400 vld-ml-auto vld-shrink-0">
            {log.userId}
          </span>
        )}
        <svg
          className={clsx(
            'vld-w-4 vld-h-4 vld-text-gray-400 vld-shrink-0 vld-transition-transform',
            expanded && 'vld-rotate-180'
          )}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {/* Expanded content */}
      {expanded && (
        <div className="vld-px-4 vld-pb-3 vld-pl-12 vld-space-y-3">
          <div className="vld-text-xs vld-text-gray-500 vld-font-mono">
            <span className="vld-font-semibold">ID:</span> {log.id}
            <span className="vld-ml-4 vld-font-semibold">Session:</span>{' '}
            {log.sessionId}
            <span className="vld-ml-4 vld-font-semibold">Time:</span>{' '}
            {formatTimestamp(log.timestamp)}
          </div>

          {log.extras && Object.keys(log.extras).length > 0 && !isApi && !isCrash && (
            <div>
              <span className="vld-text-xs vld-font-semibold vld-text-gray-500">
                Extras:
              </span>
              <pre className="vld-mt-1 vld-text-xs vld-bg-gray-100 vld-rounded vld-p-2 vld-overflow-x-auto vld-max-h-40">
                {JSON.stringify(log.extras, null, 2)}
              </pre>
            </div>
          )}

          {isApi && log.extras && <ApiLogDetail extras={log.extras} />}
          {isCrash && log.extras && <CrashDetail extras={log.extras} />}

          {/* Device info toggle */}
          <button
            onClick={(e) => {
              e.stopPropagation()
              setShowDeviceInfo(!showDeviceInfo)
            }}
            className="vld-text-xs vld-text-blue-600 hover:vld-text-blue-800 vld-underline"
          >
            {showDeviceInfo ? 'Hide' : 'Show'} device info
          </button>
          {showDeviceInfo && (
            <div className="vld-text-xs vld-bg-gray-100 vld-rounded vld-p-2 vld-grid vld-grid-cols-2 sm:vld-grid-cols-3 vld-gap-x-4 vld-gap-y-1">
              <div>
                <span className="vld-font-semibold">Device:</span>{' '}
                {log.deviceInfo.manufacturer} {log.deviceInfo.model}
              </div>
              <div>
                <span className="vld-font-semibold">OS:</span>{' '}
                {log.deviceInfo.osVersion} (SDK {log.deviceInfo.sdkVersion})
              </div>
              <div>
                <span className="vld-font-semibold">App:</span>{' '}
                {log.deviceInfo.appVersionName} (
                {log.deviceInfo.appVersionCode})
              </div>
              <div>
                <span className="vld-font-semibold">Network:</span>{' '}
                {log.deviceInfo.networkType}
              </div>
              <div>
                <span className="vld-font-semibold">Screen:</span>{' '}
                {log.deviceInfo.screenResolution} ({log.deviceInfo.screenDensity})
              </div>
              <div>
                <span className="vld-font-semibold">Locale:</span>{' '}
                {log.deviceInfo.locale}
              </div>
              <div>
                <span className="vld-font-semibold">Timezone:</span>{' '}
                {log.deviceInfo.timezone}
              </div>
              <div>
                <span className="vld-font-semibold">Emulator:</span>{' '}
                {log.deviceInfo.isEmulator ? 'Yes' : 'No'}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
