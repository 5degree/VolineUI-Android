export function formatTimestamp(ts: string): string {
  try {
    const date = new Date(ts)
    return date.toLocaleString()
  } catch {
    return ts
  }
}

export function formatTimestampShort(ts: string): string {
  try {
    const date = new Date(ts)
    return date.toLocaleTimeString()
  } catch {
    return ts
  }
}

export function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`
}

export function getLevelColor(level: string): string {
  const map: Record<string, string> = {
    DEBUG: 'vld-bg-debug',
    INFO: 'vld-bg-info',
    WARNING: 'vld-bg-warning',
    ERROR: 'vld-bg-error',
    CRITICAL: 'vld-bg-critical',
  }
  return map[level] ?? 'vld-bg-gray-500'
}

export function getLevelTextColor(level: string): string {
  const map: Record<string, string> = {
    DEBUG: 'vld-text-debug',
    INFO: 'vld-text-info',
    WARNING: 'vld-text-warning',
    ERROR: 'vld-text-error',
    CRITICAL: 'vld-text-critical',
  }
  return map[level] ?? 'vld-text-gray-500'
}

export function getLevelBgClass(level: string): string {
  const map: Record<string, string> = {
    DEBUG: 'vld-bg-gray-100 vld-text-gray-700',
    INFO: 'vld-bg-blue-100 vld-text-blue-800',
    WARNING: 'vld-bg-amber-100 vld-text-amber-800',
    ERROR: 'vld-bg-red-100 vld-text-red-800',
    CRITICAL: 'vld-bg-red-200 vld-text-red-900',
  }
  return map[level] ?? 'vld-bg-gray-100 vld-text-gray-700'
}
