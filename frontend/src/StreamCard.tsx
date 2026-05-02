import {FolderOpen, Share2, Trash2} from "lucide-react";
import {StreamInfo} from "./types";
import {VideoPlayer} from "./VideoPlayer";

export function StreamCard({stream, onShowDirectory, onDelete, onShare}: {
    stream: StreamInfo,
    onShowDirectory: (stream: StreamInfo) => void,
    onDelete: (stream: StreamInfo) => void,
    onShare: (stream: StreamInfo, username: string) => void
}) {
    const hlsUrl = `/api/live/${stream.id}/${stream.directory}/live/index.m3u8`;

    return (
        <div className="card">
            <h3 className="stream-name">{stream.alias}</h3>
            <div>
                <div className="video-container">
                    <VideoPlayer
                        src={hlsUrl}
                        style={{width: '100%', display: 'block'}}
                    />
                </div>
            </div>
            <div className="card-actions">
                <button onClick={() => onShowDirectory(stream)}>
                    <FolderOpen size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                    Show Directory
                </button>
                <button onClick={() => {
                    const username = prompt("Enter the user login to share with:");
                    if (username) onShare(stream, username);
                }}>
                    <Share2 size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                    Share
                </button>
                <button onClick={() => onDelete(stream)} className="delete-btn">
                    <Trash2 size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                    Delete
                </button>
            </div>
            <style>{`
                .delete-btn {
                    background-color: #ef4444 !important;
                    margin-left: 0.5rem;
                }
                .delete-btn:hover {
                    background-color: #dc2626 !important;
                }
            `}</style>
        </div>
    )
}