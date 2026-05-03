import {useState, useCallback} from 'react'
import {StreamCard} from "./StreamCard";
import {DirectoryView} from "./DirectoryView";
import {StatusHeader} from "./StatusHeader";
import {Login} from "./Login";
import {AddStreamModal} from "./AddStreamModal";
import {Plus, Maximize2} from "lucide-react";
import {useAuth} from "./hooks/useAuth";
import {useSystemStats} from "./hooks/useSystemStats";
import {useStreams} from "./hooks/useStreams";
import {useGridColumns} from "./hooks/useGridColumns";
import {ColumnToggler} from "./components/ColumnToggler";
import {FullscreenFavorites} from "./components/FullscreenFavorites";
import {StreamInfo} from "./types";


export function App() {
    const { isAuthenticated, isCheckingAuth, setIsAuthenticated, logout } = useAuth();
    const onUnauthorized = useCallback(() => setIsAuthenticated(false), [setIsAuthenticated]);

    const { stats, status, error: statsError } = useSystemStats(isAuthenticated, onUnauthorized);
    const { streams, error: streamsError, addStream, deleteStream, shareStream, toggleFavorite } = useStreams(isAuthenticated, onUnauthorized);
    const { columns, toggleColumns } = useGridColumns(3);

    const [selectedStream, setSelectedStream] = useState<StreamInfo | null>(null)
    const [isAddModalOpen, setIsAddModalOpen] = useState(false)
    const [isFullscreenFavorites, setIsFullscreenFavorites] = useState(false)

    const error = statsError || streamsError;

    const handleLogout = useCallback(async () => {
        await logout();
        setSelectedStream(null);
        setIsAddModalOpen(false);
        setIsFullscreenFavorites(false);
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
            ) : isFullscreenFavorites ? (
                <FullscreenFavorites 
                    streams={streams} 
                    columns={columns} 
                    onClose={() => setIsFullscreenFavorites(false)} 
                />
            ) : (
                <>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem', gap: '0.5rem' }}>
                        <button 
                            onClick={() => setIsFullscreenFavorites(true)} 
                            className="fullscreen-favorites-btn"
                            title="Fullscreen Favorites"
                        >
                            <Maximize2 size={18} />
                        </button>
                        <button onClick={() => setIsAddModalOpen(true)} className="add-stream-btn">
                            <Plus size={18} style={{ marginRight: '0.5rem', verticalAlign: 'middle' }} />
                            Add Stream
                        </button>
                        <ColumnToggler columns={columns} onClick={toggleColumns} />
                    </div>
                    <div className="streams-grid" style={{ 
                        gridTemplateColumns: `repeat(${columns}, 1fr)`,
                        gap: '0',
                        maxWidth: 'none'
                    }}>
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
        </main>
    )
}
