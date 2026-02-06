/**
 * StandardizationResults Component
 *
 * Displays complete standardization results including:
 * - Before/after molecule comparison
 * - Stereochemistry warning (if any)
 * - Pipeline steps
 * - Mass change indicator
 * - Copy SMILES button
 */
import { useState } from 'react';
import { ComparisonView } from './ComparisonView';
import { StepsList } from './StepsList';
import type { StandardizationResult } from '../../types/standardization';

interface StandardizationResultsProps {
  result: StandardizationResult;
}

export function StandardizationResults({ result }: StandardizationResultsProps) {
  const [copied, setCopied] = useState(false);

  const handleCopySmiles = async () => {
    if (result.standardized_smiles) {
      try {
        await navigator.clipboard.writeText(result.standardized_smiles);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch (err) {
        console.error('Failed to copy:', err);
      }
    }
  };

  if (!result.success) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <h4 className="font-semibold text-red-900">Standardization Failed</h4>
        <p className="text-red-700 mt-2">{result.error_message}</p>
      </div>
    );
  }

  const hasStereoWarning = result.stereo_comparison?.warning;
  const massChangePercent = result.structure_comparison?.mass_change_percent || 0;
  const hasMassChange = Math.abs(massChangePercent) > 5;

  return (
    <div className="space-y-6">
      {/* Stereochemistry Warning Banner */}
      {hasStereoWarning && (
        <div className="bg-amber-50 border-l-4 border-amber-400 p-4 rounded-r-lg">
          <div className="flex items-start gap-3">
            <span className="text-amber-500 text-xl">!</span>
            <div>
              <h4 className="font-semibold text-amber-800">
                Stereochemistry Warning
              </h4>
              <p className="text-amber-700 text-sm mt-1">
                {result.stereo_comparison!.warning}
              </p>
              <div className="flex gap-4 mt-2 text-xs text-amber-600">
                <span>Before: {result.stereo_comparison!.before_count} defined stereocenters</span>
                <span>After: {result.stereo_comparison!.after_count} defined stereocenters</span>
              </div>
              {result.stereo_comparison!.double_bond_stereo_lost > 0 && (
                <p className="text-xs text-amber-600 mt-1">
                  E/Z stereo bonds lost: {result.stereo_comparison!.double_bond_stereo_lost}
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Mass Change Warning */}
      {hasMassChange && (
        <div className={`p-4 rounded-lg ${
          massChangePercent < 0 ? 'bg-blue-50 border border-blue-200' : 'bg-orange-50 border border-orange-200'
        }`}>
          <div className="flex items-center gap-2">
            <span className={massChangePercent < 0 ? 'text-blue-500' : 'text-orange-500'}>
              {massChangePercent < 0 ? '↓' : '↑'}
            </span>
            <span className={`font-medium ${
              massChangePercent < 0 ? 'text-blue-800' : 'text-orange-800'
            }`}>
              Mass changed by {massChangePercent.toFixed(1)}%
            </span>
          </div>
          {result.structure_comparison && (
            <p className="text-xs mt-1 text-gray-600">
              {result.structure_comparison.original_mw?.toFixed(2)} Da
              {' → '}
              {result.structure_comparison.standardized_mw?.toFixed(2)} Da
            </p>
          )}
        </div>
      )}

      {/* Side-by-side Comparison */}
      <div className="bg-white p-4 rounded-lg border border-gray-200">
        <h4 className="font-semibold text-gray-900 mb-4 text-center">
          Structure Comparison
        </h4>
        <ComparisonView
          originalSmiles={result.original_smiles}
          standardizedSmiles={result.standardized_smiles}
        />

        {/* Diff Summary */}
        {result.structure_comparison && result.structure_comparison.diff_summary.length > 0 && (
          <div className="mt-4 pt-4 border-t border-gray-100">
            <p className="text-xs text-gray-600 text-center">
              {result.structure_comparison.diff_summary.join(' | ')}
            </p>
          </div>
        )}
      </div>

      {/* Copy Button */}
      {result.standardized_smiles && (
        <div className="flex justify-center">
          <button
            onClick={handleCopySmiles}
            className={`px-4 py-2 rounded-lg font-medium transition-colors ${
              copied
                ? 'bg-yellow-100 dark:bg-yellow-900/30 text-amber-800 dark:text-yellow-400 border border-yellow-300 dark:border-yellow-700'
                : 'bg-[var(--color-primary)] hover:bg-[var(--color-primary-dark)] text-white'
            }`}
          >
            {copied ? '✓ Copied!' : 'Copy Standardized SMILES'}
          </button>
        </div>
      )}

      {/* Pipeline Steps */}
      <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
        <h4 className="font-semibold text-gray-900 mb-4">
          Standardization Pipeline
        </h4>
        <StepsList
          steps={result.steps_applied}
          checkerIssues={result.checker_issues}
          excludedFragments={result.excluded_fragments}
        />
      </div>
    </div>
  );
}
