import { useState, useCallback, useRef } from 'react';
import { Upload, X, FileSpreadsheet, Database, ChevronDown, AlertCircle, CheckCircle2, Shield, FlaskConical } from 'lucide-react';
import { batchApi } from '../../services/api';
import { useLimits } from '../../context/ConfigContext';
import { ClayButton } from '../ui/ClayButton';
import type { CSVColumnsResponse } from '../../types/batch';

interface BatchUploadProps {
  onUploadSuccess: (jobId: string, totalMolecules: number) => void;
  onUploadError: (error: string) => void;
  disabled?: boolean;
}

// Threshold for showing confirmation before processing
const LARGE_FILE_THRESHOLD = 1000;

// Supported text-based formats for delimited data (CSV, TSV, TXT)
const TEXT_FORMATS = ['.csv', '.tsv', '.txt'];

/**
 * Check if a filename has a text-based delimited format extension.
 */
const isTextFormat = (filename: string): boolean => {
  const lower = filename.toLowerCase();
  return TEXT_FORMATS.some(ext => lower.endsWith(ext));
};

/**
 * File upload component with drag-and-drop support.
 * Accepts SDF and delimited text files (CSV, TSV, TXT), allows column selection for text files.
 * Shows preview and confirmation for larger files.
 */
export function BatchUpload({
  onUploadSuccess,
  onUploadError,
  disabled = false,
}: BatchUploadProps) {
  const limits = useLimits();
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isExtracting, setIsExtracting] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [csvColumns, setCsvColumns] = useState<CSVColumnsResponse | null>(null);
  const [selectedSmilesColumn, setSelectedSmilesColumn] = useState<string>('');
  const [selectedNameColumn, setSelectedNameColumn] = useState<string>('');
  const [includeExtendedSafety, setIncludeExtendedSafety] = useState(false);
  const [includeChemblAlerts, setIncludeChemblAlerts] = useState(false);
  const [includeStandardization, setIncludeStandardization] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const isLargeFile = csvColumns && csvColumns.row_count_estimate > LARGE_FILE_THRESHOLD;
  const isSDF = selectedFile?.name.toLowerCase().endsWith('.sdf');

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const validateFile = useCallback((file: File): string | null => {
    const name = file.name.toLowerCase();
    if (!name.endsWith('.sdf') && !isTextFormat(name)) {
      return 'Invalid file type. Please upload an SDF, CSV, TSV, or TXT file.';
    }
    if (file.size > limits.max_file_size_bytes) {
      return `File too large. Maximum size is ${limits.max_file_size_mb}MB.`;
    }
    return null;
  }, [limits.max_file_size_bytes, limits.max_file_size_mb]);

  const processFile = useCallback(async (file: File) => {
    const error = validateFile(file);
    if (error) {
      onUploadError(error);
      return;
    }

    setSelectedFile(file);
    setCsvColumns(null);
    setSelectedSmilesColumn('');
    setSelectedNameColumn('');

    // If delimited text file (CSV, TSV, TXT), detect columns locally (no server call)
    if (isTextFormat(file.name)) {
      setIsAnalyzing(true);
      try {
        // Use local detection - reads only first 50KB, instant
        const columns = await batchApi.detectColumnsLocal(file);
        setCsvColumns(columns);
        setSelectedSmilesColumn(columns.suggested_smiles || columns.columns[0] || '');
        setSelectedNameColumn(columns.suggested_name || '');
      } catch (e: any) {
        const errorMessage = e.message || 'Failed to read file columns';
        onUploadError(errorMessage);
        setSelectedFile(null);
      } finally {
        setIsAnalyzing(false);
      }
    }
  }, [onUploadError, validateFile]);

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragging(false);

      if (disabled || isUploading || isAnalyzing || isExtracting) return;

      const files = e.dataTransfer.files;
      if (files.length > 0) {
        await processFile(files[0]);
      }
    },
    [disabled, isUploading, isAnalyzing, isExtracting, processFile]
  );

  const handleFileSelect = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files;
      if (files && files.length > 0) {
        await processFile(files[0]);
      }
    },
    [processFile]
  );

  const handleUpload = async () => {
    if (!selectedFile || isUploading || isExtracting) return;

    // For CSV, ensure SMILES column is selected
    if (csvColumns && !selectedSmilesColumn) {
      onUploadError('Please select the SMILES column');
      return;
    }

    try {
      let fileToUpload = selectedFile;
      let smilesColForUpload = selectedSmilesColumn;
      let nameColForUpload = selectedNameColumn;

      // For small CSV files with many columns, extract only selected columns
      // For large files (> 5MB), skip extraction - faster to upload directly
      const shouldExtract = csvColumns &&
        selectedSmilesColumn &&
        csvColumns.columns.length > 3 &&
        selectedFile.size < 5 * 1024 * 1024; // 5MB threshold

      if (shouldExtract) {
        setIsExtracting(true);
        try {
          fileToUpload = await batchApi.extractColumns(
            selectedFile,
            selectedSmilesColumn,
            selectedNameColumn || undefined
          );
          // After successful extraction, columns are standardized
          smilesColForUpload = 'SMILES';
          nameColForUpload = selectedNameColumn ? 'Name' : '';
        } catch (extractError: any) {
          // If extraction fails, fall back to full file upload with original column names
          console.warn('Column extraction failed, uploading full file:', extractError);
          fileToUpload = selectedFile;
          smilesColForUpload = selectedSmilesColumn;
          nameColForUpload = selectedNameColumn;
        } finally {
          setIsExtracting(false);
        }
      }

      setIsUploading(true);
      setUploadProgress(0);
      const response = await batchApi.uploadBatch(
        fileToUpload,
        csvColumns ? smilesColForUpload : undefined,
        csvColumns && nameColForUpload ? nameColForUpload : undefined,
        (progress) => setUploadProgress(progress),
        {
          includeExtended: includeExtendedSafety,
          includeChembl: includeChemblAlerts,
          includeStandardization: includeStandardization,
        }
      );
      onUploadSuccess(response.job_id, response.total_molecules);
    } catch (e: any) {
      const errorMessage =
        e.response?.data?.detail || e.message || 'Upload failed';
      onUploadError(errorMessage);
    } finally {
      setIsExtracting(false);
      setIsUploading(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setCsvColumns(null);
    setSelectedSmilesColumn('');
    setSelectedNameColumn('');
    setIsExtracting(false);
    setUploadProgress(0);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-4">
      {/* Drop zone */}
      {!selectedFile && (
        <div
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}
          className={`
            border-2 border-dashed rounded-xl p-8 text-center cursor-pointer
            transition-all duration-200
            ${isDragging
              ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/10 scale-[1.02]'
              : 'border-[var(--color-border-strong)] hover:border-[var(--color-text-muted)] hover:bg-[var(--color-surface-sunken)]/50'}
            ${disabled || isUploading ? 'opacity-50 cursor-not-allowed' : ''}
          `}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".sdf,.csv,.tsv,.txt"
            onChange={handleFileSelect}
            className="hidden"
            disabled={disabled || isUploading}
          />

          <div className="text-[var(--color-text-secondary)]">
            <div className="mx-auto w-16 h-16 rounded-2xl bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-accent)]/10 flex items-center justify-center mb-4">
              <Upload className="w-8 h-8 text-[var(--color-primary)]" />
            </div>
            <p className="text-lg font-medium text-[var(--color-text-primary)]">
              {isDragging ? 'Drop file here' : 'Drop file here or click to browse'}
            </p>
            <p className="text-sm text-[var(--color-text-muted)] mt-2">
              Supports <span className="font-medium">SDF</span>, <span className="font-medium">CSV</span>, <span className="font-medium">TSV</span>, and <span className="font-medium">TXT</span> files
            </p>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">
              Up to {limits.max_batch_size.toLocaleString()} molecules • Max {limits.max_file_size_mb}MB
            </p>
          </div>
        </div>
      )}

      {/* Analyzing indicator */}
      {isAnalyzing && (
        <div className="bg-[var(--color-surface-sunken)] rounded-xl p-6 text-center">
          <div className="animate-spin w-8 h-8 border-2 border-[var(--color-primary)] border-t-transparent rounded-full mx-auto mb-3" />
          <p className="text-[var(--color-text-secondary)]">Analyzing file...</p>
        </div>
      )}

      {/* Selected file info */}
      {selectedFile && !isAnalyzing && (
        <div className="bg-[var(--color-surface-elevated)] rounded-xl border border-[var(--color-border)] overflow-hidden">
          {/* File header */}
          <div className="p-4 border-b border-[var(--color-border)] bg-[var(--color-surface-sunken)]/50">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-accent)]/10 flex items-center justify-center">
                  {isSDF ? (
                    <Database className="w-5 h-5 text-[var(--color-primary)]" />
                  ) : (
                    <FileSpreadsheet className="w-5 h-5 text-[var(--color-primary)]" />
                  )}
                </div>
                <div>
                  <p className="font-medium text-[var(--color-text-primary)]">{selectedFile.name}</p>
                  <p className="text-sm text-[var(--color-text-muted)]">
                    {formatFileSize(selectedFile.size)}
                    {csvColumns && (
                      <>
                        {' • '}
                        <span className="text-[var(--color-primary)] font-medium">
                          ~{csvColumns.row_count_estimate.toLocaleString()} molecules
                        </span>
                        {' • '}
                        {csvColumns.columns.length} columns
                      </>
                    )}
                  </p>
                </div>
              </div>
              <ClayButton
                variant="ghost"
                size="icon"
                onClick={handleReset}
                disabled={isUploading}
              >
                <X className="h-5 w-5" />
              </ClayButton>
            </div>
          </div>

          {/* CSV column selection */}
          {csvColumns && (
            <div className="p-4 space-y-4">
              {/* Help text */}
              <div className="flex items-start gap-2 p-3 rounded-lg bg-[var(--color-surface-sunken)]">
                <AlertCircle className="w-4 h-4 text-[var(--color-primary)] mt-0.5 flex-shrink-0" />
                <div className="text-sm text-[var(--color-text-secondary)]">
                  <p>Select the columns that contain your molecular data:</p>
                  <ul className="mt-1 ml-4 list-disc text-xs text-[var(--color-text-muted)]">
                    <li><strong>SMILES column</strong> - Required: Contains the molecular structure</li>
                    <li><strong>Name/ID column</strong> - Optional: Contains molecule identifiers</li>
                  </ul>
                </div>
              </div>

              {/* SMILES column selector */}
              <div>
                <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">
                  SMILES Column <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <select
                    value={selectedSmilesColumn}
                    onChange={(e) => setSelectedSmilesColumn(e.target.value)}
                    disabled={isUploading}
                    className="w-full appearance-none border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded-lg px-4 py-2.5 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
                  >
                    <option value="">Select SMILES column...</option>
                    {csvColumns.columns.map((col) => (
                      <option key={col} value={col}>
                        {col}
                        {col === csvColumns.suggested_smiles ? ' (suggested)' : ''}
                        {csvColumns.column_samples[col] ? ` - e.g., "${csvColumns.column_samples[col]}"` : ''}
                      </option>
                    ))}
                  </select>
                  <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-muted)] pointer-events-none" />
                </div>
                {selectedSmilesColumn && csvColumns.column_samples[selectedSmilesColumn] && (
                  <p className="mt-1 text-xs text-[var(--color-text-muted)]">
                    Sample: <code className="bg-[var(--color-surface-sunken)] px-1 py-0.5 rounded">{csvColumns.column_samples[selectedSmilesColumn]}</code>
                  </p>
                )}
              </div>

              {/* Name column selector */}
              <div>
                <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">
                  Name/ID Column <span className="text-[var(--color-text-muted)] font-normal">(optional)</span>
                </label>
                <div className="relative">
                  <select
                    value={selectedNameColumn}
                    onChange={(e) => setSelectedNameColumn(e.target.value)}
                    disabled={isUploading}
                    className="w-full appearance-none border border-[var(--color-border)] bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] rounded-lg px-4 py-2.5 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
                  >
                    <option value="">Auto-detect or none</option>
                    {csvColumns.columns
                      .filter(col => col !== selectedSmilesColumn)
                      .map((col) => (
                        <option key={col} value={col}>
                          {col}
                          {col === csvColumns.suggested_name ? ' (suggested)' : ''}
                          {csvColumns.column_samples[col] ? ` - e.g., "${csvColumns.column_samples[col]}"` : ''}
                        </option>
                      ))}
                  </select>
                  <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-muted)] pointer-events-none" />
                </div>
                {selectedNameColumn && csvColumns.column_samples[selectedNameColumn] && (
                  <p className="mt-1 text-xs text-[var(--color-text-muted)]">
                    Sample: <code className="bg-[var(--color-surface-sunken)] px-1 py-0.5 rounded">{csvColumns.column_samples[selectedNameColumn]}</code>
                  </p>
                )}
              </div>

              {/* Large file warning */}
              {isLargeFile && (
                <div className="flex items-start gap-2 p-3 rounded-lg bg-amber-500/10 border border-amber-500/20">
                  <AlertCircle className="w-4 h-4 text-amber-500 mt-0.5 flex-shrink-0" />
                  <div className="text-sm">
                    <p className="font-medium text-amber-600 dark:text-amber-400">Large File Detected</p>
                    <p className="text-amber-600/80 dark:text-amber-400/80 text-xs mt-0.5">
                      This file contains {csvColumns.row_count_estimate.toLocaleString()} molecules and may take several minutes to process.
                    </p>
                  </div>
                </div>
              )}

              {/* Ready indicator */}
              {selectedSmilesColumn && (
                <div className="flex items-start gap-2 p-3 rounded-lg bg-green-500/10 border border-green-500/20">
                  <CheckCircle2 className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <div className="text-sm text-green-600 dark:text-green-400">
                    <p>Ready to process ~{csvColumns.row_count_estimate.toLocaleString()} molecules</p>
                    {csvColumns.columns.length > 2 && (
                      <p className="text-xs text-green-600/70 dark:text-green-400/70 mt-1">
                        Only selected columns will be uploaded (faster upload)
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* SDF file info */}
          {isSDF && (
            <div className="p-4">
              <div className="flex items-center gap-2 p-3 rounded-lg bg-green-500/10 border border-green-500/20">
                <CheckCircle2 className="w-4 h-4 text-green-500" />
                <span className="text-sm text-green-600 dark:text-green-400">
                  SDF file ready to process
                </span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Standardization option */}
      {selectedFile && !isAnalyzing && (
        <div className="bg-[var(--color-surface-elevated)] rounded-xl border border-[var(--color-border)] overflow-hidden">
          <div className="p-4 border-b border-[var(--color-border)] bg-[var(--color-surface-sunken)]/50">
            <div className="flex items-center gap-2">
              <FlaskConical className="w-4 h-4 text-[var(--color-primary)]" />
              <p className="text-sm font-medium text-[var(--color-text-primary)]">Standardization</p>
            </div>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">
              Optionally run the ChEMBL standardization pipeline on each molecule before validation.
            </p>
          </div>
          <div className="p-4">
            <label className="flex items-start gap-3 cursor-pointer group">
              <input
                type="checkbox"
                checked={includeStandardization}
                onChange={(e) => setIncludeStandardization(e.target.checked)}
                disabled={isUploading}
                className="mt-1 h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
              />
              <div>
                <p className="text-sm font-medium text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors">
                  Enable ChEMBL Standardization
                </p>
                <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                  Normalizes structures using the ChEMBL pipeline: fixes nitro groups, removes salts and solvents, standardizes charge states, and extracts the parent molecule. Results will show the standardized SMILES alongside the original.
                </p>
              </div>
            </label>
          </div>
        </div>
      )}

      {/* Safety screening options */}
      {selectedFile && !isAnalyzing && (
        <div className="bg-[var(--color-surface-elevated)] rounded-xl border border-[var(--color-border)] overflow-hidden">
          <div className="p-4 border-b border-[var(--color-border)] bg-[var(--color-surface-sunken)]/50">
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-[var(--color-primary)]" />
              <p className="text-sm font-medium text-[var(--color-text-primary)]">Safety Screening</p>
            </div>
            <p className="text-xs text-[var(--color-text-muted)] mt-1">
              All molecules are screened against PAINS and Brenk filters by default. Enable additional filters below for more comprehensive screening.
            </p>
          </div>
          <div className="p-4 space-y-3">
            {/* Extended safety (NIH + ZINC) */}
            <label className="flex items-start gap-3 cursor-pointer group">
              <input
                type="checkbox"
                checked={includeExtendedSafety}
                onChange={(e) => setIncludeExtendedSafety(e.target.checked)}
                disabled={isUploading}
                className="mt-1 h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
              />
              <div>
                <p className="text-sm font-medium text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors">
                  Extended Filters (NIH + ZINC)
                </p>
                <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                  NIH/NCGC alerts flag compounds that interfere with high-throughput screening assays. ZINC filters identify reactive or unstable functional groups. Recommended for HTS compound libraries.
                </p>
              </div>
            </label>

            {/* ChEMBL alerts */}
            <label className="flex items-start gap-3 cursor-pointer group">
              <input
                type="checkbox"
                checked={includeChemblAlerts}
                onChange={(e) => setIncludeChemblAlerts(e.target.checked)}
                disabled={isUploading}
                className="mt-1 h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
              />
              <div>
                <p className="text-sm font-medium text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors">
                  ChEMBL Pharma Alerts (7 filter sets)
                </p>
                <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                  Structural alerts curated by pharmaceutical companies: Bristol-Myers Squibb, Dundee, GlaxoSmithKline, Inpharmatica, Lilly (LINT), MLSMR, and SureChEMBL. These are aggressive filters designed for drug discovery — many flagged compounds may be acceptable outside that context.
                </p>
              </div>
            </label>
          </div>
        </div>
      )}

      {/* Upload/Process button */}
      {selectedFile && !isAnalyzing && (
        <div className="space-y-2">
          <ClayButton
            variant="primary"
            size="lg"
            onClick={handleUpload}
            disabled={!selectedFile || isUploading || isExtracting || disabled || !!(csvColumns && !selectedSmilesColumn)}
            loading={isUploading || isExtracting}
            leftIcon={!isUploading && !isExtracting ? <Upload className="w-5 h-5" /> : undefined}
            className="w-full"
          >
            {isExtracting
              ? 'Preparing file...'
              : isUploading && uploadProgress < 100
              ? `Uploading... ${uploadProgress}%`
              : isUploading && uploadProgress >= 100
              ? 'Processing on server...'
              : isLargeFile
              ? `Start Processing ~${csvColumns?.row_count_estimate.toLocaleString()} Molecules`
              : 'Upload and Process'}
          </ClayButton>
          {isUploading && uploadProgress > 0 && uploadProgress < 100 && (
            <div className="w-full bg-[var(--color-surface-sunken)] rounded-full h-2 overflow-hidden">
              <div
                className="h-full bg-[var(--color-primary)] transition-all duration-300"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
