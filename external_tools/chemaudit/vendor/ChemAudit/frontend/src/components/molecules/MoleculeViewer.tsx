import type { ReactElement } from 'react';
import { useMolecule } from '../../hooks/useMolecule';
import { sanitizeSvg } from '../../lib/sanitize';

interface MoleculeViewerProps {
  smiles: string | null;
  highlightAtoms?: number[];
  width?: number;
  height?: number;
  className?: string;
  showCIP?: boolean;
}

const PLACEHOLDER_CLASS =
  'flex items-center justify-center rounded-lg w-full min-h-[200px]';

/**
 * Strip the fixed width/height attributes and the opaque background rect that
 * RDKit inserts. The SVG keeps its viewBox so the browser can compute the
 * correct aspect ratio; actual sizing is handled via CSS on the container.
 */
function makeSvgResponsive(svgStr: string): string {
  return svgStr
    .replace(/(<svg[^>]*)\s+width=["']\d+(?:px)?["']/, '$1')
    .replace(/(<svg[^>]*)\s+height=["']\d+(?:px)?["']/, '$1')
    .replace(/<rect[^>]*style=['"]opacity:\s*1\.0;fill:#FFFFFF[^"']*['"][^>]*\/>/, '');
}

export function MoleculeViewer({
  smiles,
  highlightAtoms = [],
  width = 300,
  height = 200,
  className = '',
  showCIP = false,
}: MoleculeViewerProps): ReactElement {
  const { svg, isLoading, error, isValid } = useMolecule(smiles, {
    width,
    height,
    highlightAtoms,
    showCIP,
  });

  if (!smiles) {
    return (
      <div className={`${PLACEHOLDER_CLASS} bg-gray-100 dark:bg-gray-800 ${className}`}>
        <span className="text-gray-400 text-sm">Enter a molecule</span>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className={`${PLACEHOLDER_CLASS} bg-gray-100 dark:bg-gray-800 ${className}`}>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !isValid) {
    return (
      <div className={`${PLACEHOLDER_CLASS} bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 ${className}`}>
        <span className="text-red-500 text-sm px-4 text-center">{error || 'Invalid molecule'}</span>
      </div>
    );
  }

  const responsiveSvg = makeSvgResponsive(sanitizeSvg(svg));

  return (
    <div
      className={`rounded-lg w-full [&>svg]:block [&>svg]:w-full [&>svg]:h-auto ${className}`}
      dangerouslySetInnerHTML={{ __html: responsiveSvg }}
    />
  );
}
