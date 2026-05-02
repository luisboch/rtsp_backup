import {useState, useCallback} from 'react'
import {StreamCard} from "./StreamCard";
import {DirectoryView} from "./DirectoryView";
import {StatusHeader} from "./StatusHeader";
import {Login} from "./Login";
import {AddStreamModal} from "./AddStreamModal";
import {Plus} from "lucide-react";
import {useAuth} from "./hooks/useAuth";
import {useSystemStats} from "./hooks/useSystemStats";
import {useStreams} from "./hooks/useStreams";


export function App() {
    const { isAuthenticated, isCheckingAuth, setIsAuthenticated, logout } = useAuth();
    const onUnauthorized = useCallback(() => setIsAuthenticated(false), [setIsAuthenticated]);

    const { stats, status, error: statsError } = useSystemStats(isAuthenticated, onUnauthorized);
    const { streams, error: streamsError, addStream, deleteStream, shareStream, toggleFavorite } = useStreams(isAuthenticated, onUnauthorized);

    const [selectedStream, setSelectedStream] = useState<StreamInfo | null>(null)
    const [isAddModalOpen, setIsAddModalOpen] = useState(false)

    const error = statsError || streamsError;

    const handleLogout = useCallback(async () => {
        await logout();
        setSelectedStream(null);
        setIsAddModalOpen(false);
    }, [logout]);

    if (isCheckingAuth) {
        return <div className="loading">Checking authentication...</div>;
    }

    if (!isAuthenticated) {
        return <Login onLoginSuccess={() => setIsAuthenticated(true)} />;
    }

    return (
        <main className="container">
            <StatusHeader status={status} stats={stats} onLogout={handleLogout}/>

            {selectedStream ? (
                <DirectoryView stream={selectedStream} onClose={() => setSelectedStream(null)}/>
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
                                onShowDirectory={(stream) => setSelectedStream(stream)}
                                onDelete={deleteStream}
                                onShare={shareStream}
                                onToggleFavorite={toggleFavorite}
                            />
                        ))}
                    </div>
                </>
            )}

            {isAddModalOpen && (
                <AddStreamModal
                    onClose={() => setIsAddModalOpen(false)}
                    onAdd={addStream}
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
