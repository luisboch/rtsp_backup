import { useState, useEffect, useCallback } from 'react';
import { StreamInfo, NewStream } from '../types';

export function useStreams(isAuthenticated: boolean, onUnauthorized: () => void) {
    const [streams, setStreams] = useState<StreamInfo[]>([]);
    const [error, setError] = useState<string | null>(null);

    const fetchStreams = useCallback(async () => {
        try {
            const res = await fetch('/api/streams');
            if (res.status === 401) {
                onUnauthorized();
                throw new Error('Unauthorized');
            }
            if (!res.ok) throw new Error('Failed to load streams');
            const data = await res.json();
            setStreams(data);
        } catch (err: any) {
            if (err.message !== 'Unauthorized') {
                setError(err.message);
            }
        }
    }, [onUnauthorized]);

    useEffect(() => {
        if (isAuthenticated) {
            fetchStreams();
        } else {
            setStreams([]);
        }
    }, [isAuthenticated, fetchStreams]);

    const addStream = async (newStream: NewStream) => {
        const response = await fetch('/api/streams', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(newStream),
        });

        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.message || 'Failed to add stream');
        }

        const addedStream = await response.json();
        setStreams(prev => [...prev, addedStream]);
    };

    const deleteStream = async (stream: StreamInfo) => {
        if (!window.confirm('Are you sure you want to remove this stream?')) return;

        try {
            const response = await fetch(`/api/streams/${stream.id}`, {
                method: 'DELETE',
            });

            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.message || 'Failed to delete stream');
            }

            setStreams(prev => prev.filter(s => s.id !== stream.id));
        } catch (err: any) {
            setError(err.message);
        }
    };

    const shareStream = async (stream: StreamInfo, username: string) => {
        try {
            const response = await fetch(`/api/streams/${stream.id}/share`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username }),
            });

            const data = await response.json();
            if (response.ok) {
                alert('Stream shared successfully!');
            } else {
                alert(data.message || 'Failed to share stream');
            }
        } catch (err) {
            alert('Connection error while sharing');
        }
    };

    return {
        streams,
        error,
        setError,
        addStream,
        deleteStream,
        shareStream,
        refreshStreams: fetchStreams
    };
}
