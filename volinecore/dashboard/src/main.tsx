import './index.css'
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { initFirebase } from './firebase/config'
import type { DashboardConfig } from './types/log'

function getConfigFromGlobal(): DashboardConfig | null {
  const win = window as unknown as { __VOLINE_DASHBOARD_CONFIG__?: DashboardConfig }
  return win.__VOLINE_DASHBOARD_CONFIG__ ?? null
}

function ConfigForm({ onConnect }: { onConnect: (config: DashboardConfig) => void }) {
  const [apiKey, setApiKey] = useState('')
  const [databaseURL, setDatabaseURL] = useState('')
  const [projectId, setProjectId] = useState('')
  const [appId, setAppId] = useState('')
  const [dbRef, setDbRef] = useState('logs')
  const [error, setError] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!apiKey || !databaseURL || !projectId || !appId) {
      setError('All fields are required')
      return
    }
    setError('')
    onConnect({
      firebaseConfig: {
        apiKey,
        authDomain: `${projectId}.firebaseapp.com`,
        databaseURL,
        projectId,
        appId: projectId,
      },
      appId,
      databaseRef: dbRef,
    })
  }

  return (
    <div className="vld-flex vld-items-center vld-justify-center vld-min-h-screen vld-bg-gray-100 vld-p-4">
      <form onSubmit={handleSubmit} className="vld-bg-white vld-rounded-lg vld-shadow-lg vld-p-8 vld-max-w-md vld-w-full vld-space-y-4">
        <h1 className="vld-text-xl vld-font-bold vld-text-gray-800">Voline Log Dashboard</h1>
        <p className="vld-text-sm vld-text-gray-500">Enter your Firebase config to connect.</p>
        {error && <p className="vld-text-sm vld-text-red-600">{error}</p>}
        <div>
          <label className="vld-block vld-text-sm vld-font-medium vld-text-gray-700">API Key</label>
          <input value={apiKey} onChange={e => setApiKey(e.target.value)} className="vld-w-full vld-px-3 vld-py-2 vld-border vld-border-gray-300 vld-rounded vld-text-sm focus:vld-ring-2 focus:vld-ring-blue-500" />
        </div>
        <div>
          <label className="vld-block vld-text-sm vld-font-medium vld-text-gray-700">Database URL</label>
          <input value={databaseURL} onChange={e => setDatabaseURL(e.target.value)} placeholder="https://my-project-default-rtdb.firebaseio.com" className="vld-w-full vld-px-3 vld-py-2 vld-border vld-border-gray-300 vld-rounded vld-text-sm focus:vld-ring-2 focus:vld-ring-blue-500" />
        </div>
        <div>
          <label className="vld-block vld-text-sm vld-font-medium vld-text-gray-700">Project ID</label>
          <input value={projectId} onChange={e => setProjectId(e.target.value)} className="vld-w-full vld-px-3 vld-py-2 vld-border vld-border-gray-300 vld-rounded vld-text-sm focus:vld-ring-2 focus:vld-ring-blue-500" />
        </div>
        <div>
          <label className="vld-block vld-text-sm vld-font-medium vld-text-gray-700">App ID (from LoggerConfig)</label>
          <input value={appId} onChange={e => setAppId(e.target.value)} className="vld-w-full vld-px-3 vld-py-2 vld-border vld-border-gray-300 vld-rounded vld-text-sm focus:vld-ring-2 focus:vld-ring-blue-500" />
        </div>
        <div>
          <label className="vld-block vld-text-sm vld-font-medium vld-text-gray-700">Database Reference (optional)</label>
          <input value={dbRef} onChange={e => setDbRef(e.target.value)} className="vld-w-full vld-px-3 vld-py-2 vld-border vld-border-gray-300 vld-rounded vld-text-sm focus:vld-ring-2 focus:vld-ring-blue-500" />
        </div>
        <button type="submit" className="vld-w-full vld-py-2 vld-bg-blue-600 vld-text-white vld-rounded vld-font-medium hover:vld-bg-blue-700 vld-transition-colors">
          Connect
        </button>
      </form>
    </div>
  )
}

function AppWithConfig() {
  const [config, setConfig] = useState<DashboardConfig | null>(() => getConfigFromGlobal())

  if (!config) {
    return <ConfigForm onConnect={setConfig} />
  }

  try {
    const db = initFirebase(config)
    return <App db={db} config={config} />
  } catch (err) {
    return (
      <div className="vld-p-8 vld-text-center">
        <p className="vld-text-red-600">Failed to initialize Firebase: {String(err)}</p>
        <button onClick={() => setConfig(null)} className="vld-mt-4 vld-px-4 vld-py-2 vld-bg-blue-600 vld-text-white vld-rounded">
          Try again
        </button>
      </div>
    )
  }
}

const root = document.getElementById('root')
if (root) {
  createRoot(root).render(<AppWithConfig />)
}
