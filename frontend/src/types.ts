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
    ownerId: number
    alias: string
    rtspUrl: string
    directory: string
}

export type NewStream = {
    alias: string
    rtspUrl: string
}

export type ShareStreamRequest = {
    username: string
}

export type UserSession = {
    userId: number
    token: string
}
