interface ApiLogDetailProps {
  extras: Record<string, unknown>
}

function safeStringify(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

export function ApiLogDetail({ extras }: ApiLogDetailProps) {
  const request = extras.request as Record<string, unknown> | undefined
  const response = extras.response as Record<string, unknown> | undefined
  const durationMs = extras.duration_ms as number | undefined
  const error = extras.error as string | undefined

  if (!request) return null

  const method = request.method as string
  const url = request.url as string
  const statusCode = response?.status_code as number | undefined
  const statusMessage = response?.status_message as string | undefined

  return (
    <div className="vld-space-y-2">
      <div className="vld-text-xs vld-font-semibold vld-text-gray-500">
        API Call {durationMs != null && `(${durationMs}ms)`}
      </div>

      <div className="vld-grid vld-grid-cols-1 md:vld-grid-cols-2 vld-gap-2">
        <div className="vld-bg-blue-50 vld-rounded vld-p-2 vld-border vld-border-blue-200">
          <div className="vld-text-xs vld-font-semibold vld-text-blue-700 vld-mb-1">
            Request
          </div>
          <div className="vld-text-xs vld-font-mono">
            <span className="vld-font-semibold">{method}</span> {url}
          </div>
          {request.query_params ? (
            <div className="vld-mt-1 vld-text-xs">
              <span className="vld-font-semibold">Params:</span>{' '}
              {safeStringify(request.query_params)}
            </div>
          ) : null}
          {request.headers ? (
            <div className="vld-mt-1">
              <span className="vld-text-xs vld-font-semibold">Headers:</span>
              <pre className="vld-text-xs vld-mt-0.5 vld-overflow-x-auto">
                {safeStringify(request.headers)}
              </pre>
            </div>
          ) : null}
          {request.body ? (
            <div className="vld-mt-1">
              <span className="vld-text-xs vld-font-semibold">Body:</span>
              <pre className="vld-text-xs vld-mt-0.5 vld-bg-white vld-rounded vld-p-1 vld-overflow-x-auto vld-max-h-32">
                {formatJson(request.body as string)}
              </pre>
            </div>
          ) : null}
        </div>

        {response && (
          <div className="vld-bg-green-50 vld-rounded vld-p-2 vld-border vld-border-green-200">
            <div className="vld-text-xs vld-font-semibold vld-text-green-700 vld-mb-1">
              Response
            </div>
            <div className="vld-text-xs vld-font-mono">
              <span
                className={`vld-font-semibold ${
                  statusCode != null && statusCode >= 400
                    ? 'vld-text-red-600'
                    : 'vld-text-green-700'
                }`}
              >
                {statusCode ?? 'N/A'}
              </span>
              {statusMessage ? ` ${statusMessage}` : null}
            </div>
            {response.headers ? (
              <div className="vld-mt-1">
                <span className="vld-text-xs vld-font-semibold">Headers:</span>
                <pre className="vld-text-xs vld-mt-0.5 vld-overflow-x-auto">
                  {safeStringify(response.headers)}
                </pre>
              </div>
            ) : null}
            {response.body ? (
              <div className="vld-mt-1">
                <span className="vld-text-xs vld-font-semibold">Body:</span>
                <pre className="vld-text-xs vld-mt-0.5 vld-bg-white vld-rounded vld-p-1 vld-overflow-x-auto vld-max-h-32">
                  {formatJson(response.body as string)}
                </pre>
              </div>
            ) : null}
          </div>
        )}
      </div>

      {error && (
        <div className="vld-text-xs vld-text-red-600 vld-bg-red-50 vld-rounded vld-p-2 vld-border vld-border-red-200">
          <span className="vld-font-semibold">Error:</span> {error}
        </div>
      )}
    </div>
  )
}

function formatJson(str: string): string {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}
