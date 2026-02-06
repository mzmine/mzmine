import { useEffect, useRef, useMemo } from 'react';
import { motion } from 'framer-motion';
import { XCircle } from 'lucide-react';
import { ProcessingLogo } from './ProcessingLogo';
import { ClayButton } from '../ui/ClayButton';
import type { BatchProgress as BatchProgressType } from '../../types/batch';

interface BatchProgressProps {
  progress: BatchProgressType | null;
  isConnected: boolean;
  onCancel: () => void;
  onComplete: () => void;
}

/**
 * Progress display component with animated progress bar and ETA.
 */
export function BatchProgress({
  progress,
  isConnected,
  onCancel,
  onComplete,
}: BatchProgressProps) {
  // Use the actual progress value directly - let CSS handle the animation
  // This avoids the runaway requestAnimationFrame loop that was causing unresponsiveness
  const displayProgress = progress?.progress ?? 0;

  // Track if we've already called onComplete to prevent multiple calls
  const hasCompletedRef = useRef(false);

  // Call onComplete when status changes to complete (only once)
  useEffect(() => {
    if (progress?.status === 'complete' && !hasCompletedRef.current) {
      hasCompletedRef.current = true;
      onComplete();
    }
  }, [progress?.status, onComplete]);

  // Reset completion flag when job changes
  useEffect(() => {
    if (progress?.status === 'pending' || progress?.status === 'processing') {
      hasCompletedRef.current = false;
    }
  }, [progress?.status]);

  const formatETA = (seconds: number | null | undefined): string => {
    if (seconds === null || seconds === undefined || seconds <= 0) return 'Calculating...';

    if (seconds < 60) {
      return `${Math.ceil(seconds)} seconds`;
    }
    if (seconds < 3600) {
      const mins = Math.floor(seconds / 60);
      const secs = Math.ceil(seconds % 60);
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    }
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${hours}:${mins.toString().padStart(2, '0')}:00`;
  };

  // Memoize status strings to prevent unnecessary re-renders
  const statusTitle = useMemo(() => {
    switch (progress?.status) {
      case 'complete': return 'Processing Complete!';
      case 'failed': return 'Processing Failed';
      case 'cancelled': return 'Processing Cancelled';
      case 'pending': return 'Preparing...';
      default: return 'Processing Molecules';
    }
  }, [progress?.status]);

  const statusSubtitle = useMemo(() => {
    if (progress?.status === 'processing' && progress.processed > 0) {
      return `${progress.processed.toLocaleString()} of ${progress.total.toLocaleString()} molecules analyzed`;
    }
    if (progress?.status === 'complete') {
      return `Successfully processed ${progress.total.toLocaleString()} molecules`;
    }
    if (progress?.status === 'pending') {
      return 'Initializing batch job...';
    }
    return 'Connecting to server...';
  }, [progress?.status, progress?.processed, progress?.total]);

  const statusText = useMemo(() => {
    switch (progress?.status) {
      case 'pending':
        return 'Waiting to start...';
      case 'processing':
        return `Processing ${progress.processed} of ${progress.total} molecules...`;
      case 'complete':
        return 'Processing complete!';
      case 'failed':
        return `Failed: ${progress.error_message || 'Unknown error'}`;
      case 'cancelled':
        return 'Cancelled';
      default:
        return 'Connecting...';
    }
  }, [progress?.status, progress?.processed, progress?.total, progress?.error_message]);

  // Progress bar style with gradient
  const progressBarStyle = useMemo(() => {
    let background: string;
    if (progress?.status === 'complete') {
      background = 'linear-gradient(90deg, #F59E0B, #EAB308, #FBBF24)';
    } else if (progress?.status === 'failed') {
      background = 'linear-gradient(90deg, #EF4444, #DC2626)';
    } else {
      background = 'linear-gradient(90deg, #c41e3a, #e11d48, #d97706)';
    }
    return { background };
  }, [progress?.status]);

  return (
    <motion.div
      className="bg-[var(--color-surface-elevated)] rounded-2xl shadow-lg border border-[var(--color-border)] p-8 space-y-6 overflow-hidden relative"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* Animated Logo Section */}
      <motion.div
        className="flex flex-col items-center py-8"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <ProcessingLogo
          progress={displayProgress}
          status={progress?.status}
          className="w-56 h-56 mb-6"
        />

        {/* Status text below logo */}
        <div className="text-center">
          <h2 className="text-xl font-bold text-[var(--color-text-primary)] font-display mb-1">
            {statusTitle}
          </h2>
          <p className="text-[var(--color-text-secondary)] text-sm">
            {statusSubtitle}
          </p>
        </div>
      </motion.div>

      {/* Status header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)]">Batch Processing</h3>
        {/* Connection indicator */}
        <div className="flex items-center space-x-2">
          <span
            className={`w-2 h-2 rounded-full ${
              isConnected ? 'bg-yellow-500' : 'bg-[var(--color-text-muted)]'
            }`}
          />
          <span className="text-xs text-[var(--color-text-muted)]">
            {isConnected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
      </div>

      {/* Progress bar - use CSS transition instead of JS animation */}
      <div className="space-y-2">
        <div className="h-3 bg-[var(--color-surface-sunken)] rounded-full overflow-hidden relative">
          {/* Animated background shimmer */}
          {progress?.status === 'processing' && (
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent"
              animate={{ x: ['-100%', '100%'] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
            />
          )}
          {/* Progress fill with CSS transition for smooth animation */}
          <div
            className="h-full relative overflow-hidden transition-all duration-300 ease-out"
            style={{
              width: `${Math.max(0, Math.min(100, displayProgress))}%`,
              ...progressBarStyle,
            }}
          >
            {/* Shimmer overlay on progress */}
            {progress?.status === 'processing' && (
              <motion.div
                className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent"
                animate={{ x: ['-100%', '100%'] }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
              />
            )}
          </div>
        </div>

        <div className="flex justify-between text-sm">
          <span className="text-[var(--color-text-secondary)]">{statusText}</span>
          <span className="font-semibold text-[var(--color-text-primary)]">
            {Math.round(displayProgress)}%
          </span>
        </div>
      </div>

      {/* Stats row */}
      {progress && progress.status === 'processing' && (
        <div className="grid grid-cols-3 gap-4 py-4 px-2 border-t border-b border-[var(--color-border)] bg-gradient-to-r from-transparent via-[var(--color-surface-sunken)]/50 to-transparent rounded-lg">
          <div className="text-center p-3 rounded-lg bg-[var(--color-surface-elevated)] border border-[var(--color-border)]/50">
            <p className="text-2xl font-bold bg-gradient-to-r from-[#c41e3a] to-[#d97706] bg-clip-text text-transparent">
              {progress.processed.toLocaleString()}
            </p>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">Processed</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-[var(--color-surface-elevated)] border border-[var(--color-border)]/50">
            <p className="text-2xl font-bold text-[var(--color-text-primary)]">
              {(progress.total - progress.processed).toLocaleString()}
            </p>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">Remaining</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-[var(--color-surface-elevated)] border border-[var(--color-border)]/50">
            <p className="text-2xl font-bold text-amber-500 dark:text-amber-400">
              {formatETA(progress.eta_seconds)}
            </p>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">ETA</p>
          </div>
        </div>
      )}

      {/* Error message */}
      {progress?.status === 'failed' && progress.error_message && (
        <motion.div
          className="bg-gradient-to-r from-red-500/10 to-rose-500/10 dark:from-red-500/20 dark:to-rose-500/20 border border-red-500/30 rounded-xl p-4"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="flex items-start gap-3">
            <div className="w-8 h-8 rounded-full bg-red-500/20 flex items-center justify-center flex-shrink-0">
              <svg className="w-4 h-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <div>
              <p className="font-medium text-red-600 dark:text-red-400">Processing Failed</p>
              <p className="text-sm text-red-500/80 dark:text-red-400/80 mt-1">{progress.error_message}</p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Cancel button */}
      {progress?.status === 'processing' && (
        <ClayButton
          variant="danger"
          size="lg"
          onClick={onCancel}
          leftIcon={<XCircle className="w-5 h-5" />}
          className="w-full"
        >
          Cancel Processing
        </ClayButton>
      )}

      {/* Completion message */}
      {progress?.status === 'complete' && (
        <motion.div
          className="bg-gradient-to-r from-yellow-500/10 via-amber-500/10 to-orange-500/10 dark:from-yellow-500/20 dark:via-amber-500/20 dark:to-orange-500/20 border border-yellow-500/30 rounded-xl p-6 text-center"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.3 }}
        >
          <motion.div
            className="w-16 h-16 mx-auto mb-4 rounded-full bg-gradient-to-br from-yellow-400 to-amber-500 flex items-center justify-center shadow-lg shadow-yellow-500/30"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200, delay: 0.1 }}
          >
            <svg
              className="w-8 h-8 text-white"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={3}
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path d="M5 13l4 4L19 7" />
            </svg>
          </motion.div>
          <p className="text-amber-700 dark:text-yellow-400 font-semibold text-lg font-display">
            Successfully processed {progress.total.toLocaleString()} molecules!
          </p>
          <p className="text-amber-600 dark:text-yellow-500 text-sm mt-2">
            Loading results...
          </p>
        </motion.div>
      )}
    </motion.div>
  );
}
