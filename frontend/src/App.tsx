import {useEffect, useState, useCallback} from 'react'
import {StreamCard} from "./StreamCard";
import {DirectoryView} from "./DirectoryView";
import {StatusHeader} from "./StatusHeader";
import {Login} from "./Login";
import {StatusPayload, SystemStats, StreamInfo, NewStream} from "./types";
import {AddStreamModal} from "./AddStreamModal";
import {Plus} from "lucide-react";


export function App() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)
    const [isCheckingAuth, setIsCheckingAuth] = useState<boolean>(true)
    const [stats, setStats] = useState<SystemStats | null>(null)
    const [status, setStatus] = useState<StatusPayload | null>(null)
    const [streams, setStreams] = useState<StreamInfo[]>([])
    const [error, setError] = useState<string | null>(null)
    const [selectedDirectory, setSelectedDirectory] = useState<string | null>(null)
    const [isAddModalOpen, setIsAddModalOpen] = useState(false)

    const handleLogout = useCallback(async () => {
        try {
            await fetch('/api/auth/logout', { method: 'POST' });
        } finally {
            setIsAuthenticated(false);
            setStats(null);
            setStatus(null);
            setStreams([]);
            setIsAddModalOpen(false);
        }
    }, []);

    const handleAddStream = async (newStream: NewStream) => {
        const response = await fetch('/api/streams', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(newStream),
        });

        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.message || 'Failed to add stream');
        }

        const addedStream = await response.json();
        setStreams(prev => [...prev, addedStream]);
    };

    const handleDeleteStream = async (stream: StreamInfo) => {
        if (!window.confirm('Are you sure you want to remove this stream?')) return;

        try {
            const response = await fetch(`/api/streams/${stream.id}`, {
                method: 'DELETE',
            });

            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.message || 'Failed to delete stream');
            }

            setStreams(prev => prev.filter(s => s.id !== stream.id));
        } catch (err: any) {
            setError(err.message);
        }
    };

    useEffect(() => {
        fetch('/api/status')
            .then(res => {
                if (res.ok) {
                    setIsAuthenticated(true);
                }
            })
            .finally(() => {
                setIsCheckingAuth(false);
            });
    }, []);

    useEffect(
        () => {
            if (!isAuthenticated) return;

            const refreshStatus = () => {
                fetch('/api/status')
                    .then((response) => {
                        if (response.status === 401) {
                            setIsAuthenticated(false);
                            throw new Error('Unauthorized');
                        }
                        return response.json()
                    })
                    .then((payload: StatusPayload) => setStatus(payload))
                    .catch((err) => {
                        if (err.message !== 'Unauthorized') {
                            setError('Failed to load server status')
                        }
                    })
            }

            refreshStatus()
            const statusInterval = setInterval(
                refreshStatus,
                5000
            )

            fetch('/api/streams')
                .then(res => {
                    if (res.status === 401) {
                        setIsAuthenticated(false);
                        throw new Error('Unauthorized');
                    }
                    return res.json()
                })
                .then(data => setStreams(data))
                .catch((err) => {
                    if (err.message !== 'Unauthorized') {
                        setError('Failed to load streams')
                    }
                })

            const eventSource = new EventSource('/api/stats/sse')
            eventSource.addEventListener(
                'stats',
                (event) => {
                    const payload = JSON.parse((event as MessageEvent).data) as SystemStats
                    setStats(payload)
                }
            )

            eventSource.onerror = (e) => {
                console.error('SSE Error', e);
                setError('SSE connection interrupted')
                eventSource.close()
            }

            return () => {
                clearInterval(statusInterval)
                eventSource.close()
            }
        },
        [isAuthenticated]
    )

    if (isCheckingAuth) {
        return <div className="loading">Checking authentication...</div>;
    }

    if (!isAuthenticated) {
        return <Login onLoginSuccess={() => setIsAuthenticated(true)} />;
    }

    return (
        <main className="container">
            <StatusHeader status={status} stats={stats} onLogout={handleLogout}/>

            {selectedDirectory ? (
                <DirectoryView alias={selectedDirectory} onClose={() => setSelectedDirectory(null)}/>
            ) : (
                <>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
                        <button onClick={() => setIsAddModalOpen(true)} className="add-stream-btn">
                            <Plus size={18} style={{ marginRight: '0.5rem', verticalAlign: 'middle' }} />
                            Add Stream
                        </button>
                    </div>
                    <div className="streams-grid">
                        {streams.map(stream => (
                            <StreamCard
                                key={stream.id}
                                stream={stream}
                                onShowDirectory={(alias) => setSelectedDirectory(alias)}
                                onDelete={handleDeleteStream}
                            />
                        ))}
                    </div>
                </>
            )}

            {isAddModalOpen && (
                <AddStreamModal
                    onClose={() => setIsAddModalOpen(false)}
                    onAdd={handleAddStream}
                />
            )}

            {error ? <p className="error" style={{marginTop: '2rem'}}>{error}</p> : null}
            <style>{`
                .add-stream-btn {
                    background-color: #10b981;
                    color: white;
                    border: none;
                    padding: 0.5rem 1rem;
                    border-radius: 4px;
                    cursor: pointer;
                    font-weight: 500;
                    display: flex;
                    align-items: center;
                }
                .add-stream-btn:hover {
                    background-color: #059669;
                }
            `}</style>
        </main>
    )
}
