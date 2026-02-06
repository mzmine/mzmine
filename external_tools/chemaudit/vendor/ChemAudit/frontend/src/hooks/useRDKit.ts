import { useState, useEffect } from 'react';

interface RDKitModule {
  get_mol: (input: string, opts?: object) => RDKitMol | null;
  get_mol_from_molblock: (molblock: string) => RDKitMol | null;
  version: () => string;
}

interface RDKitMol {
  get_svg: (width?: number, height?: number) => string;
  get_svg_with_highlights: (details: string) => string;
  get_smiles: () => string;
  get_molblock: () => string;
  delete: () => void;  // CRITICAL: Must call to free WASM memory
}

let rdkitInstance: RDKitModule | null = null;
let rdkitPromise: Promise<RDKitModule> | null = null;

export function getRDKit(): Promise<RDKitModule> {
  if (rdkitInstance) {
    return Promise.resolve(rdkitInstance);
  }

  if (rdkitPromise) {
    return rdkitPromise;
  }

  rdkitPromise = new Promise((resolve, reject) => {
    const win = window as any;
    if (typeof win.initRDKitModule === 'undefined') {
      reject(new Error('RDKit.js not loaded. Check script tag in index.html'));
      return;
    }

    win.initRDKitModule()
      .then((rdkit: RDKitModule) => {
        rdkitInstance = rdkit;
        resolve(rdkit);
      })
      .catch(reject);
  });

  return rdkitPromise;
}

export function useRDKit() {
  const [rdkit, setRDKit] = useState<RDKitModule | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    getRDKit()
      .then((module) => {
        if (mounted) {
          setRDKit(module);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (mounted) {
          setError(err.message);
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  return { rdkit, loading, error };
}

export type { RDKitModule, RDKitMol };
