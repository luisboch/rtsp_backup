import { Minimize2 } from "lucide-react";
import { VideoPlayer } from "../VideoPlayer";
import { StreamInfo } from "../types";

interface FullscreenFavoritesProps {
    streams: StreamInfo[];
    columns: number;
    onClose: () => void;
}

export function FullscreenFavorites({ streams, columns, onClose }: FullscreenFavoritesProps) {
    const favoriteStreams = streams.filter(s => s.isFavorite);

    return (
        <div className="fullscreen-overlay">
            <div 
                className="fullscreen-grid" 
                style={{ gridTemplateColumns: `repeat(${columns}, 1fr)` }}
            >
                {favoriteStreams.map(stream => (
                    <VideoPlayer
                        key={stream.id}
                        src={`/api/live/${stream.id}/${stream.directory}/live/index.m3u8`}
                        style={{ width: '100%', display: 'block' }}
                    />
                ))}
            </div>
            <button 
                onClick={onClose}
                className="fullscreen-close-btn"
                title="Exit Fullscreen"
            >
                <Minimize2 size={24} />
            </button>
        </div>
    );
}
