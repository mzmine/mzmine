import type { ChangeEvent, ClipboardEvent, KeyboardEvent, ReactElement } from 'react';
import { useHotkeys } from 'react-hotkeys-hook';
import { cn } from '../../lib/utils';

interface StructureInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit?: () => void;
  disabled?: boolean;
  placeholder?: string;
}

const MOL_BLOCK_PATTERN = /V[23]000|M\s{2}END/i;

const isMac =
  typeof navigator !== 'undefined' && /Mac|iPhone|iPad|iPod/.test(navigator.platform);
const modifierKey = isMac ? 'Cmd' : 'Ctrl';

/** Text input for SMILES, InChI, or MOL block with keyboard shortcuts for validation. */
export function StructureInput({
  value,
  onChange,
  onSubmit,
  disabled = false,
  placeholder = 'Enter SMILES, InChI, or paste MDL Mol file...',
}: StructureInputProps): ReactElement {
  useHotkeys(
    'ctrl+enter, meta+enter',
    (e) => {
      e.preventDefault();
      onSubmit?.();
    },
    {
      enableOnFormTags: ['TEXTAREA'],
      enabled: !disabled && !!onSubmit,
    },
    [onSubmit, disabled],
  );

  function handleChange(e: ChangeEvent<HTMLTextAreaElement>): void {
    onChange(e.target.value);
  }

  function handlePaste(e: ClipboardEvent<HTMLTextAreaElement>): void {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text');
    const cleaned = MOL_BLOCK_PATTERN.test(pasted)
      ? pasted
      : pasted.replace(/[\r\n\t]+/g, '').trim();
    onChange(cleaned);
  }

  function handleKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey && onSubmit) {
      e.preventDefault();
      onSubmit();
    }
  }

  return (
    <div className="space-y-2">
      <textarea
        value={value}
        onChange={handleChange}
        onPaste={handlePaste}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        placeholder={placeholder}
        className={cn(
          'w-full h-24 px-4 py-3 rounded-xl font-mono text-sm resize-none',
          'bg-[var(--color-surface-elevated)] dark:bg-[var(--color-surface-sunken)]',
          'border border-[var(--color-border-strong)]',
          'text-[var(--color-text-primary)]',
          'placeholder:text-[var(--color-text-muted)]',
          'focus:outline-none focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--glow-primary)]',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          'transition-all duration-200',
        )}
        spellCheck={false}
      />
      <div className="flex items-center justify-between text-xs text-[var(--color-text-muted)]">
        <span>Press {modifierKey}+Enter to validate, Shift+Enter for new line</span>
        <span>{value.length} chars</span>
      </div>
    </div>
  );
}
