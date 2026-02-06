/**
 * StepsList Component
 *
 * Displays the pipeline steps that were applied during standardization,
 * along with checker issues and excluded fragments.
 */
import type { StandardizationStep, CheckerIssue } from '../../types/standardization';

interface StepsListProps {
  steps: StandardizationStep[];
  checkerIssues?: CheckerIssue[];
  excludedFragments?: string[];
}

export function StepsList({
  steps,
  checkerIssues = [],
  excludedFragments = []
}: StepsListProps) {
  return (
    <div className="space-y-4">
      {/* Pipeline Steps */}
      <div>
        <h4 className="text-sm font-semibold text-gray-700 mb-2">Pipeline Steps</h4>
        <div className="space-y-2">
          {steps.map((step, index) => (
            <div
              key={index}
              className={`flex items-start gap-3 p-3 rounded-lg border ${
                step.applied
                  ? 'border-yellow-200 bg-yellow-50 dark:border-yellow-800 dark:bg-yellow-900/20'
                  : 'border-gray-200 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50'
              }`}
            >
              {/* Status Icon */}
              <div className={`flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-xs ${
                step.applied
                  ? 'bg-amber-500 text-white'
                  : 'bg-gray-300 dark:bg-gray-600 text-white'
              }`}>
                {step.applied ? '✓' : '−'}
              </div>

              {/* Step Info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-gray-900 text-sm">
                    {formatStepName(step.step_name)}
                  </span>
                  {!step.applied && (
                    <span className="text-xs text-gray-500 px-1.5 py-0.5 bg-gray-200 rounded">
                      skipped
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-600 mt-0.5">
                  {step.description}
                </p>
                {step.changes && (
                  <p className="text-xs text-gray-500 mt-1 italic">
                    {step.changes}
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Checker Issues */}
      {checkerIssues.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold text-gray-700 mb-2">
            Checker Issues ({checkerIssues.length})
          </h4>
          <div className="space-y-2">
            {checkerIssues.map((issue, index) => (
              <div
                key={index}
                className="flex items-start gap-3 p-3 rounded-lg border border-amber-200 bg-amber-50"
              >
                <div className={`flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium ${
                  issue.penalty_score >= 10
                    ? 'bg-red-100 text-red-700'
                    : issue.penalty_score >= 5
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-yellow-100 text-yellow-700'
                }`}>
                  -{issue.penalty_score}
                </div>
                <p className="text-sm text-gray-700">{issue.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Excluded Fragments */}
      {excludedFragments.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold text-gray-700 mb-2">
            Removed Fragments ({excludedFragments.length})
          </h4>
          <div className="flex flex-wrap gap-2">
            {excludedFragments.map((fragment, index) => (
              <span
                key={index}
                className="inline-flex items-center px-2.5 py-1 rounded-md bg-gray-100 border border-gray-200"
              >
                <code className="text-xs text-gray-700 font-mono">{fragment}</code>
              </span>
            ))}
          </div>
          <p className="text-xs text-gray-500 mt-2">
            These salts, solvents, or counterions were removed during standardization.
          </p>
        </div>
      )}
    </div>
  );
}

/**
 * Format step name for display.
 */
function formatStepName(name: string): string {
  return name
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}
