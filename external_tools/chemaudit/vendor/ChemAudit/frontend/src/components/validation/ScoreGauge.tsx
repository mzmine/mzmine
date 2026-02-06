import { useState, useEffect, useRef } from 'react';
import { RadialBarChart, RadialBar, ResponsiveContainer, PolarAngleAxis } from 'recharts';
import { CalculationTooltip } from '../ui/Tooltip';
import { cn } from '../../lib/utils';
import { useThemeContext } from '../../contexts/ThemeContext';

interface ScoreGaugeProps {
  score: number;
  size?: number;
  className?: string;
  showCalculation?: boolean;
}

/**
 * Circular score gauge using recharts RadialBarChart.
 * Displays validation score with color-coded feedback and optional calculation tooltip.
 */
export function ScoreGauge({ score, size = 140, className = '', showCalculation = true }: ScoreGaugeProps) {
  const { isDark } = useThemeContext();
  const containerRef = useRef<HTMLDivElement>(null);
  const [isReady, setIsReady] = useState(false);

  // Delay chart render until container has valid dimensions
  useEffect(() => {
    const checkDimensions = () => {
      if (containerRef.current) {
        const { offsetWidth, offsetHeight } = containerRef.current;
        if (offsetWidth > 0 && offsetHeight > 0) {
          setIsReady(true);
        }
      }
    };
    
    checkDimensions();
    const timer = setTimeout(checkDimensions, 50);
    return () => clearTimeout(timer);
  }, []);

  // Clamp score to 0-100
  const clampedScore = Math.max(0, Math.min(100, score));

  // Determine color based on score
  const getColor = (score: number) => {
    if (score >= 80) return {
      fill: '#b45309',
      text: 'text-amber-600 dark:text-yellow-400',
      bg: 'bg-yellow-500/10 dark:bg-yellow-400/15',
      label: 'Excellent',
      gradientId: 'gaugeExcellent',
    };
    if (score >= 50) return {
      fill: '#d97706',
      text: 'text-amber-600 dark:text-amber-400',
      bg: 'bg-amber-500/10 dark:bg-amber-400/15',
      label: 'Fair',
      gradientId: 'gaugeFair',
    };
    return {
      fill: '#dc2626',
      text: 'text-red-600 dark:text-red-400',
      bg: 'bg-red-500/10 dark:bg-red-400/15',
      label: 'Poor',
      gradientId: 'gaugePoor',
    };
  };

  const color = getColor(clampedScore);
  const backgroundFill = isDark ? '#374151' : '#e5e7eb';

  const data = [
    {
      name: 'Validation Score',
      value: clampedScore,
      fill: `url(#${color.gradientId})`,
    },
  ];

  const calculation = `Score = 100 - (CRITICAL * 50 + ERROR * 20 + WARNING * 5)
Clamped to range 0-100`;

  // Determine interpretation based on score thresholds
  let interpretation: string;
  if (clampedScore >= 80) {
    interpretation = 'This molecule passes all critical validation checks and has minimal issues. It is suitable for most applications.';
  } else if (clampedScore >= 50) {
    interpretation = 'This molecule has some validation issues that may need attention. Review the warnings and errors below.';
  } else {
    interpretation = 'This molecule has significant validation problems. Critical issues must be resolved before use.';
  }

  const chartContent = (
    <div ref={containerRef} className={cn('relative', className)} style={{ width: size, height: size }}>
      {/* SVG Gradient Definitions */}
      <svg width="0" height="0" className="absolute">
        <defs>
          <linearGradient id="gaugeExcellent" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={isDark ? '#fde68a' : '#fcd34d'} />
            <stop offset="100%" stopColor={isDark ? '#fbbf24' : '#b45309'} />
          </linearGradient>
          <linearGradient id="gaugeFair" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={isDark ? '#fcd34d' : '#fbbf24'} />
            <stop offset="100%" stopColor={isDark ? '#f59e0b' : '#d97706'} />
          </linearGradient>
          <linearGradient id="gaugePoor" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={isDark ? '#fca5a5' : '#f87171'} />
            <stop offset="100%" stopColor={isDark ? '#ef4444' : '#dc2626'} />
          </linearGradient>
        </defs>
      </svg>

      {/* Chart - only render when container has valid dimensions */}
      {isReady && (
        <ResponsiveContainer width="100%" height="100%" minWidth={1} minHeight={1}>
          <RadialBarChart
            innerRadius="65%"
            outerRadius="100%"
            data={data}
            startAngle={90}
            endAngle={-270}
            barSize={12}
          >
            <PolarAngleAxis
              type="number"
              domain={[0, 100]}
              angleAxisId={0}
              tick={false}
            />
            <RadialBar
              background={{ fill: backgroundFill }}
              dataKey="value"
              cornerRadius={10}
              animationDuration={1000}
              animationEasing="ease-out"
            />
          </RadialBarChart>
        </ResponsiveContainer>
      )}

      {/* Score text in center */}
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className={cn('text-3xl font-bold', color.text)}>
          {Math.round(clampedScore)}
        </span>
        <span className="text-xs text-text-muted uppercase tracking-wide">Score</span>
      </div>
    </div>
  );

  if (showCalculation) {
    return (
      <div className="flex flex-col items-center">
        <CalculationTooltip
          calculation={calculation}
          interpretation={interpretation}
          title="Validation Score"
          value={`${Math.round(clampedScore)}/100`}
          position="top"
        >
          {chartContent}
        </CalculationTooltip>
        <div className={cn('mt-3 px-4 py-1.5 rounded-full', color.bg)}>
          <span className={cn('text-sm font-medium', color.text)}>{color.label}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center">
      {chartContent}
      <div className={cn('mt-3 px-4 py-1.5 rounded-full', color.bg)}>
        <span className={cn('text-sm font-medium', color.text)}>{color.label}</span>
      </div>
    </div>
  );
}
