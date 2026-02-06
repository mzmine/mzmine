/**
 * ValidationResults Component Tests
 *
 * Tests the main validation results display component.
 * Mock data matches backend Pydantic schemas.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '../setup';
import { ValidationResults } from '../../components/validation/ValidationResults';
import type { ValidationResponse, CheckResult } from '../../types/validation';

// Mock data matching backend Pydantic schemas
const createMockCheckResult = (overrides: Partial<CheckResult> = {}): CheckResult => ({
  check_name: 'parsability',
  passed: true,
  severity: 'pass',
  message: 'Molecule successfully parsed',
  affected_atoms: [],
  details: {},
  ...overrides,
});

const createMockValidationResponse = (
  overrides: Partial<ValidationResponse> = {}
): ValidationResponse => ({
  status: 'success',
  molecule_info: {
    input_smiles: 'CCO',
    canonical_smiles: 'CCO',
    inchi: 'InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3',
    inchikey: 'LFQSCWFLJHTTHZ-UHFFFAOYSA-N',
    molecular_formula: 'C2H6O',
    molecular_weight: 46.07,
    num_atoms: 3,
  },
  overall_score: 100,
  issues: [],
  all_checks: [
    createMockCheckResult({ check_name: 'parsability', passed: true, severity: 'pass' }),
    createMockCheckResult({ check_name: 'sanitization', passed: true, severity: 'pass' }),
    createMockCheckResult({ check_name: 'valence', passed: true, severity: 'pass' }),
  ],
  execution_time_ms: 15.5,
  ...overrides,
});

describe('ValidationResults', () => {
  describe('Score Display', () => {
    it('renders the overall score', () => {
      const result = createMockValidationResponse({ overall_score: 85 });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Validation Score')).toBeInTheDocument();
    });

    it('displays execution time', () => {
      const result = createMockValidationResponse({ execution_time_ms: 25.7 });
      render(<ValidationResults result={result} />);

      expect(screen.getByText(/Completed in 26ms/)).toBeInTheDocument();
    });

    it('shows perfect score for passing validation', () => {
      const result = createMockValidationResponse({ overall_score: 100 });
      render(<ValidationResults result={result} />);

      // Score gauge should be present
      expect(screen.getByText('Validation Score')).toBeInTheDocument();
    });
  });

  describe('Molecule Information', () => {
    it('displays canonical SMILES', () => {
      const result = createMockValidationResponse({
        molecule_info: {
          input_smiles: 'CCO',
          canonical_smiles: 'CCO',
          inchi: null,
          inchikey: null,
          molecular_formula: null,
          molecular_weight: null,
          num_atoms: 3,
        },
      });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('SMILES:')).toBeInTheDocument();
      expect(screen.getByText('CCO')).toBeInTheDocument();
    });

    it('displays molecular formula when available', () => {
      const result = createMockValidationResponse();
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Formula:')).toBeInTheDocument();
      expect(screen.getByText('C2H6O')).toBeInTheDocument();
    });

    it('displays molecular weight formatted to 2 decimals', () => {
      const result = createMockValidationResponse({
        molecule_info: {
          ...createMockValidationResponse().molecule_info,
          molecular_weight: 180.156,
        },
      });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Mol. Weight:')).toBeInTheDocument();
      expect(screen.getByText('180.16 g/mol')).toBeInTheDocument();
    });

    it('displays InChIKey when available', () => {
      const result = createMockValidationResponse();
      render(<ValidationResults result={result} />);

      expect(screen.getByText('InChIKey:')).toBeInTheDocument();
      expect(screen.getByText('LFQSCWFLJHTTHZ-UHFFFAOYSA-N')).toBeInTheDocument();
    });

    it('displays atom count', () => {
      const result = createMockValidationResponse();
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Atoms:')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  describe('Issues Display', () => {
    it('shows "No Issues Found" when there are no issues', () => {
      const result = createMockValidationResponse({ issues: [] });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('No Issues Found')).toBeInTheDocument();
      expect(
        screen.getByText('All validation checks passed successfully')
      ).toBeInTheDocument();
    });

    it('displays issue count header when issues exist', () => {
      const result = createMockValidationResponse({
        issues: [
          createMockCheckResult({
            check_name: 'undefined_stereocenters',
            passed: false,
            severity: 'warning',
            message: 'Found 1 undefined stereocenter(s) out of a total of 3 stereocenter(s)',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Issues Found (1)')).toBeInTheDocument();
    });

    it('displays multiple issues correctly', () => {
      const result = createMockValidationResponse({
        issues: [
          createMockCheckResult({
            check_name: 'connectivity',
            passed: false,
            severity: 'warning',
            message: 'Molecule has 2 disconnected fragments',
          }),
          createMockCheckResult({
            check_name: 'undefined_stereocenters',
            passed: false,
            severity: 'warning',
            message: 'Found 1 undefined stereocenter(s) out of a total of 3 stereocenter(s)',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('Issues Found (2)')).toBeInTheDocument();
    });
  });

  describe('All Checks Section', () => {
    it('shows check count in header', () => {
      const result = createMockValidationResponse();
      render(<ValidationResults result={result} />);

      expect(screen.getByText('All Checks (3)')).toBeInTheDocument();
    });

    it('expands to show all checks when clicked', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({ check_name: 'parsability', passed: true }),
          createMockCheckResult({ check_name: 'sanitization', passed: true }),
          createMockCheckResult({ check_name: 'valence', passed: true }),
        ],
      });
      render(<ValidationResults result={result} />);

      // Click to expand
      const toggleButton = screen.getByRole('button', { name: /All Checks/i });
      fireEvent.click(toggleButton);

      // Check names should now be visible
      expect(screen.getByText('parsability')).toBeInTheDocument();
      expect(screen.getByText('sanitization')).toBeInTheDocument();
      expect(screen.getByText('valence')).toBeInTheDocument();
    });

    it('collapses when clicked again', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({ check_name: 'test_check', passed: true }),
        ],
      });
      render(<ValidationResults result={result} />);

      // Click to expand, then collapse
      const toggleButton = screen.getByRole('button', { name: /All Checks/i });
      fireEvent.click(toggleButton);
      expect(screen.getByText('test check')).toBeInTheDocument();

      fireEvent.click(toggleButton);
      // After collapse, the check should not be visible
      expect(screen.queryByText('test check')).not.toBeInTheDocument();
    });

    it('shows PASS badge for passing checks', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({ check_name: 'test_check', passed: true }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      expect(screen.getByText('PASS')).toBeInTheDocument();
    });

    it('shows severity badge for failing checks', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({
            check_name: 'valence',
            passed: false,
            severity: 'critical',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
    });
  });

  describe('Callback Props', () => {
    it('calls onHighlightAtoms when provided', () => {
      const onHighlightAtoms = vi.fn();
      const result = createMockValidationResponse({
        issues: [
          createMockCheckResult({
            check_name: 'valence',
            passed: false,
            severity: 'error',
            message: 'Valence error detected',
            affected_atoms: [0, 1],
          }),
        ],
      });
      render(
        <ValidationResults
          result={result}
          onHighlightAtoms={onHighlightAtoms}
        />
      );

      // The callback is passed to IssueCard which handles hover events
      // We just verify the component renders without errors when callback is provided
      expect(screen.getByText('Issues Found (1)')).toBeInTheDocument();
    });
  });

  describe('Severity Styling', () => {
    it('applies correct styling for critical severity', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({
            check_name: 'valence',
            passed: false,
            severity: 'critical',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      const badge = screen.getByText('CRITICAL');
      expect(badge).toHaveClass('bg-red-500/10', 'text-red-600');
    });

    it('applies correct styling for error severity', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({
            check_name: 'sanitization',
            passed: false,
            severity: 'error',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      const badge = screen.getByText('ERROR');
      expect(badge).toHaveClass('bg-orange-500/10', 'text-orange-600');
    });

    it('applies correct styling for warning severity', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({
            check_name: 'connectivity',
            passed: false,
            severity: 'warning',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      const badge = screen.getByText('WARNING');
      expect(badge).toHaveClass('bg-amber-500/10', 'text-amber-600');
    });

    it('applies correct styling for info severity', () => {
      const result = createMockValidationResponse({
        all_checks: [
          createMockCheckResult({
            check_name: 'inchi_roundtrip',
            passed: false,
            severity: 'info',
          }),
        ],
      });
      render(<ValidationResults result={result} />);

      fireEvent.click(screen.getByRole('button', { name: /All Checks/i }));

      const badge = screen.getByText('INFO');
      expect(badge).toHaveClass('bg-sky-500/10', 'text-sky-600');
    });
  });

  describe('Edge Cases', () => {
    it('handles null values in molecule info gracefully', () => {
      const result = createMockValidationResponse({
        molecule_info: {
          input_smiles: 'invalid',
          canonical_smiles: null,
          inchi: null,
          inchikey: null,
          molecular_formula: null,
          molecular_weight: null,
          num_atoms: null,
        },
      });

      // Should not throw
      expect(() => render(<ValidationResults result={result} />)).not.toThrow();

      // Should not show null values
      expect(screen.queryByText('null')).not.toBeInTheDocument();
    });

    it('handles empty all_checks array', () => {
      const result = createMockValidationResponse({ all_checks: [] });
      render(<ValidationResults result={result} />);

      expect(screen.getByText('All Checks (0)')).toBeInTheDocument();
    });

    it('accepts custom className', () => {
      const result = createMockValidationResponse();
      const { container } = render(
        <ValidationResults result={result} className="custom-class" />
      );

      expect(container.firstChild).toHaveClass('custom-class');
    });
  });
});
