interface CrashDetailProps {
  extras: Record<string, unknown>
}

export function CrashDetail({ extras }: CrashDetailProps) {
  const exceptionClass = extras.exception_class as string | undefined
  const exceptionMessage = extras.exception_message as string | undefined
  const stackTrace = extras.stack_trace as string | undefined
  const cause = extras.cause as string | undefined
  const threadName = extras.thread_name as string | undefined
  const isMainThread = extras.is_main_thread as boolean | undefined
  const isFatal = extras.is_fatal as boolean | undefined

  return (
    <div className="vld-space-y-2 vld-bg-red-50 vld-rounded vld-p-3 vld-border vld-border-red-200">
      <div className="vld-flex vld-items-center vld-gap-2">
        <span className="vld-text-xs vld-font-bold vld-text-red-800 vld-uppercase">
          {isFatal ? 'Fatal Crash' : 'Exception'}
        </span>
        {threadName && (
          <span className="vld-text-xs vld-text-red-600">
            Thread: {threadName}{' '}
            {isMainThread !== undefined && (isMainThread ? '(main)' : '(background)')}
          </span>
        )}
      </div>

      {exceptionClass && (
        <div className="vld-text-sm vld-font-semibold vld-text-red-900">
          {exceptionClass}
        </div>
      )}

      {exceptionMessage && (
        <div className="vld-text-sm vld-text-red-700 vld-font-mono vld-bg-white vld-rounded vld-p-2 vld-border vld-border-red-100">
          {exceptionMessage}
        </div>
      )}

      {cause && (
        <div className="vld-text-xs vld-text-red-600">
          <span className="vld-font-semibold">Caused by:</span> {cause}
        </div>
      )}

      {stackTrace && (
        <div>
          <span className="vld-text-xs vld-font-semibold vld-text-red-700">
            Stack Trace:
          </span>
          <pre className="vld-mt-1 vld-text-xs vld-bg-white vld-rounded vld-p-2 vld-overflow-x-auto vld-max-h-60 vld-font-mono vld-leading-relaxed vld-border vld-border-red-100">
            {stackTrace}
          </pre>
        </div>
      )}
    </div>
  )
}
