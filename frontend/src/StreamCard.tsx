import {FolderOpen, Info, Share2, Star, Trash2} from "lucide-react";
import {StreamInfo} from "./types";
import {VideoPlayer} from "./VideoPlayer";

export function StreamCard({stream, onShowDirectory, onDelete, onShare, onToggleFavorite}: {
    stream: StreamInfo,
    onShowDirectory: (stream: StreamInfo) => void,
    onDelete: (stream: StreamInfo) => void,
    onShare: (stream: StreamInfo, username: string) => void,
    onToggleFavorite: (stream: StreamInfo) => void
}) {
    const hlsUrl = `/api/live/${stream.id}/${stream.directory}/live/index.m3u8`;

    return (
        <div className="card">
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <div style={{display: 'flex', alignItems: 'center'}}>
                    <Star
                        size={20}
                        onClick={() => onToggleFavorite(stream)}
                        style={{
                            cursor: 'pointer',
                            marginRight: '0.5rem',
                            fill: stream.isFavorite ? '#fbbf24' : 'none',
                            color: stream.isFavorite ? '#fbbf24' : '#94a3b8'
                        }}
                    />
                    <h3 className="stream-name">{stream.alias}</h3>
                </div>
                <div className="tooltip">
                    <Info size={18} style={{cursor: 'pointer', color: '#94a3b8'}}/>
                    <span className="tooltiptext">{stream.rtspUrl}</span>
                </div>
            </div>
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
                {stream.isOwner && (
                    <button onClick={() => {
                        const username = prompt("Enter the user login to share with:");
                        if (username) onShare(stream, username);
                    }}>
                        <Share2 size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                        Share
                    </button>
                )}
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
                .tooltip {
                    position: relative;
                    display: inline-block;
                }
                .tooltip .tooltiptext {
                    visibility: hidden;
                    width: 200px;
                    background-color: #1e293b;
                    color: #fff;
                    text-align: center;
                    border-radius: 6px;
                    padding: 5px;
                    position: absolute;
                    z-index: 1;
                    bottom: 125%;
                    left: 50%;
                    margin-left: -100px;
                    opacity: 0;
                    transition: opacity 0.3s;
                    font-size: 0.8rem;
                    word-break: break-all;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                }
                .tooltip:hover .tooltiptext {
                    visibility: visible;
                    opacity: 1;
                }
            `}</style>
        </div>
    )
}