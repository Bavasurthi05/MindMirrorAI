import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { apiRequest } from '../lib/api';
import { AUTH_STORAGE_KEY } from '../lib/config';
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload } from '../lib/auth-types';

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    const session = readSession();
    if (session) {
      setUser({
        userId: session.userId,
        email: session.email,
        fullName: session.fullName,
        role: session.role,
        emailVerified: session.emailVerified,
      });
    }
    setIsInitializing(false);
  }, []);

  const persist = useCallback((session: AuthSession) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
    setUser({
      userId: session.userId,
      email: session.email,
      fullName: session.fullName,
      role: session.role,
      emailVerified: session.emailVerified,
    });
  }, []);

  const login = useCallback(
    async (payload: LoginPayload) => {
      const session = await apiRequest<AuthSession>('/auth/login', {
        method: 'POST',
        body: payload,
        auth: false,
      });
      persist(session);
    },
    [persist],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => {
      const session = await apiRequest<AuthSession>('/auth/register', {
        method: 'POST',
        body: payload,
        auth: false,
      });
      persist(session);
    },
    [persist],
  );

  const logout = useCallback(() => {
    const session = readSession();
    if (session?.refreshToken) {
      void apiRequest<void>('/auth/logout', {
        method: 'POST',
        body: { refreshToken: session.refreshToken },
        auth: false,
      }).catch(() => undefined);
    }
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isInitializing,
      login,
      register,
      logout,
    }),
    [user, isInitializing, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
