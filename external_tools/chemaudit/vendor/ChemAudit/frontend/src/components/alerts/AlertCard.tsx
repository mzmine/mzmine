import type { AlertResult, AlertSeverity } from '../../types/alerts';

interface AlertCardProps {
  alert: AlertResult;
  onAtomHover?: (atoms: number[]) => void;
  className?: string;
}

/**
 * Known PAINS patterns that appear in FDA-approved drugs.
 * Provides educational context to help users understand alerts are warnings.
 */
const APPROVED_DRUG_EXAMPLES: Record<string, string[]> = {
  rhodanine: ['Methotrexate', 'Epalrestat'],
  catechol: ['Dopamine', 'Epinephrine', 'Levodopa'],
  quinone: ['Doxorubicin', 'Vitamin K'],
  michael_acceptor: ['Ibrutinib', 'Afatinib'],
  azo: ['Sulfasalazine', 'Phenazopyridine'],
  thiourea: ['Methimazole', 'Thiouracil'],
};

function getApprovedDrugNote(patternName: string): string | null {
  const patternLower = patternName.toLowerCase();

  for (const [key, drugs] of Object.entries(APPROVED_DRUG_EXAMPLES)) {
    if (patternLower.includes(key)) {
      return `Found in approved drugs: ${drugs.slice(0, 2).join(', ')}`;
    }
  }

  return null;
}

export function AlertCard({ alert, onAtomHover, className = '' }: AlertCardProps) {
  const getSeverityStyles = (severity: AlertSeverity) => {
    switch (severity) {
      case 'critical':
        return {
          bg: 'bg-red-50',
          border: 'border-red-200',
          badge: 'bg-red-100 text-red-800',
          icon: '!!!',
        };
      case 'warning':
        return {
          bg: 'bg-amber-50',
          border: 'border-amber-200',
          badge: 'bg-amber-100 text-amber-800',
          icon: '!',
        };
      case 'info':
        return {
          bg: 'bg-blue-50',
          border: 'border-blue-200',
          badge: 'bg-blue-100 text-blue-800',
          icon: 'i',
        };
      default:
        return {
          bg: 'bg-gray-50',
          border: 'border-gray-200',
          badge: 'bg-gray-100 text-gray-800',
          icon: '?',
        };
    }
  };

  const styles = getSeverityStyles(alert.severity);

  const formatPatternName = (name: string) => {
    // Remove trailing numbers/IDs like "(370)"
    const cleaned = name.replace(/\(\d+\)$/, '').trim();
    // Replace underscores and capitalize
    return cleaned
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  const approvedDrugNote = getApprovedDrugNote(alert.pattern_name);

  return (
    <div
      className={`${styles.bg} border ${styles.border} rounded-lg p-4 cursor-pointer transition-all hover:shadow-md ${className}`}
      onMouseEnter={() => onAtomHover?.(alert.matched_atoms)}
      onMouseLeave={() => onAtomHover?.([])}
    >
      <div className="flex items-start gap-3">
        <div
          className={`flex items-center justify-center w-8 h-8 rounded-full ${styles.badge} font-bold text-sm`}
        >
          {styles.icon}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h4 className="font-medium text-gray-900">
              {formatPatternName(alert.pattern_name)}
            </h4>
            <span
              className={`px-2 py-0.5 text-xs font-medium rounded-full ${styles.badge}`}
            >
              {alert.severity.toUpperCase()}
            </span>
            <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-gray-100 text-gray-600">
              {alert.catalog_source}
            </span>
          </div>

          <p className="text-sm text-gray-700 mb-2">{alert.description}</p>

          {alert.matched_atoms.length > 0 && (
            <div className="text-xs text-gray-500 mb-2">
              <span className="font-medium">Matched atoms:</span>{' '}
              {alert.matched_atoms.join(', ')}
              <span className="ml-2 text-amber-600">(hover to highlight)</span>
            </div>
          )}

          {approvedDrugNote && (
            <div className="text-xs text-amber-700 dark:text-yellow-400 bg-yellow-50 dark:bg-yellow-900/30 rounded px-2 py-1 inline-block">
              {approvedDrugNote}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
