import { useEffect, useState, useCallback } from 'react'
import type { Database } from 'firebase/database'
import { useLogs } from './hooks/useLogs'
import { useFilters } from './hooks/useFilters'
import { LogStream } from './components/LogStream'
import { FilterBar } from './components/FilterBar'
import { Toolbar } from './components/Toolbar'
import { StatsPanel } from './components/StatsPanel'
import type { DashboardConfig } from './types/log'

interface AppProps {
  db: Database
  config: DashboardConfig
}

export function App({ db, config }: AppProps) {
  const databaseRef = config.databaseRef ?? 'logs'
  const {
    logs,
    isLoading,
    sessions,
    users,
    tags,
    isPaused,
    pause,
    resume,
    clear,
  } = useLogs(db, databaseRef, config.appId)

  const {
    filters,
    filteredLogs,
    setLevels,
    setTypes,
    setTags,
    setSearch,
    setSessionId,
    setUserId,
    resetFilters,
  } = useFilters(logs)

  return (
    <div className="vld-h-full vld-flex vld-flex-col vld-bg-gray-100">
      {/* Header */}
      <div className="vld-px-4 vld-py-3 vld-bg-white vld-border-b vld-border-gray-200 vld-shadow-sm">
        <div className="vld-flex vld-items-center vld-justify-between">
          <h1 className="vld-text-lg vld-font-bold vld-text-gray-800">
            Voline Log Dashboard
          </h1>
          <span className="vld-text-xs vld-text-gray-500 vld-font-mono">
            {config.appId}
          </span>
        </div>
      </div>

      {/* Toolbar */}
      <Toolbar
        logs={filteredLogs}
        filteredCount={filteredLogs.length}
        isPaused={isPaused}
        sessions={sessions}
        users={users}
        onPause={pause}
        onResume={resume}
        onClear={clear}
      />

      {/* Stats */}
      <StatsPanel logs={filteredLogs} />

      {/* Filter bar */}
      <div className="vld-px-3 vld-py-2">
        <FilterBar
          levels={filters.levels}
          types={filters.types}
          tags={filters.tags}
          search={filters.search}
          allTags={tags}
          sessionId={filters.sessionId}
          userId={filters.userId}
          sessions={sessions}
          users={users}
          onLevelsChange={setLevels}
          onTypesChange={setTypes}
          onTagsChange={setTags}
          onSearchChange={setSearch}
          onSessionIdChange={setSessionId}
          onUserIdChange={setUserId}
          onReset={resetFilters}
        />
      </div>

      {/* Log stream */}
      <div className="vld-flex-1 vld-px-3 vld-pb-3 vld-overflow-hidden">
        <LogStream logs={filteredLogs} isLoading={isLoading} />
      </div>
    </div>
  )
}
