import {useEffect, useRef} from "react";
import Hls from 'hls.js';

interface VideoPlayerProps {
    src: string;
    controls?: boolean;
    muted?: boolean;
    autoPlay?: boolean;
    style?: React.CSSProperties;
    className?: string;
}

export function VideoPlayer({
    src,
    controls = true,
    muted = true,
    autoPlay = true,
    style,
    className
}: VideoPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null);

    useEffect(() => {
        const video = videoRef.current;
        if (!video) return;

        if (Hls.isSupported()) {
            const hls = new Hls();
            hls.loadSource(src);
            hls.attachMedia(video);
            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                if (autoPlay) {
                    video.play().catch(e => console.error("Error playing video:", e));
                }
            });
            return () => {
                hls.destroy();
            };
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = src;
            video.addEventListener('loadedmetadata', () => {
                if (autoPlay) {
                    video.play().catch(e => console.error("Error playing video:", e));
                }
            });
        }
    }, [src, autoPlay]);

    return (
        <video
            ref={videoRef}
            controls={controls}
            muted={muted}
            autoPlay={autoPlay}
            style={style}
            className={className}
        />
    );
}
