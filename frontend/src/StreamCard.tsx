import {useState, useEffect, useRef} from "react";
import {FolderOpen, Info, Share2, Star, Trash2, MoreVertical} from "lucide-react";
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
    const [showMenu, setShowMenu] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    // Close menu when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setShowMenu(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className="card" style={{ padding: '0.75rem 0rem 0rem 0rem', gap: '0.5rem', borderRadius: 0 }}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <div style={{display: 'flex', alignItems: 'center', marginLeft: '0.5rem'}}>
                    <Star
                        size={18}
                        onClick={() => onToggleFavorite(stream)}
                        style={{
                            cursor: 'pointer',
                            marginRight: '0.5rem',
                            fill: stream.isFavorite ? '#fbbf24' : 'none',
                            color: stream.isFavorite ? '#fbbf24' : '#94a3b8'
                        }}
                    />
                    <h3 className="stream-name" style={{ fontSize: '1rem' }}>{stream.alias}</h3>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <div className="tooltip">
                        <Info size={18} style={{cursor: 'pointer', color: '#94a3b8'}}/>
                        <span className="tooltiptext">{stream.rtspUrl}</span>
                    </div>
                    {/* Hamburger Menu */}
                    <div style={{ position: 'relative', marginRight: '0.5rem' }} ref={menuRef}>
                        <MoreVertical 
                            size={18} 
                            style={{ cursor: 'pointer', color: '#94a3b8' }} 
                            onClick={() => setShowMenu(!showMenu)}
                        />
                        {showMenu && (
                            <div className="dropdown-menu">
                                <button onClick={() => { onShowDirectory(stream); setShowMenu(false); }}>
                                    <FolderOpen size={14} /> Show Directory
                                </button>
                                {stream.isOwner && (
                                    <button onClick={() => {
                                        const username = prompt("Enter the user login to share with:");
                                        if (username) onShare(stream, username);
                                        setShowMenu(false);
                                    }}>
                                        <Share2 size={14} /> Share
                                    </button>
                                )}
                                <button className="delete-item" onClick={() => { onDelete(stream); setShowMenu(false); }}>
                                    <Trash2 size={14} /> Delete
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            <div className="video-container">
                <VideoPlayer
                    src={hlsUrl}
                    style={{width: '100%', display: 'block'}}
                />
            </div>
            
            <style>{`
                .dropdown-menu {
                    position: absolute;
                    top: 100%;
                    right: 0;
                    background: #1e293b;
                    border: 1px solid #334155;
                    border-radius: 4px;
                    z-index: 100;
                    min-width: 150px;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.2);
                    display: flex;
                    flex-direction: column;
                    padding: 0.25rem;
                }
                .dropdown-menu button {
                    background: transparent;
                    text-align: left;
                    padding: 0.5rem 0.75rem;
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    font-size: 0.85rem;
                    width: 100%;
                    border-radius: 2px;
                    border: none;
                    color: white;
                    cursor: pointer;
                }
                .dropdown-menu button:hover {
                    background: #334155;
                }
                .dropdown-menu .delete-item {
                    color: #ef4444;
                }
                .dropdown-menu .delete-item:hover {
                    background: #450a0a;
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