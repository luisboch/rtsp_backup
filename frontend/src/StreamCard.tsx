import {useEffect, useRef} from "react";
import {FolderOpen, Trash2} from "lucide-react";
import {StreamInfo} from "./types";
import Hls from 'hls.js';

export function StreamCard({stream, onShowDirectory, onDelete}: {
    stream: StreamInfo,
    onShowDirectory: (alias: string) => void,
    onDelete: (stream: StreamInfo) => void
}) {
    const videoRef = useRef<HTMLVideoElement>(null);

    useEffect(() => {
        const video = videoRef.current;
        if (!video) return;

        const hlsUrl = `/api/live/${stream.directory}/live/index.m3u8`;

        if (Hls.isSupported()) {
            const hls = new Hls();
            hls.loadSource(hlsUrl);
            hls.attachMedia(video);
            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                video.play().catch(e => console.error("Error playing video:", e));
            });
            return () => {
                hls.destroy();
            };
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = hlsUrl;
            video.addEventListener('loadedmetadata', () => {
                video.play().catch(e => console.error("Error playing video:", e));
            });
        }
    }, [stream.directory]);

    return (
        <div className="card">
            <h3 className="stream-name">{stream.alias}</h3>
            <div>
                <div className="video-container">
                    <video
                        ref={videoRef}
                        controls
                        muted
                        autoPlay
                        style={{width: '100%', display: 'block'}}
                    />
                </div>
            </div>
            <div className="card-actions">
                <button onClick={() => onShowDirectory(stream.alias)}>
                    <FolderOpen size={16} style={{marginRight: '0.5rem', verticalAlign: 'middle'}}/>
                    Show Directory
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