import { useCallback } from 'react';
import { useLocalStorage } from './useLocalStorage';

export interface RecentMolecule {
  smiles: string;
  name?: string; // Optional name from validation result
  timestamp: number;
}

const MAX_RECENT_MOLECULES = 50;
const STORAGE_KEY = 'chemaudit-recent-molecules';

/**
 * Hook for managing recently validated molecules.
 * Persists to localStorage, limits to MAX_RECENT_MOLECULES items.
 */
export function useRecentMolecules() {
  const [recent, setRecent, clearStorage] = useLocalStorage<RecentMolecule[]>(
    STORAGE_KEY,
    []
  );

  /**
   * Add a molecule to recent history.
   * Removes duplicates and limits total count.
   */
  const addRecent = useCallback((smiles: string, name?: string) => {
    if (!smiles.trim()) return;

    const newItem: RecentMolecule = {
      smiles: smiles.trim(),
      name,
      timestamp: Date.now(),
    };

    setRecent((prev) => {
      // Remove existing entry with same SMILES (case-insensitive)
      const filtered = prev.filter(
        (item) => item.smiles.toLowerCase() !== smiles.trim().toLowerCase()
      );
      // Add new item at front, limit to MAX
      return [newItem, ...filtered].slice(0, MAX_RECENT_MOLECULES);
    });
  }, [setRecent]);

  /**
   * Remove a specific molecule from history.
   */
  const removeRecent = useCallback((smiles: string) => {
    setRecent((prev) =>
      prev.filter((item) => item.smiles !== smiles)
    );
  }, [setRecent]);

  /**
   * Clear all recent molecules.
   */
  const clearRecent = useCallback(() => {
    clearStorage();
  }, [clearStorage]);

  return {
    recent,
    addRecent,
    removeRecent,
    clearRecent,
  };
}
