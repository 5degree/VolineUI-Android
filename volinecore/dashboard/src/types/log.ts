export interface DeviceInfo {
  manufacturer: string
  model: string
  brand: string
  osVersion: string
  sdkVersion: number
  deviceId: string
  appVersionName: string
  appVersionCode: number
  screenDensity: string
  screenResolution: string
  locale: string
  timezone: string
  networkType: string
  isEmulator: boolean
}

export type LogLevelName = 'DEBUG' | 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'

export type LogType =
  | 'debug'
  | 'info'
  | 'warning'
  | 'error'
  | 'critical'
  | 'api'
  | 'event'
  | 'crash'

export interface LogEntryData {
  id: string
  timestamp: string
  timestampMillis: number
  logLevel: LogLevelName
  logType: LogType
  tag: string
  message: string
  extras?: Record<string, unknown>
  deviceInfo: DeviceInfo
  sessionId: string
  appId: string
  userId?: string
  customIdentifiers: Record<string, string>
}

export interface LogFilters {
  levels: LogLevelName[]
  types: LogType[]
  tags: string[]
  search: string
  fromDate: number | null
  toDate: number | null
  sessionId: string | null
  userId: string | null
}

export interface DashboardConfig {
  firebaseConfig: {
    apiKey: string
    authDomain: string
    databaseURL: string
    projectId: string
    storageBucket?: string
    messagingSenderId?: string
    appId: string
  }
  appId: string
  databaseRef?: string
}
