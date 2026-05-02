import { useState, useEffect } from 'react';
import { SystemStats, StatusPayload } from '../types';

export function useSystemStats(isAuthenticated: boolean, onUnauthorized: () => void) {
    const [stats, setStats] = useState<SystemStats | null>(null);
    const [status, setStatus] = useState<StatusPayload | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!isAuthenticated) {
            setStats(null);
            setStatus(null);
            return;
        }

        const refreshStatus = () => {
            fetch('/api/status')
                .then((response) => {
                    if (response.status === 401) {
                        onUnauthorized();
                        throw new Error('Unauthorized');
                    }
                    return response.json();
                })
                .then((payload: StatusPayload) => setStatus(payload))
                .catch((err) => {
                    if (err.message !== 'Unauthorized') {
                        setError('Failed to load server status');
                    }
                });
        };

        refreshStatus();
        const statusInterval = setInterval(refreshStatus, 5000);

        const eventSource = new EventSource('/api/stats/sse');
        eventSource.addEventListener('stats', (event) => {
            const payload = JSON.parse((event as MessageEvent).data) as SystemStats;
            setStats(payload);
        });

        eventSource.onerror = (e) => {
            console.error('SSE Error', e);
            setError('SSE connection interrupted');
            eventSource.close();
        };

        return () => {
            clearInterval(statusInterval);
            eventSource.close();
        };
    }, [isAuthenticated, onUnauthorized]);

    return { stats, status, error, setError, setStats, setStatus };
}
