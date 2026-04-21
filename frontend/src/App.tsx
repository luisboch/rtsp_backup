import {useEffect, useState} from 'react'
import {StreamCard} from "./StreamCard";
import {DirectoryView} from "./DirectoryView";
import {StatusHeader} from "./StatusHeader";
import {StatusPayload, SystemStats, StreamInfo} from "./types";


export function App() {
    const [stats, setStats] = useState<SystemStats | null>(null)
    const [status, setStatus] = useState<StatusPayload | null>(null)
    const [streams, setStreams] = useState<StreamInfo[]>([])
    const [error, setError] = useState<string | null>(null)
    const [selectedDirectory, setSelectedDirectory] = useState<string | null>(null)

    useEffect(
        () => {
            const refreshStatus = () => {
                fetch('/api/status')
                    .then((response) => response.json())
                    .then((payload: StatusPayload) => setStatus(payload))
                    .catch(() => setError('Failed to load server status'))
            }

            refreshStatus()
            const statusInterval = setInterval(
                refreshStatus,
                5000
            )

            fetch('/api/streams')
                .then(res => res.json())
                .then(data => setStreams(data))
                .catch(() => setError('Failed to load streams'))

            const eventSource = new EventSource('/api/stats/sse')
            eventSource.addEventListener(
                'stats',
                (event) => {
                    const payload = JSON.parse((event as MessageEvent).data) as SystemStats
                    setStats(payload)
                }
            )

            eventSource.onerror = () => {
                setError('SSE connection interrupted')
                eventSource.close()
            }

            return () => {
                clearInterval(statusInterval)
                eventSource.close()
            }
        },
        []
    )

    return (
        <main className="container">
            <StatusHeader status={status} stats={stats}/>

            {selectedDirectory ? (
                <DirectoryView alias={selectedDirectory} onClose={() => setSelectedDirectory(null)}/>
            ) : (
                <div className="streams-grid">
                    {streams.map(stream => (
                        <StreamCard
                            stream={stream}
                            onShowDirectory={(alias) => setSelectedDirectory(alias)}
                        />
                    ))}
                </div>
            )}

            {error ? <p className="error" style={{marginTop: '2rem'}}>{error}</p> : null}
        </main>
    )
}
