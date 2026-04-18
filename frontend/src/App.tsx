import { useEffect, useState, useMemo } from 'react'
import { 
  EyeOff, 
  Clock, 
  Activity, 
  Layers, 
  CheckCircle2, 
  AlertCircle,
  FolderOpen,
  X
} from 'lucide-react'

type SystemStats = {
  cpuLoadPercent: number
  memoryUsedBytes: number
  memoryTotalBytes: number
  diskUsedBytes: number
  diskTotalBytes: number
  timestamp: number
}

type StatusPayload = {
  recording: boolean
  streamsConfigured: number
  ffmpegAvailable: boolean
  timestamp: number
}

type StreamInfo = {
  alias: string
  url: string
  directory: string
}

function StatusHeader({ status, stats }: { status: StatusPayload | null, stats: SystemStats | null }) {
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  const staleness = status ? (now - status.timestamp) / 1000 : Infinity
  
  const clockColor = useMemo(() => {
    if (staleness < 10) return '#22c55e' // green
    if (staleness < 30) return '#f97316' // orange
    return '#ef4444' // red
  }, [staleness])

  return (
    <header>
      <h1 style={{ margin: 0, fontSize: '1.5rem' }}>RTSP Backup</h1>
      <div className="status-group">
        <div className="status-item">
          <Activity size={18} color={status?.recording ? '#22c55e' : '#94a3b8'} />
          <span>Recording: {status?.recording ? 'Active' : 'Idle'}</span>
        </div>
        <div className="status-item">
          <Layers size={18} color="#3b82f6" />
          <span>Streams: {status?.streamsConfigured ?? 0}</span>
        </div>
        <div className="status-item">
          {status?.ffmpegAvailable ? (
            <CheckCircle2 size={18} color="#22c55e" />
          ) : (
            <AlertCircle size={18} color="#ef4444" />
          )}
          <span>FFmpeg</span>
        </div>
        <div className="status-item">
          <Clock size={18} color={clockColor} />
          <span>{status ? new Date(status.timestamp).toLocaleTimeString() : '--:--:--'}</span>
        </div>
      </div>
    </header>
  )
}

function StreamCard({ stream, onShowDirectory }: { stream: StreamInfo, onShowDirectory: (alias: string) => void }) {
  const [isPlaying, setIsPlaying] = useState(false)

  return (
    <div className="card">
      <h3 className="stream-name">{stream.alias}</h3>
      <div className="video-container" onClick={() => setIsPlaying(!isPlaying)}>
        {isPlaying ? (
          <img 
            src={`/api/proxy/${stream.alias}?t=${Date.now()}`} 
            alt={stream.alias} 
            onError={() => setIsPlaying(false)}
          />
        ) : (
          <EyeOff size={48} color="#475569" />
        )}
      </div>
      <div className="card-actions">
        <button onClick={() => onShowDirectory(stream.alias)}>
          <FolderOpen size={16} style={{ marginRight: '0.5rem', verticalAlign: 'middle' }} />
          Show Directory
        </button>
      </div>
    </div>
  )
}

function DirectoryView({ alias, onClose }: { alias: string, onClose: () => void }) {
  const [files, setFiles] = useState<string[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch(`/api/files/${alias}`)
      .then(res => res.json())
      .then(data => {
        setFiles(data)
        setLoading(false)
      })
  }, [alias])

  return (
    <div className="directory-view card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>Files for {alias}</h2>
        <button onClick={onClose} style={{ background: '#334155' }}>
          <X size={20} />
        </button>
      </div>
      {loading ? (
        <p>Loading files...</p>
      ) : (
        <ul className="file-list">
          {files.map(file => (
            <li key={file} className="file-item">
              <span>{file.split('/').pop()}</span>
              <a href={`/api/video/${file}`} target="_blank" rel="noreferrer">
                Open in new page
              </a>
            </li>
          ))}
          {files.length === 0 && <p>No files found.</p>}
        </ul>
      )}
    </div>
  )
}

export function App() {
  const [stats, setStats] = useState<SystemStats | null>(null)
  const [status, setStatus] = useState<StatusPayload | null>(null)
  const [streams, setStreams] = useState<StreamInfo[]>([])
  const [error, setError] = useState<string | null>(null)
  const [selectedDirectory, setSelectedDirectory] = useState<string | null>(null)

  useEffect(() => {
    const refreshStatus = () => {
      fetch('/api/status')
        .then((response) => response.json())
        .then((payload: StatusPayload) => setStatus(payload))
        .catch(() => setError('Failed to load server status'))
    }

    refreshStatus()
    const statusInterval = setInterval(refreshStatus, 5000)

    fetch('/api/streams')
      .then(res => res.json())
      .then(data => setStreams(data))
      .catch(() => setError('Failed to load streams'))

    const eventSource = new EventSource('/api/stats/sse')
    eventSource.addEventListener('stats', (event) => {
      const payload = JSON.parse((event as MessageEvent).data) as SystemStats
      setStats(payload)
    })

    eventSource.onerror = () => {
      setError('SSE connection interrupted')
      eventSource.close()
    }

    return () => {
      clearInterval(statusInterval)
      eventSource.close()
    }
  }, [])

  return (
    <main className="container">
      <StatusHeader status={status} stats={stats} />
      
      {selectedDirectory ? (
        <DirectoryView alias={selectedDirectory} onClose={() => setSelectedDirectory(null)} />
      ) : (
        <div className="streams-grid">
          {streams.map(stream => (
            <StreamCard 
              key={stream.alias} 
              stream={stream} 
              onShowDirectory={(alias) => setSelectedDirectory(alias)} 
            />
          ))}
        </div>
      )}

      {error ? <p className="error" style={{ marginTop: '2rem' }}>{error}</p> : null}
    </main>
  )
}
