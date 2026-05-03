import { LayoutGrid } from "lucide-react";

interface ColumnTogglerProps {
    columns: number;
    onClick: () => void;
}

export function ColumnToggler({ columns, onClick }: ColumnTogglerProps) {
    return (
        <button 
            onClick={onClick} 
            className="add-stream-btn" 
            style={{ backgroundColor: '#6366f1' }}
        >
            <LayoutGrid size={18} style={{ marginRight: '0.5rem', verticalAlign: 'middle' }} />
            {columns} Columns
        </button>
    );
}
