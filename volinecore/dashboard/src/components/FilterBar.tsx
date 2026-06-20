import type { LogLevelName, LogType } from '../types/log'

interface FilterBarProps {
  levels: LogLevelName[]
  types: LogType[]
  tags: string[]
  search: string
  allTags: string[]
  sessionId: string | null
  userId: string | null
  sessions: string[]
  users: string[]
  onLevelsChange: (levels: LogLevelName[]) => void
  onTypesChange: (types: LogType[]) => void
  onTagsChange: (tags: string[]) => void
  onSearchChange: (search: string) => void
  onSessionIdChange: (sessionId: string | null) => void
  onUserIdChange: (userId: string | null) => void
  onReset: () => void
}

const ALL_LEVELS: LogLevelName[] = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
const ALL_TYPES: LogType[] = ['debug', 'info', 'warning', 'error', 'critical', 'api', 'event', 'crash']

function ToggleChip({
  label,
  active,
  onClick,
  color,
}: {
  label: string
  active: boolean
  onClick: () => void
  color?: string
}) {
  return (
    <button
      onClick={onClick}
      className={`vld-px-2 vld-py-0.5 vld-rounded vld-text-xs vld-font-medium vld-border vld-transition-colors ${
        active
          ? `${color ?? 'vld-bg-blue-100 vld-text-blue-800 vld-border-blue-300'}`
          : 'vld-bg-white vld-text-gray-500 vld-border-gray-200 hover:vld-bg-gray-100'
      }`}
    >
      {label}
    </button>
  )
}

export function FilterBar({
  levels,
  types,
  tags,
  search,
  allTags,
  sessionId,
  userId,
  sessions,
  users,
  onLevelsChange,
  onTypesChange,
  onTagsChange,
  onSearchChange,
  onSessionIdChange,
  onUserIdChange,
  onReset,
}: FilterBarProps) {
  return (
    <div className="vld-space-y-2 vld-p-3 vld-bg-white vld-rounded-lg vld-shadow-sm vld-border vld-border-gray-200">
      {/* Row 1: Level + Type toggles */}
      <div className="vld-flex vld-flex-wrap vld-items-center vld-gap-2">
        <span className="vld-text-xs vld-font-semibold vld-text-gray-500 vld-w-10">
          Level:
        </span>
        {ALL_LEVELS.map((lvl) => (
          <ToggleChip
            key={lvl}
            label={lvl}
            active={levels.includes(lvl)}
            onClick={() => {
              const next = levels.includes(lvl)
                ? levels.filter((l) => l !== lvl)
                : [...levels, lvl]
              onLevelsChange(next.length === 0 ? ALL_LEVELS : next)
            }}
            color={
              lvl === 'DEBUG'
                ? 'vld-bg-gray-100 vld-text-gray-700 vld-border-gray-300'
                : lvl === 'INFO'
                  ? 'vld-bg-blue-100 vld-text-blue-700 vld-border-blue-300'
                  : lvl === 'WARNING'
                    ? 'vld-bg-amber-100 vld-text-amber-700 vld-border-amber-300'
                    : lvl === 'ERROR'
                      ? 'vld-bg-red-100 vld-text-red-700 vld-border-red-300'
                      : 'vld-bg-red-200 vld-text-red-800 vld-border-red-400'
            }
          />
        ))}

        <span className="vld-text-xs vld-font-semibold vld-text-gray-500 vld-ml-2 vld-w-10">
          Type:
        </span>
        {ALL_TYPES.map((t) => (
          <ToggleChip
            key={t}
            label={t}
            active={types.includes(t)}
            onClick={() => {
              const next = types.includes(t)
                ? types.filter((x) => x !== t)
                : [...types, t]
              onTypesChange(next.length === 0 ? ALL_TYPES : next)
            }}
          />
        ))}
      </div>

      {/* Row 2: Search + dropdowns */}
      <div className="vld-flex vld-flex-wrap vld-items-center vld-gap-2">
        <input
          type="text"
          placeholder="Search logs..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className="vld-flex-1 vld-min-w-[200px] vld-px-3 vld-py-1.5 vld-text-sm vld-border vld-border-gray-300 vld-rounded vld-outline-none focus:vld-ring-2 focus:vld-ring-blue-500 focus:vld-border-blue-500"
        />

        <select
          value={sessionId ?? ''}
          onChange={(e) => onSessionIdChange(e.target.value || null)}
          className="vld-px-2 vld-py-1.5 vld-text-sm vld-border vld-border-gray-300 vld-rounded vld-bg-white"
        >
          <option value="">All sessions</option>
          {sessions.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>

        <select
          value={userId ?? ''}
          onChange={(e) => onUserIdChange(e.target.value || null)}
          className="vld-px-2 vld-py-1.5 vld-text-sm vld-border vld-border-gray-300 vld-rounded vld-bg-white"
        >
          <option value="">All users</option>
          {users.map((u) => (
            <option key={u} value={u}>
              {u}
            </option>
          ))}
        </select>

        <button
          onClick={onReset}
          className="vld-px-3 vld-py-1.5 vld-text-sm vld-text-gray-600 hover:vld-text-gray-900 vld-border vld-border-gray-300 vld-rounded hover:vld-bg-gray-100"
        >
          Reset
        </button>
      </div>
    </div>
  )
}
