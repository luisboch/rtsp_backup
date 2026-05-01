import {useEffect, useMemo, useState} from "react";
import {Activity, AlertCircle, CheckCircle2, Clock, Cpu, HardDrive, Layers, LogOut} from "lucide-react";
import {StatusPayload, SystemStats} from "./types";

export function StatusHeader({status, stats, onLogout}: {
    status: StatusPayload | null,
    stats: SystemStats | null,
    onLogout?: () => void
}) {
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

    const formatBytes = (bytes: number) => {
        if (bytes === 0) return '0 B'
        const k = 1024
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const diskUsage = useMemo(
        () => {
            const used = status?.diskUsedBytes ?? stats?.diskUsedBytes ?? 0
            const total = status?.diskTotalBytes ?? stats?.diskTotalBytes ?? 0
            if (total === 0) return '0 / 0'
            return `${formatBytes(used)} / ${formatBytes(total)}`
        },
        [status, stats]
    )

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
                    <HardDrive size={18} color="#8b5cf6"/>
                    <span>Disk: {diskUsage}</span>
                </div>
                <div className="status-item">
                    <Cpu size={18} color="#f59e0b"/>
                    <span>CPU: {stats?.cpuLoadPercent ?? 0}%</span>
                </div>
                <div className="status-item">
                    <Clock size={18} color={clockColor}/>
                    <span>{status ? new Date(status.timestamp).toLocaleTimeString() : '--:--:--'}</span>
                </div>
                {onLogout && (
                    <button
                        onClick={onLogout}
                        style={{
                            background: 'transparent',
                            color: '#ef4444',
                            padding: '0.25rem',
                            display: 'flex',
                            alignItems: 'center'
                        }}
                        title="Logout"
                    >
                        <LogOut size={18}/>
                    </button>
                )}
            </div>
        </header>
    )
}