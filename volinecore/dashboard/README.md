# Voline Log Dashboard

Real-time log viewer for [VolineLogger](https://github.com/yourusername/voline-android) — the Android remote logging SDK. Connects directly to your Firebase Realtime Database and streams logs in real time.

## Two Ways to Use

### 1. React Component (npm)

```bash
npm install @voline/log-dashboard
```

```tsx
import { VolineLogDashboard } from '@voline/log-dashboard'

function AdminPage() {
  return (
    <VolineLogDashboard
      firebaseConfig={{
        apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
        authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
        databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL,
        projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
      }}
      appId="com.mycompany.myapp"
      databaseRef="logs"
    />
  )
}
```

### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `firebaseConfig` | `FirebaseConfig` | Yes | — | Standard Firebase JS SDK config object |
| `appId` | `string` | Yes | — | Matches `LoggerConfig.appId` in your Android app |
| `databaseRef` | `string` | No | `"logs"` | Matches `LoggerConfig.databaseReference` |
| `className` | `string` | No | — | Additional CSS class for the wrapper |
| `style` | `React.CSSProperties` | No | — | Inline styles for the wrapper |

### 2. Standalone HTML (UMD / Script Tag)

Create an HTML file anywhere:

```html
<!DOCTYPE html>
<html>
<head>
  <title>Voline Logs</title>
  <script src="https://cdn.jsdelivr.net/gh/yourusername/voline-android@v1.0.0/dashboard/dist/voline-log-dashboard.umd.cjs"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/yourusername/voline-android@v1.0.0/dashboard/dist/style.css">
</head>
<body>
  <div id="app" style="height: 100vh;"></div>
  <script>
    VolineLogDashboard.mount('#app', {
      firebaseConfig: {
        apiKey: "AIza...",
        authDomain: "my-app.firebaseapp.com",
        databaseURL: "https://my-app-default-rtdb.firebaseio.com",
        projectId: "my-app"
      },
      appId: "com.mycompany.myapp",
      databaseRef: "logs"
    });
  </script>
</body>
</html>
```

> **Note:** After deploying to GitHub Pages, replace the URL with your own:
> `https://<username>.github.io/<repo>/dashboard/voline-log-dashboard.umd.cjs`

---

## Features

- **Real-time streaming** — logs appear as they're written to Firebase RTDB
- **Level filtering** — toggle DEBUG / INFO / WARNING / ERROR / CRITICAL
- **Type filtering** — toggle debug / info / api / event / crash
- **Full-text search** — searches message, tag, and extras
- **Session & user filtering** — pick from observed sessions/users
- **Pause/Resume** — freeze the live feed to inspect a log
- **Export JSON** — download filtered logs
- **API log detail** — side-by-side request/response with formatted JSON bodies
- **Crash detail** — stack trace viewer with device info
- **Device info panel** — expandable on any log entry
- **Stats bar** — live counts per log level

---

## Firebase Security Rules

Your Firebase RTDB must allow read access for the dashboard. Example rules:

```json
{
  "rules": {
    "logs": {
      "$appId": {
        ".read": "auth != null",
        ".write": false
      }
    }
  }
}
```

For simpler setups (development only):

```json
{
  "rules": {
    "logs": {
      "$appId": {
        ".read": true,
        ".write": false
      }
    }
  }
}
```

---

## Development

```bash
cd volinecore/dashboard

# Install
npm install

# Dev server (SPA mode with config form)
npm run dev

# Build SPA (for GitHub Pages)
npm run build

# Build library (for npm publishing)
npm run build:lib
```

### Project Structure

```
volinecore/dashboard/
├── src/
│   ├── main.tsx                      # SPA entry (config form + dashboard)
│   ├── main.lib.tsx                  # Library entry (exports component + mount)
│   ├── index.ts                      # Public API exports
│   ├── App.tsx                       # Root dashboard component
│   ├── index.css                     # Tailwind imports
│   ├── firebase/
│   │   ├── config.ts                 # Firebase initialization
│   │   └── realtime.ts               # RTDB listeners
│   ├── hooks/
│   │   ├── useLogs.ts                # Real-time log subscription
│   │   ├── useFilters.ts             # Filter state + logic
│   ├── components/
│   │   ├── VolineLogDashboard.tsx     # Main component for npm
│   │   ├── LogStream.tsx             # Virtualized log list
│   │   ├── LogEntry.tsx              # Expandable log row
│   │   ├── ApiLogDetail.tsx          # API request/response viewer
│   │   ├── CrashDetail.tsx           # Stack trace viewer
│   │   ├── FilterBar.tsx             # Filter controls
│   │   ├── Toolbar.tsx               # Pause, export, clear
│   │   ├── StatsPanel.tsx            # Level count summary
│   │   └── LevelBadge.tsx            # Colored level pill
│   └── types/
│       └── log.ts                    # TypeScript interfaces
├── package.json
├── vite.config.ts
├── tailwind.config.ts
├── postcss.config.js
├── tsconfig.json
└── index.html
```

---

## Deployment

### GitHub Pages

Push to `main` — the included GitHub Action automatically deploys to GitHub Pages.

### npm

Create a GitHub Release with a tag matching `dashboard-v*` — the npm publish action runs automatically.

> Requires `NPM_TOKEN` secret in your repository settings.
