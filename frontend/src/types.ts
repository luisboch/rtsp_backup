export type SystemStats = {
    cpuLoadPercent: number
    memoryUsedBytes: number
    memoryTotalBytes: number
    diskUsedBytes: number
    diskTotalBytes: number
    timestamp: number
}

export type StatusPayload = {
    recording: boolean
    streamsConfigured: number
    ffmpegAvailable: boolean
    diskUsedBytes: number
    diskTotalBytes: number
    timestamp: number
}

export type StreamInfo = {
    id: string
    alias: string
    url: string
    directory: string
}
