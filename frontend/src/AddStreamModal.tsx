import React, { useState } from 'react';
import { NewStream } from './types';
import { X } from 'lucide-react';

interface AddStreamModalProps {
    onClose: () => void;
    onAdd: (stream: NewStream) => Promise<void>;
}

export function AddStreamModal({ onClose, onAdd }: AddStreamModalProps) {
    const [alias, setAlias] = useState('');
    const [rtspUrl, setRtspUrl] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsSubmitting(true);

        try {
            await onAdd({ alias, rtspUrl });
            onClose();
        } catch (err: any) {
            setError(err.message || 'Failed to add stream');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h2>Add New Stream</h2>
                    <button className="close-button" onClick={onClose}>
                        <X size={24} />
                    </button>
                </div>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="alias">Alias (Unique Name)</label>
                        <input
                            id="alias"
                            type="text"
                            value={alias}
                            onChange={(e) => setAlias(e.target.value)}
                            placeholder="e.g. Front Door"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="rtspUrl">RTSP URL</label>
                        <input
                            id="rtspUrl"
                            type="text"
                            value={rtspUrl}
                            onChange={(e) => setRtspUrl(e.target.value)}
                            placeholder="rtsp://user:pass@ip:port/path"
                            required
                        />
                    </div>
                    {error && <p className="error">{error}</p>}
                    <div className="modal-actions">
                        <button type="button" onClick={onClose} disabled={isSubmitting}>
                            Cancel
                        </button>
                        <button type="submit" className="primary" disabled={isSubmitting}>
                            {isSubmitting ? 'Adding...' : 'Add Stream'}
                        </button>
                    </div>
                </form>
            </div>

            <style>{`
                .modal-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(0, 0, 0, 0.7);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 1000;
                }
                .modal-content {
                    background: #1e1e1e;
                    padding: 2rem;
                    border-radius: 8px;
                    width: 100%;
                    max-width: 500px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.5);
                }
                .modal-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 1.5rem;
                }
                .close-button {
                    background: none;
                    border: none;
                    color: #aaa;
                    cursor: pointer;
                    padding: 0;
                }
                .form-group {
                    margin-bottom: 1rem;
                }
                .form-group label {
                    display: block;
                    margin-bottom: 0.5rem;
                    color: #ccc;
                }
                .form-group input {
                    width: 100%;
                    padding: 0.75rem;
                    background: #2a2a2a;
                    border: 1px solid #444;
                    border-radius: 4px;
                    color: white;
                }
                .form-group small {
                    display: block;
                    margin-top: 0.25rem;
                    color: #888;
                    font-size: 0.8rem;
                }
                .modal-actions {
                    display: flex;
                    justify-content: flex-end;
                    gap: 1rem;
                    margin-top: 2rem;
                }
                button.primary {
                    background: #3b82f6;
                    color: white;
                    border: none;
                }
                button.primary:hover {
                    background: #2563eb;
                }
                button:disabled {
                    opacity: 0.5;
                    cursor: not-allowed;
                }
            `}</style>
        </div>
    );
}
