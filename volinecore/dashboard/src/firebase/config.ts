import { initializeApp, FirebaseApp } from 'firebase/app'
import { getDatabase, Database } from 'firebase/database'
import type { DashboardConfig } from '../types/log'

let app: FirebaseApp | null = null
let db: Database | null = null

export function initFirebase(config: DashboardConfig): Database {
  if (db) return db

  app = initializeApp(config.firebaseConfig)
  db = getDatabase(app)
  return db
}

export function getDb(): Database {
  if (!db) {
    throw new Error(
      'Firebase not initialized. Call initFirebase(config) first.'
    )
  }
  return db
}
