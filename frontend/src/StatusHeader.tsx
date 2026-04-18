import {useEffect, useMemo, useState} from "react";
import {Activity, AlertCircle, CheckCircle2, Clock, Layers} from "lucide-react";
import {StatusPayload, SystemStats} from "./types";

export function StatusHeader({status, stats}: { status: StatusPayload | null, stats: SystemStats | null }) {
    const [now, setNow] = useState(Date.now())

    useEffect(
        () => {
            const timer = setInterval(
                () => setNow(Date.now()),
                1000
            )
            return () => clearInterval(timer)
        },
        []
    )

    const staleness = status ? (now - status.timestamp) / 1000 : Infinity

    const clockColor = useMemo(
        () => {
            if (staleness < 10) return '#22c55e' // green
            if (staleness < 30) return '#f97316' // orange
            return '#ef4444' // red
        },
        [staleness]
    )

    return (
        <header>
            <h1 style={{margin: 0, fontSize: '1.5rem'}}>RTSP Backup</h1>
            <div className="status-group">
                <div className="status-item">
                    <Activity size={18} color={status?.recording ? '#22c55e' : '#94a3b8'}/>
                    <span>Recording: {status?.recording ? 'Active' : 'Idle'}</span>
                </div>
                <div className="status-item">
                    <Layers size={18} color="#3b82f6"/>
                    <span>Streams: {status?.streamsConfigured ?? 0}</span>
                </div>
                <div className="status-item">
                    {status?.ffmpegAvailable ? (
                        <CheckCircle2 size={18} color="#22c55e"/>
                    ) : (
                        <AlertCircle size={18} color="#ef4444"/>
                    )}
                    <span>FFmpeg</span>
                </div>
                <div className="status-item">
                    <Clock size={18} color={clockColor}/>
                    <span>{status ? new Date(status.timestamp).toLocaleTimeString() : '--:--:--'}</span>
                </div>
            </div>
        </header>
    )
}