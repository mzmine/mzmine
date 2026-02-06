import { useState, useRef, useEffect, ReactNode, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { cn } from '../../lib/utils';

interface TooltipProps {
  children: ReactNode;
  content: ReactNode;
  title?: string;
  position?: 'top' | 'bottom' | 'left' | 'right';
  delay?: number;
  maxWidth?: number;
  disabled?: boolean;
  className?: string;
}

/**
 * Simple tooltip using portal to avoid overflow clipping
 */
export function Tooltip({
  children,
  content,
  title,
  position = 'top',
  delay = 150,
  maxWidth = 280,
  disabled = false,
  className = '',
}: TooltipProps) {
  const [isVisible, setIsVisible] = useState(false);
  const [coords, setCoords] = useState({ top: 0, left: 0 });
  const [actualPosition, setActualPosition] = useState(position);
  const triggerRef = useRef<HTMLDivElement>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();

  const OFFSET = 8; // Gap between trigger and tooltip

  const calculatePosition = useCallback(() => {
    if (!triggerRef.current) return;

    const triggerRect = triggerRef.current.getBoundingClientRect();
    const tooltipWidth = maxWidth;
    const tooltipHeight = tooltipRef.current?.offsetHeight || 100;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const scrollY = window.scrollY;
    const scrollX = window.scrollX;

    let top = 0;
    let left = 0;
    let finalPosition = position;

    // Calculate initial position
    switch (position) {
      case 'top':
        top = triggerRect.top + scrollY - tooltipHeight - OFFSET;
        left = triggerRect.left + scrollX + triggerRect.width / 2 - tooltipWidth / 2;
        // If would overflow top, flip to bottom
        if (triggerRect.top - tooltipHeight - OFFSET < 0) {
          finalPosition = 'bottom';
          top = triggerRect.bottom + scrollY + OFFSET;
        }
        break;
      case 'bottom':
        top = triggerRect.bottom + scrollY + OFFSET;
        left = triggerRect.left + scrollX + triggerRect.width / 2 - tooltipWidth / 2;
        // If would overflow bottom, flip to top
        if (triggerRect.bottom + tooltipHeight + OFFSET > viewportHeight) {
          finalPosition = 'top';
          top = triggerRect.top + scrollY - tooltipHeight - OFFSET;
        }
        break;
      case 'left':
        top = triggerRect.top + scrollY + triggerRect.height / 2 - tooltipHeight / 2;
        left = triggerRect.left + scrollX - tooltipWidth - OFFSET;
        // If would overflow left, flip to right or bottom
        if (triggerRect.left - tooltipWidth - OFFSET < 0) {
          if (triggerRect.right + tooltipWidth + OFFSET < viewportWidth) {
            finalPosition = 'right';
            left = triggerRect.right + scrollX + OFFSET;
          } else {
            finalPosition = 'bottom';
            top = triggerRect.bottom + scrollY + OFFSET;
            left = triggerRect.left + scrollX + triggerRect.width / 2 - tooltipWidth / 2;
          }
        }
        break;
      case 'right':
        top = triggerRect.top + scrollY + triggerRect.height / 2 - tooltipHeight / 2;
        left = triggerRect.right + scrollX + OFFSET;
        // If would overflow right, flip to left or bottom
        if (triggerRect.right + tooltipWidth + OFFSET > viewportWidth) {
          if (triggerRect.left - tooltipWidth - OFFSET > 0) {
            finalPosition = 'left';
            left = triggerRect.left + scrollX - tooltipWidth - OFFSET;
          } else {
            finalPosition = 'bottom';
            top = triggerRect.bottom + scrollY + OFFSET;
            left = triggerRect.left + scrollX + triggerRect.width / 2 - tooltipWidth / 2;
          }
        }
        break;
    }

    // Clamp left to prevent horizontal overflow
    const minLeft = scrollX + 8;
    const maxLeft = scrollX + viewportWidth - tooltipWidth - 8;
    left = Math.max(minLeft, Math.min(maxLeft, left));

    // Clamp top to prevent vertical overflow
    const minTop = scrollY + 8;
    const maxTop = scrollY + viewportHeight - tooltipHeight - 8;
    top = Math.max(minTop, Math.min(maxTop, top));

    setCoords({ top, left });
    setActualPosition(finalPosition);
  }, [position, maxWidth]);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  // Recalculate position when visible
  useEffect(() => {
    if (isVisible) {
      calculatePosition();
      // Recalculate after a brief delay to get accurate tooltip height
      const timer = setTimeout(calculatePosition, 10);
      return () => clearTimeout(timer);
    }
  }, [isVisible, calculatePosition]);

  const show = () => {
    if (disabled) return;
    timeoutRef.current = setTimeout(() => {
      setIsVisible(true);
    }, delay);
  };

  const hide = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setIsVisible(false);
  };

  const arrowStyles: Record<string, string> = {
    top: 'left-1/2 -translate-x-1/2 top-full border-t-zinc-800 border-x-transparent border-b-transparent',
    bottom: 'left-1/2 -translate-x-1/2 bottom-full border-b-zinc-800 border-x-transparent border-t-transparent',
    left: 'top-1/2 -translate-y-1/2 left-full border-l-zinc-800 border-y-transparent border-r-transparent',
    right: 'top-1/2 -translate-y-1/2 right-full border-r-zinc-800 border-y-transparent border-l-transparent',
  };

  return (
    <div
      ref={triggerRef}
      className="relative inline-flex"
      onMouseEnter={show}
      onMouseLeave={hide}
      onFocus={show}
      onBlur={hide}
    >
      {children}

      {isVisible && createPortal(
        <div
          ref={tooltipRef}
          className={cn(
            'fixed z-[9999] px-3 py-2 rounded-lg shadow-xl',
            'bg-zinc-800 text-white text-sm',
            'whitespace-normal',
            'animate-in fade-in-0 zoom-in-95 duration-150',
            className
          )}
          style={{
            top: coords.top,
            left: coords.left,
            maxWidth,
            minWidth: 120,
            position: 'absolute',
          }}
          role="tooltip"
        >
          <div
            className={cn(
              'absolute w-0 h-0 border-[6px]',
              arrowStyles[actualPosition]
            )}
          />

          {title && (
            <div className="font-semibold text-white mb-1.5 pb-1.5 border-b border-white/20">
              {title}
            </div>
          )}
          <div className="text-zinc-200">{content}</div>
        </div>,
        document.body
      )}
    </div>
  );
}

/**
 * Info icon with tooltip - clearly visible information indicator
 * Use asSpan={true} when placing inside buttons or other interactive elements
 */
export function InfoTooltip({
  content,
  title,
  position = 'top',
  size = 'default',
  asSpan = false,
}: {
  content: ReactNode;
  title?: string;
  position?: 'top' | 'bottom' | 'left' | 'right';
  size?: 'small' | 'default';
  asSpan?: boolean;
}) {
  const isSmall = size === 'small';
  
  const className = cn(
    'inline-flex items-center justify-center rounded-full',
    'border border-[var(--color-text-muted)]/40',
    'bg-[var(--color-surface-sunken)]',
    'text-[var(--color-text-secondary)]',
    'hover:bg-[var(--color-primary)]/10',
    'hover:border-[var(--color-primary)]/50',
    'hover:text-[var(--color-primary)]',
    'transition-all duration-200 cursor-help',
    isSmall ? 'w-4 h-4' : 'w-[18px] h-[18px]'
  );

  const innerContent = (
    <span className={cn(
      'font-semibold leading-none',
      isSmall ? 'text-[9px]' : 'text-[11px]'
    )}>
      i
    </span>
  );

  return (
    <Tooltip content={content} title={title} position={position} maxWidth={280}>
      {asSpan ? (
        <span
          role="img"
          aria-label="More information"
          className={className}
        >
          {innerContent}
        </span>
      ) : (
        <button
          type="button"
          aria-label="More information"
          className={className}
        >
          {innerContent}
        </button>
      )}
    </Tooltip>
  );
}

/**
 * Calculation tooltip for explaining scores
 */
export function CalculationTooltip({
  children,
  calculation,
  interpretation,
  title,
  value,
  position = 'top',
}: {
  children: ReactNode;
  calculation: string;
  interpretation: string;
  title?: string;
  value?: string | number;
  position?: 'top' | 'bottom' | 'left' | 'right';
}) {
  const content = (
    <div className="space-y-2">
      {value !== undefined && (
        <div className="flex items-center gap-2 pb-2 border-b border-white/20">
          <span className="text-zinc-400 text-xs">Value:</span>
          <span className="font-mono font-semibold text-red-400">{value}</span>
        </div>
      )}
      <div>
        <span className="text-zinc-400 text-xs block mb-1">Calculation:</span>
        <code className="text-xs bg-black/30 px-2 py-1 rounded block">{calculation}</code>
      </div>
      <div>
        <span className="text-zinc-400 text-xs block mb-1">Meaning:</span>
        <span className="text-zinc-200">{interpretation}</span>
      </div>
    </div>
  );

  return (
    <Tooltip content={content} title={title} position={position} maxWidth={300}>
      <span className="cursor-help border-b border-dashed border-[var(--color-text-muted)] hover:border-[var(--color-primary)] transition-colors inline-flex items-center gap-1">
        {children}
        <svg className="w-3 h-3 text-[var(--color-text-muted)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3M12 17h.01" />
        </svg>
      </span>
    </Tooltip>
  );
}

export default Tooltip;
