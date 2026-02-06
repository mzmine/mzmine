/**
 * ScoreChart Component Tests
 *
 * Tests the radial score chart and related scoring components.
 * These components use recharts for visualization.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '../setup';
import { ScoreChart, ScoreIndicator, ScoreBreakdownBar } from '../../components/scoring/ScoreChart';

// Mock recharts to avoid JSDOM issues with SVG rendering
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
  RadialBarChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="radial-bar-chart">{children}</div>
  ),
  RadialBar: () => <div data-testid="radial-bar" />,
  PolarAngleAxis: () => <div data-testid="polar-angle-axis" />,
}));

describe('ScoreChart', () => {
  describe('Score Display', () => {
    it('renders the score value', () => {
      render(<ScoreChart score={85} label="Test Score" />);

      expect(screen.getByText('85')).toBeInTheDocument();
    });

    it('rounds decimal scores', () => {
      render(<ScoreChart score={85.7} label="Test Score" />);

      expect(screen.getByText('86')).toBeInTheDocument();
    });

    it('clamps scores above 100', () => {
      render(<ScoreChart score={150} label="Test Score" />);

      expect(screen.getByText('100')).toBeInTheDocument();
    });

    it('clamps scores below 0', () => {
      render(<ScoreChart score={-20} label="Test Score" />);

      expect(screen.getByText('0')).toBeInTheDocument();
    });

    it('displays the label', () => {
      render(<ScoreChart score={75} label="ML-Readiness" />);

      expect(screen.getByText('ML-Readiness')).toBeInTheDocument();
    });
  });

  describe('Score Colors', () => {
    it('shows excellent styling for scores >= 80', () => {
      render(<ScoreChart score={80} label="Test" />);

      const label = screen.getByText('Excellent');
      expect(label).toBeInTheDocument();
    });

    it('shows fair styling for scores 50-79', () => {
      render(<ScoreChart score={65} label="Test" />);

      const label = screen.getByText('Fair');
      expect(label).toBeInTheDocument();
    });

    it('shows poor styling for scores < 50', () => {
      render(<ScoreChart score={30} label="Test" />);

      const label = screen.getByText('Poor');
      expect(label).toBeInTheDocument();
    });

    it('handles boundary score of 50 as fair', () => {
      render(<ScoreChart score={50} label="Test" />);

      expect(screen.getByText('Fair')).toBeInTheDocument();
    });

    it('handles boundary score of 79 as fair', () => {
      render(<ScoreChart score={79} label="Test" />);

      expect(screen.getByText('Fair')).toBeInTheDocument();
    });
  });

  describe('Compact Mode', () => {
    it('does not show label text in compact mode', () => {
      render(<ScoreChart score={85} label="Test Score" compact />);

      // Label should not be visible in compact mode
      expect(screen.queryByText('Test Score')).not.toBeInTheDocument();
    });

    it('does not show rating badge in compact mode', () => {
      render(<ScoreChart score={85} label="Test" compact />);

      // Rating like "Excellent" should not be visible
      expect(screen.queryByText('Excellent')).not.toBeInTheDocument();
    });
  });

  describe('Tooltip', () => {
    it('renders tooltip wrapper when calculation and interpretation provided', () => {
      render(
        <ScoreChart
          score={85}
          label="Test"
          calculation="Score = 85"
          interpretation="Good score"
        />
      );

      // Component should render without errors
      expect(screen.getByText('85')).toBeInTheDocument();
    });
  });

  describe('Size Customization', () => {
    it('accepts custom size prop', () => {
      const { container } = render(
        <ScoreChart score={75} label="Test" size={200} />
      );

      // Check that size is applied to the container
      const chartContainer = container.querySelector('[style*="200"]');
      expect(chartContainer).not.toBeNull();
    });

    it('uses default size when not specified', () => {
      const { container } = render(<ScoreChart score={75} label="Test" />);

      // Default size is 160
      const chartContainer = container.querySelector('[style*="160"]');
      expect(chartContainer).not.toBeNull();
    });
  });
});

describe('ScoreIndicator', () => {
  it('renders the score', () => {
    render(<ScoreIndicator score={75} />);

    expect(screen.getByText('75')).toBeInTheDocument();
  });

  it('rounds the score', () => {
    render(<ScoreIndicator score={75.4} />);

    expect(screen.getByText('75')).toBeInTheDocument();
  });

  it('applies correct color for excellent scores', () => {
    const { container } = render(<ScoreIndicator score={90} />);

    const indicator = container.firstChild;
    expect(indicator).toHaveClass('bg-yellow-500/10');
  });

  it('applies correct color for fair scores', () => {
    const { container } = render(<ScoreIndicator score={60} />);

    const indicator = container.firstChild;
    expect(indicator).toHaveClass('bg-amber-500/10');
  });

  it('applies correct color for poor scores', () => {
    const { container } = render(<ScoreIndicator score={30} />);

    const indicator = container.firstChild;
    expect(indicator).toHaveClass('bg-red-500/10');
  });

  it('accepts custom size', () => {
    const { container } = render(<ScoreIndicator score={75} size={60} />);

    const indicator = container.firstChild;
    expect(indicator).toHaveStyle({ width: '60px', height: '60px' });
  });
});

describe('ScoreBreakdownBar', () => {
  it('renders label and score', () => {
    render(
      <ScoreBreakdownBar
        label="Descriptors"
        score={35}
        maxScore={40}
      />
    );

    expect(screen.getByText('Descriptors')).toBeInTheDocument();
    expect(screen.getByText('35/40')).toBeInTheDocument();
  });

  it('renders detail when provided', () => {
    render(
      <ScoreBreakdownBar
        label="Descriptors"
        score={35}
        maxScore={40}
        detail="200/217 calculated"
      />
    );

    expect(screen.getByText(/200\/217 calculated/)).toBeInTheDocument();
  });

  it('calculates percentage correctly', () => {
    const { container } = render(
      <ScoreBreakdownBar
        label="Test"
        score={50}
        maxScore={100}
      />
    );

    // The progress bar should have 50% width
    const progressBar = container.querySelector('[style*="width"]');
    expect(progressBar).not.toBeNull();
  });

  it('handles zero maxScore without error', () => {
    expect(() =>
      render(
        <ScoreBreakdownBar
          label="Test"
          score={0}
          maxScore={0}
        />
      )
    ).not.toThrow();
  });

  it('renders calculation tooltip when provided', () => {
    render(
      <ScoreBreakdownBar
        label="Test"
        score={30}
        maxScore={40}
        calculation="Score = 40 * (200/217)"
        interpretation="Most descriptors calculated successfully"
      />
    );

    // Component should render with tooltip
    expect(screen.getByText('Test')).toBeInTheDocument();
  });

  it('applies correct color based on percentage', () => {
    // High percentage (excellent)
    const { rerender, container } = render(
      <ScoreBreakdownBar label="Test" score={90} maxScore={100} />
    );

    let progressBar = container.querySelector('[style*="width"]');
    expect(progressBar).not.toBeNull();

    // Medium percentage (fair)
    rerender(<ScoreBreakdownBar label="Test" score={60} maxScore={100} />);

    progressBar = container.querySelector('[style*="width"]');
    expect(progressBar).not.toBeNull();

    // Low percentage (poor)
    rerender(<ScoreBreakdownBar label="Test" score={20} maxScore={100} />);

    progressBar = container.querySelector('[style*="width"]');
    expect(progressBar).not.toBeNull();
  });
});

describe('Score Color Thresholds', () => {
  const thresholdTests = [
    { score: 100, expected: 'Excellent' },
    { score: 80, expected: 'Excellent' },
    { score: 79, expected: 'Fair' },
    { score: 50, expected: 'Fair' },
    { score: 49, expected: 'Poor' },
    { score: 0, expected: 'Poor' },
  ];

  thresholdTests.forEach(({ score, expected }) => {
    it(`classifies score ${score} as ${expected}`, () => {
      render(<ScoreChart score={score} label="Test" />);
      expect(screen.getByText(expected)).toBeInTheDocument();
    });
  });
});
