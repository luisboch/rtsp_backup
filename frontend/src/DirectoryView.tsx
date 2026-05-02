import {useEffect, useState} from "react";
import {X} from "lucide-react";
import {StreamInfo} from "./types";

export function DirectoryView({stream, onClose}: { stream: StreamInfo, onClose: () => void }) {
    const [files, setFiles] = useState<string[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(
        () => {
            fetch(`/api/files/${stream.id}`)
                .then(res => {
                    if (res.status === 401) {
                        window.location.reload(); // Simple way to trigger auth check in App.tsx
                        return [];
                    }
                    return res.json()
                })
                .then(data => {
                    setFiles(data)
                    setLoading(false)
                })
        },
        [stream.id]
    )

    return (
        <div className="directory-view card">
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <h2 style={{margin: 0}}>Files for {stream.alias}</h2>
                <button onClick={onClose} style={{background: '#334155'}}>
                    <X size={20}/>
                </button>
            </div>
            {loading ? (
                <p>Loading files...</p>
            ) : (
                <ul className="file-list">
                    {files.map(file => (
                        <li key={file} className="file-item">
                            <span>{file.split('/').pop()}</span>
                            <a href={`/api/video/${stream.id}/${file}`} target="_blank" rel="noreferrer">
                                Open
                            </a>
                            <a href={`/api/video/${stream.id}/${file}?download=true`} target="_blank" rel="noreferrer">
                                Download
                            </a>
                        </li>
                    ))}
                    {files.length === 0 && <p>No files found.</p>}
                </ul>
            )}
        </div>
    )
}