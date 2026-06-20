import type { LogLevelName } from '../types/log'
import { getLevelBgClass } from '../utils'

interface LevelBadgeProps {
  level: LogLevelName
}

export function LevelBadge({ level }: LevelBadgeProps) {
  return (
    <span
      className={`vld-inline-flex vld-items-center vld-px-2 vld-py-0.5 vld-rounded vld-text-xs vld-font-semibold vld-uppercase vld-tracking-wider ${getLevelBgClass(level)}`}
    >
      {level}
    </span>
  )
}
