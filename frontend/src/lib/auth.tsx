"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { createApi, type Api, type AuthResponse, type User } from "./api";

const STORAGE_KEY = "fila.session";

type Session = {
  token: string;
  user: User;
};

type AuthContextValue = {
  session: Session | null;
  ready: boolean;
  api: Api;
  signIn: (response: AuthResponse) => void;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        setSession(JSON.parse(raw) as Session);
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
    setReady(true);
  }, []);

  const api = useMemo(() => createApi(session?.token ?? null), [session?.token]);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      ready,
      api,
      signIn(response) {
        const next = { token: response.token, user: response.user };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setSession(next);
      },
      signOut() {
        localStorage.removeItem(STORAGE_KEY);
        setSession(null);
      },
    }),
    [api, ready, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("AuthProvider is missing");
  }
  return value;
}
