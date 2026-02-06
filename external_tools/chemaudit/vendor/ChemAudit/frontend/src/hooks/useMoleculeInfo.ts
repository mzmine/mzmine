import { useState, useEffect, useRef } from 'react';
import { getRDKit, RDKitMol } from './useRDKit';

interface MoleculeInfo {
  canonicalSmiles: string;
  kekulizedSmiles: string | null;
  numAtoms: number;
  numBonds: number;
  numRings: number;
  numStereocenters: number;
  hasStereochemistry: boolean;
  isValid: boolean;
}

interface UseMoleculeInfoResult {
  info: MoleculeInfo | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * Convert aromatic SMILES to kekulized form
 * This is a simple heuristic - replaces lowercase aromatic atoms with uppercase
 * and handles basic aromatic ring patterns
 */
function kekulizeSmiles(smiles: string): string | null {
  // Check if there are aromatic atoms (lowercase c, n, o, s, etc.)
  if (!/[cnos]/.test(smiles)) {
    return null; // No aromatic atoms, kekulized would be the same
  }

  // Simple kekulization: replace aromatic atoms with their uppercase versions
  // and add alternating double bonds for 6-membered rings
  // This is a simplified approach - RDKit on backend does proper kekulization

  let kekulized = smiles;

  // Replace aromatic atoms with uppercase
  kekulized = kekulized
    .replace(/c/g, 'C')
    .replace(/n/g, 'N')
    .replace(/o/g, 'O')
    .replace(/s/g, 'S');

  // For simple aromatic rings like benzene (C1CCCCC1), add alternating double bonds
  // Match patterns like C1CCCCC1 and convert to C1=CC=CC=C1
  kekulized = kekulized.replace(
    /C(\d)(C)(C)(C)(C)(C)\1/g,
    'C$1=C$3=C$5=C$1'
  );

  // Handle 6-membered rings - this is a simplified pattern
  // Real kekulization is more complex, but this handles common cases
  const ringPattern = /([A-Z])(\d+)([A-Z])([A-Z])([A-Z])([A-Z])([A-Z])\2/;
  if (ringPattern.test(kekulized)) {
    kekulized = kekulized.replace(ringPattern, '$1$2=$3$4=$5$6=$7$2');
  }

  return kekulized !== smiles ? kekulized : null;
}

/**
 * Hook to extract basic molecule information using RDKit.js
 * Shows info immediately when a valid molecule is entered
 */
export function useMoleculeInfo(smiles: string | null): UseMoleculeInfoResult {
  const [info, setInfo] = useState<MoleculeInfo | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const molRef = useRef<RDKitMol | null>(null);

  useEffect(() => {
    if (!smiles || smiles.trim() === '') {
      setInfo(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setError(null);

    getRDKit()
      .then((rdkit) => {
        if (cancelled) return;

        // Clean up previous molecule
        if (molRef.current) {
          molRef.current.delete();
          molRef.current = null;
        }

        try {
          const mol = rdkit.get_mol(smiles);

          if (!mol) {
            setError('Invalid molecule structure');
            setInfo(null);
            setIsLoading(false);
            return;
          }

          molRef.current = mol;

          // Extract info using RDKit methods
          const canonicalSmiles = mol.get_smiles();

          // Try multiple approaches to get kekulized SMILES
          let kekulizedSmiles: string | null = null;

          // Approach 1: Try RDKit.js get_smiles with kekulize option
          try {
            const rdkitMol = mol as any;

            // Try different option formats that RDKit.js might accept
            if (typeof rdkitMol.get_smiles === 'function') {
              // Try with JSON options
              const result1 = rdkitMol.get_smiles(JSON.stringify({ kekulize: true }));
              if (result1 && result1 !== canonicalSmiles) {
                kekulizedSmiles = result1;
              }

              // If that didn't work, try with object directly
              if (!kekulizedSmiles) {
                const result2 = rdkitMol.get_smiles({ kekulize: true });
                if (result2 && typeof result2 === 'string' && result2 !== canonicalSmiles) {
                  kekulizedSmiles = result2;
                }
              }
            }
          } catch {
            // RDKit.js kekulize option not available
          }

          // Approach 2: If RDKit didn't provide kekulized, use simple heuristic
          if (!kekulizedSmiles) {
            kekulizedSmiles = kekulizeSmiles(canonicalSmiles);
          }

          // Get JSON info which includes atom/bond counts
          const molJson = (mol as any).get_json?.();
          let numAtoms = 0;
          let numBonds = 0;
          let numRings = 0;

          if (molJson) {
            try {
              const parsed = JSON.parse(molJson);
              numAtoms = parsed.molecules?.[0]?.atoms?.length || 0;
              numBonds = parsed.molecules?.[0]?.bonds?.length || 0;
            } catch {
              // Fallback: count from SMILES (rough estimate)
              numAtoms = canonicalSmiles.replace(/[^A-Z]/gi, '').length;
            }
          } else {
            // Fallback: rough estimate from SMILES
            const atomMatches = canonicalSmiles.match(/[A-Z][a-z]?/g);
            numAtoms = atomMatches ? atomMatches.length : 0;
          }

          // Get ring info if available
          const ringInfo = (mol as any).get_ring_info?.();
          if (ringInfo) {
            try {
              const parsed = JSON.parse(ringInfo);
              numRings = parsed.atomRings?.length || 0;
            } catch {
              // Count ring closures in SMILES as estimate
              const ringMatches = canonicalSmiles.match(/\d/g);
              numRings = ringMatches ? Math.floor(ringMatches.length / 2) : 0;
            }
          } else {
            // Count ring closures in SMILES
            const ringMatches = canonicalSmiles.match(/\d/g);
            numRings = ringMatches ? Math.floor(ringMatches.length / 2) : 0;
          }

          // Detect stereochemistry from SMILES
          // @ or @@ indicates tetrahedral stereocenters
          // / or \ indicates E/Z double bond stereochemistry
          const stereoMatches = canonicalSmiles.match(/@+/g);
          const numStereocenters = stereoMatches ? stereoMatches.length : 0;
          const hasEZStereo = /[/\\]/.test(canonicalSmiles);
          const hasStereochemistry = numStereocenters > 0 || hasEZStereo;

          setInfo({
            canonicalSmiles,
            kekulizedSmiles,
            numAtoms,
            numBonds,
            numRings,
            numStereocenters,
            hasStereochemistry,
            isValid: true,
          });
          setError(null);
        } catch (e) {
          setError(e instanceof Error ? e.message : 'Failed to parse molecule');
          setInfo(null);
        } finally {
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message);
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
      if (molRef.current) {
        molRef.current.delete();
        molRef.current = null;
      }
    };
  }, [smiles]);

  return { info, isLoading, error };
}
