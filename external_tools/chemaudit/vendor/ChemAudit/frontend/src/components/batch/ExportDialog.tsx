import { useState } from 'react';
import { Download } from 'lucide-react';
import { ClayButton } from '../ui/ClayButton';

/**
 * Export format type matching backend ExportFormat enum
 */
export type ExportFormat = 'csv' | 'excel' | 'sdf' | 'json' | 'pdf';

interface ExportDialogProps {
  jobId: string;
  isOpen: boolean;
  onClose: () => void;
  selectedIndices?: Set<number>;
}

/**
 * Dialog for selecting export format and downloading batch results.
 *
 * Supports five formats:
 * - CSV: Plain text data
 * - Excel: Formatted spreadsheet with conditional coloring
 * - SDF: Chemical structure file with properties
 * - JSON: Programmatic access with full metadata
 * - PDF: Professional report with charts and images
 */
export function ExportDialog({ jobId, isOpen, onClose, selectedIndices }: ExportDialogProps) {
  const [selectedFormat, setSelectedFormat] = useState<ExportFormat>('csv');
  const [isExporting, setIsExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const isSelectedMode = selectedIndices && selectedIndices.size > 0;

  const handleExport = async () => {
    setIsExporting(true);
    setError(null);

    try {
      let response: Response;

      if (!isSelectedMode) {
        // Export all: use GET request
        const url = `/api/v1/batch/${jobId}/export?format=${selectedFormat}`;
        response = await fetch(url);
      } else {
        // Export selected: use GET for small selections, POST for large
        const indicesArray = Array.from(selectedIndices).sort((a, b) => a - b);

        if (indicesArray.length <= 200) {
          // Small selection: use GET with query parameter
          const indicesParam = indicesArray.join(',');
          const url = `/api/v1/batch/${jobId}/export?format=${selectedFormat}&indices=${indicesParam}`;
          response = await fetch(url);
        } else {
          // Large selection: use POST with JSON body
          const url = `/api/v1/batch/${jobId}/export?format=${selectedFormat}`;
          response = await fetch(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ indices: indicesArray }),
          });
        }
      }

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.detail || 'Export failed');
      }

      // Get filename from Content-Disposition header or generate default
      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = `batch_${jobId}.${selectedFormat}`;
      if (contentDisposition) {
        const match = contentDisposition.match(/filename="?([^"]+)"?/);
        if (match) filename = match[1];
      }

      // Download file
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);

      // Close dialog on success
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    } finally {
      setIsExporting(false);
    }
  };

  const formats: Array<{
    value: ExportFormat;
    label: string;
    description: string;
    icon: string;
  }> = [
    {
      value: 'csv',
      label: 'CSV',
      description: 'Plain text data - Excel, Google Sheets compatible',
      icon: '📄',
    },
    {
      value: 'excel',
      label: 'Excel',
      description: 'Formatted spreadsheet with colors and summary sheet',
      icon: '📊',
    },
    {
      value: 'sdf',
      label: 'SDF',
      description: 'Chemical structure file with attached properties',
      icon: '🧪',
    },
    {
      value: 'json',
      label: 'JSON',
      description: 'Programmatic access with full metadata',
      icon: '{ }',
    },
    {
      value: 'pdf',
      label: 'PDF',
      description: 'Professional report with charts and molecule images',
      icon: '📑',
    },
  ];

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-[var(--color-surface-elevated)] rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto border border-[var(--color-border)]">
        {/* Header */}
        <div className="border-b border-[var(--color-border)] px-6 py-4">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Export Results</h2>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            {isSelectedMode ? (
              <>Export Selected ({selectedIndices.size} molecules)</>
            ) : (
              <>Choose a format to download batch validation results</>
            )}
          </p>
        </div>

        {/* Format selection */}
        <div className="px-6 py-4 space-y-3">
          {formats.map((format) => (
            <label
              key={format.value}
              className={`
                flex items-start p-4 border-2 rounded-lg cursor-pointer
                transition-colors hover:bg-[var(--color-surface-sunken)]
                ${
                  selectedFormat === format.value
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5'
                    : 'border-[var(--color-border)]'
                }
              `}
            >
              <input
                type="radio"
                name="format"
                value={format.value}
                checked={selectedFormat === format.value}
                onChange={(e) => setSelectedFormat(e.target.value as ExportFormat)}
                className="mt-1 text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
              />
              <div className="ml-3 flex-1">
                <div className="flex items-center">
                  <span className="text-2xl mr-2">{format.icon}</span>
                  <span className="font-medium text-[var(--color-text-primary)]">{format.label}</span>
                </div>
                <p className="text-sm text-[var(--color-text-secondary)] mt-1">{format.description}</p>
              </div>
            </label>
          ))}
        </div>

        {/* Error message */}
        {error && (
          <div className="mx-6 mb-4 p-3 bg-red-500/10 dark:bg-red-500/20 border border-red-500/30 rounded-lg">
            <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
          </div>
        )}

        {/* Actions */}
        <div className="border-t border-[var(--color-border)] px-6 py-4 flex justify-end space-x-3">
          <ClayButton
            variant="default"
            onClick={onClose}
            disabled={isExporting}
          >
            Cancel
          </ClayButton>
          <ClayButton
            variant="primary"
            onClick={handleExport}
            disabled={isExporting}
            loading={isExporting}
            leftIcon={!isExporting ? <Download className="w-4 h-4" /> : undefined}
          >
            {isExporting ? 'Exporting...' : `Download ${selectedFormat.toUpperCase()}`}
          </ClayButton>
        </div>
      </div>
    </div>
  );
}
