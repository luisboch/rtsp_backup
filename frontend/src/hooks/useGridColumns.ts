import { useState, useEffect } from 'react';

const STORAGE_KEY = 'rtsp_backup_grid_columns';

export function useGridColumns(initialColumns = 3, maxColumns = 4) {
    const [columns, setColumns] = useState(() => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            const parsed = parseInt(saved, 10);
            if (!isNaN(parsed) && parsed >= 1 && parsed <= maxColumns) {
                return parsed;
            }
        }
        return initialColumns;
    });

    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, columns.toString());
    }, [columns]);

    const toggleColumns = () => {
        setColumns(prev => (prev % maxColumns) + 1);
    };

    return { columns, toggleColumns };
}
