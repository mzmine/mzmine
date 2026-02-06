import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { ConfigResponse, DeploymentLimits } from '../types/config';
import { DEFAULT_CONFIG, DEFAULT_LIMITS } from '../types/config';

interface ConfigContextValue {
  config: ConfigResponse;
  limits: DeploymentLimits;
  isLoading: boolean;
  error: string | null;
}

const ConfigContext = createContext<ConfigContextValue | undefined>(undefined);

interface ConfigProviderProps {
  children: ReactNode;
}

/**
 * Provider that fetches deployment configuration from the API.
 * Makes limits available throughout the application via useConfig/useLimits hooks.
 */
export function ConfigProvider({ children }: ConfigProviderProps) {
  const [config, setConfig] = useState<ConfigResponse>(DEFAULT_CONFIG);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchConfig = async () => {
      try {
        const response = await fetch('/api/v1/config');
        if (!response.ok) {
          throw new Error(`Failed to fetch config: ${response.status}`);
        }
        const data: ConfigResponse = await response.json();
        setConfig(data);
        setError(null);
      } catch (err) {
        console.error('Failed to load config, using defaults:', err);
        setError(err instanceof Error ? err.message : 'Failed to load configuration');
        // Keep using default config on error
      } finally {
        setIsLoading(false);
      }
    };

    fetchConfig();
  }, []);

  const value: ConfigContextValue = {
    config,
    limits: config.limits,
    isLoading,
    error,
  };

  return (
    <ConfigContext.Provider value={value}>
      {children}
    </ConfigContext.Provider>
  );
}

/**
 * Hook to access the full configuration.
 */
export function useConfig(): ConfigResponse {
  const context = useContext(ConfigContext);
  if (context === undefined) {
    throw new Error('useConfig must be used within a ConfigProvider');
  }
  return context.config;
}

/**
 * Hook to access deployment limits.
 * Returns default limits if config hasn't loaded yet.
 */
export function useLimits(): DeploymentLimits {
  const context = useContext(ConfigContext);
  if (context === undefined) {
    // Return defaults if used outside provider (shouldn't happen, but safe fallback)
    return DEFAULT_LIMITS;
  }
  return context.limits;
}

/**
 * Hook to check if config is still loading.
 */
export function useConfigLoading(): boolean {
  const context = useContext(ConfigContext);
  return context?.isLoading ?? true;
}
