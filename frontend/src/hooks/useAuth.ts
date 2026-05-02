import { useState, useEffect, useCallback } from 'react';

export function useAuth() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [isCheckingAuth, setIsCheckingAuth] = useState<boolean>(true);

    const checkAuth = useCallback(async () => {
        try {
            const res = await fetch('/api/status');
            if (res.ok) {
                setIsAuthenticated(true);
            } else {
                setIsAuthenticated(false);
            }
        } catch (err) {
            setIsAuthenticated(false);
        } finally {
            setIsCheckingAuth(false);
        }
    }, []);

    const logout = useCallback(async () => {
        try {
            const res = await fetch('/api/p/auth/logout', { method: 'POST' });
        } finally {
            setIsAuthenticated(false);
        }
    }, []);

    useEffect(() => {
        checkAuth();
    }, [checkAuth]);

    return {
        isAuthenticated,
        isCheckingAuth,
        setIsAuthenticated,
        logout
    };
}
