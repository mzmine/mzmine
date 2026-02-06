import { useState } from 'react';
import type { AlertResult } from '../../types/alerts';
import { AlertCard } from './AlertCard';

interface AlertResultsProps {
  alerts: AlertResult[];
  screenedCatalogs: string[];
  educationalNote?: string;
  onHighlightAtoms?: (atoms: number[]) => void;
  className?: string;
}

export function AlertResults({
  alerts,
  screenedCatalogs,
  educationalNote,
  onHighlightAtoms,
  className = '',
}: AlertResultsProps) {
  const [groupByCatalog, setGroupByCatalog] = useState(screenedCatalogs.length > 1);

  // Count alerts by severity
  const criticalCount = alerts.filter((a) => a.severity === 'critical').length;
  const warningCount = alerts.filter((a) => a.severity === 'warning').length;
  const infoCount = alerts.filter((a) => a.severity === 'info').length;

  // Group alerts by catalog if multiple catalogs
  const alertsByCatalog = alerts.reduce(
    (acc, alert) => {
      const catalog = alert.catalog_source;
      if (!acc[catalog]) {
        acc[catalog] = [];
      }
      acc[catalog].push(alert);
      return acc;
    },
    {} as Record<string, AlertResult[]>
  );

  // No alerts found
  if (alerts.length === 0) {
    return (
      <div className={`bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-6 ${className}`}>
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-12 h-12 rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-amber-600 dark:text-yellow-400">
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-amber-900 dark:text-yellow-400">
              No Structural Alerts Detected
            </h3>
            <p className="text-sm text-amber-700 dark:text-yellow-500">
              Screened against: {screenedCatalogs.join(', ')}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Summary Header */}
      <div className="bg-white rounded-lg shadow-md p-4">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">
              {alerts.length} Structural Alert{alerts.length !== 1 ? 's' : ''} Found
            </h3>
            <p className="text-sm text-gray-600">
              Screened: {screenedCatalogs.join(', ')}
            </p>
          </div>

          <div className="flex gap-2">
            {criticalCount > 0 && (
              <span className="px-3 py-1 text-sm font-medium rounded-full bg-red-100 text-red-800">
                {criticalCount} Critical
              </span>
            )}
            {warningCount > 0 && (
              <span className="px-3 py-1 text-sm font-medium rounded-full bg-amber-100 text-amber-800">
                {warningCount} Warning
              </span>
            )}
            {infoCount > 0 && (
              <span className="px-3 py-1 text-sm font-medium rounded-full bg-blue-100 text-blue-800">
                {infoCount} Info
              </span>
            )}
          </div>
        </div>

        {/* Group toggle for multiple catalogs */}
        {screenedCatalogs.length > 1 && (
          <div className="mt-3 pt-3 border-t border-gray-100">
            <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
              <input
                type="checkbox"
                checked={groupByCatalog}
                onChange={(e) => setGroupByCatalog(e.target.checked)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              Group by catalog
            </label>
          </div>
        )}
      </div>

      {/* Educational Note */}
      {educationalNote && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start gap-3">
            <div className="flex items-center justify-center w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex-shrink-0">
              <svg
                className="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div>
              <h4 className="font-medium text-blue-900 mb-1">About Structural Alerts</h4>
              <p className="text-sm text-blue-800">{educationalNote}</p>
            </div>
          </div>
        </div>
      )}

      {/* Alert Cards */}
      {groupByCatalog && screenedCatalogs.length > 1 ? (
        // Grouped view
        <div className="space-y-6">
          {Object.entries(alertsByCatalog).map(([catalog, catalogAlerts]) => (
            <div key={catalog}>
              <h4 className="font-medium text-gray-700 mb-3 flex items-center gap-2">
                <span className="px-2 py-0.5 text-xs font-medium rounded bg-gray-100">
                  {catalog}
                </span>
                <span className="text-sm text-gray-500">
                  ({catalogAlerts.length} alert{catalogAlerts.length !== 1 ? 's' : ''})
                </span>
              </h4>
              <div className="space-y-3">
                {catalogAlerts.map((alert, index) => (
                  <AlertCard
                    key={`${alert.pattern_name}-${index}`}
                    alert={alert}
                    onAtomHover={onHighlightAtoms}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        // Flat view
        <div className="space-y-3">
          {alerts.map((alert, index) => (
            <AlertCard
              key={`${alert.pattern_name}-${index}`}
              alert={alert}
              onAtomHover={onHighlightAtoms}
            />
          ))}
        </div>
      )}
    </div>
  );
}
