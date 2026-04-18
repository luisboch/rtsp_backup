import {useState} from "react";
import {EyeOff, FolderOpen} from "lucide-react";
import {StreamInfo} from "./types";

const EMPTY_PIXEL = "data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=";

export function StreamCard({stream, onShowDirectory}: {
    stream: StreamInfo,
    onShowDirectory: (alias: string) => void
}) {
    const [isPlaying, setIsPlaying] = useState(false)

    return (
        <div className="card">
            <h3 className="stream-name">{stream.alias}</h3>
            <div onClick={() => setIsPlaying(!isPlaying)} style={{cursor: 'pointer'}}>
                <div className="video-container">

                    {/* Image is always mounted, but src changes to kill the stream */}
                    <img
                        src={isPlaying ? `api/stream/proxy/${stream.id}` : EMPTY_PIXEL}
                        alt="Video Preview"
                        style={{display: isPlaying ? 'block' : 'none'}}
                    />

                    {!isPlaying && (
                        <EyeOff size={48} color="#475569"/>
                    )}

                </div>
            </div>
            <div className="card-actions">
                <button onClick={() => onShowDirectory(stream.alias)}>
                    <FolderOpen size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                    Show Directory
                </button>
            </div>
        </div>
    )
}