/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.R.REngineType;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.google.common.collect.Range;

/**
 * @description Abstract corrector class for baseline correction. Has to be
 *              specialized via the implementation of a "BaselineProvider".
 * 
 */
public abstract class BaselineCorrector implements BaselineProvider,
        MZmineModule {

    // Logger.
    protected static final Logger LOG = Logger
            .getLogger(BaselineCorrector.class.getName());

    // Processing info storage
    /**
     * String: dataFile being processed. int[]: 3 values array => { progress,
     * progressMax, isAborted }
     */
    HashMap<RawDataFile, int[]> progressMap;

    // Filename suffix.
    private String suffix;

    // General parameters (common to all baseline correction methods).
    private REngineType rEgineType;
    private ChromatogramType chromatogramType;
    private double binWidth;
    private boolean useBins;
    private int msLevel;


    /**
     * Initialization
     */
    public BaselineCorrector() {

        // Processing info storage
        progressMap = new HashMap<RawDataFile, int[]>();
    }

    /**
     * Getting general parameters (common to all the correctors).
     * 
     * @param generalParameters
     *            The parameters common to all methods (grabbed from
     *            "BaselineCorrectionParameters")
     */
    public void collectCommonParameters(final ParameterSet parameters) {

        ParameterSet generalParameters;
        if (parameters != null) {
            generalParameters = parameters;
        } else if (BaselineCorrectionParameters
                .getBaselineCorrectionParameters() == null) {
            generalParameters = MZmineCore.getConfiguration()
                    .getModuleParameters(BaselineCorrectionModule.class);
        } else {
            generalParameters = BaselineCorrectionParameters
                    .getBaselineCorrectionParameters();
        }
        // Get common parameters.
        rEgineType = generalParameters.getParameter(
                BaselineCorrectionParameters.RENGINE_TYPE).getValue();
        suffix = generalParameters.getParameter(
                BaselineCorrectionParameters.SUFFIX).getValue();
        chromatogramType = generalParameters.getParameter(
                BaselineCorrectionParameters.CHROMOTAGRAM_TYPE).getValue();
        binWidth = generalParameters.getParameter(
                BaselineCorrectionParameters.MZ_BIN_WIDTH).getValue();
        useBins = generalParameters.getParameter(
                BaselineCorrectionParameters.USE_MZ_BINS).getValue();
        msLevel = generalParameters.getParameter(
                BaselineCorrectionParameters.MS_LEVEL).getValue();
    }

    public final RawDataFile correctDatafile(final RSessionWrapper rSession,
            final RawDataFile dataFile, final ParameterSet parameters,
            final ParameterSet commonParameters) throws IOException,
            RSessionWrapperException {

        if (isAborted(dataFile) || !rSession.isSessionRunning())
            return null;
        // Get very last information from root module setup
        // this.setGeneralParameters(MZmineCore.getConfiguration().getModuleParameters(BaselineCorrectionModule.class));
        this.collectCommonParameters(commonParameters);

        RawDataFile correctedDataFile = null;

        RawDataFile origDataFile = dataFile;

        // Initialize progress info if not done already.
        if (!progressMap.containsKey(origDataFile))
            progressMap.put(origDataFile, new int[] { 0, 0, 0 });

        // Create a new temporary file to write in.
        RawDataFileWriter rawDataFileWriter = MZmineCore
                .createNewFile(origDataFile.getName() + ' ' + suffix);

        // Determine number of bins.
        final double mzLen = origDataFile.getDataMZRange().upperEndpoint()
                - origDataFile.getDataMZRange().lowerEndpoint();
        final int numBins = useBins ? (int) Math.ceil(mzLen / binWidth) : 1;

        // Get MS levels.
        final int[] levels = origDataFile.getMSLevels();

        // Measure progress and find MS-level.
        boolean foundLevel = msLevel == 0;
        // progressMax = 0;
        for (final int level : levels) {
            final boolean isMSLevel = msLevel == level;
            final int numScans = origDataFile.getScanNumbers(level).length;
            foundLevel |= isMSLevel;
            // progressMax += isMSLevel || msLevel == 0 ? 2 * numScans +
            // numBins : numScans;
            progressMap.get(origDataFile)[1] += (isMSLevel || msLevel == 0 ? 2
                    * numScans + numBins : numScans);
        }

        // Is the specified MS-level present?
        if (!foundLevel) {
            throw new IllegalArgumentException(
                    "The data file doesn't contain data for MS-level "
                            + msLevel + '.');
        }

        // Which chromatogram type.
        final boolean useTIC = (chromatogramType == ChromatogramType.TIC);

        // Process each MS level.
        for (final int level : levels) {

            if (!isAborted(origDataFile)) {
                if (level == msLevel || msLevel == 0) {

                    // Correct baseline for this MS-level.
                    if (useTIC) {
                        correctTICBaselines(rSession, origDataFile,
                                rawDataFileWriter, level, numBins, parameters);
                    } else {
                        correctBasePeakBaselines(rSession, origDataFile,
                                rawDataFileWriter, level, numBins, parameters);
                    }
                } else {

                    // Copy scans for this MS-level.
                    copyScansToWriter(origDataFile, rawDataFileWriter, level);
                }
            }
        }

        // If the referring task was canceled, stop processing.
        if (!isAborted(origDataFile)) {
            // Finalize writing.
            correctedDataFile = rawDataFileWriter.finishWriting();
        }

        return correctedDataFile;
    }

    /**
     * Copy scans to RawDataFileWriter.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param writer
     *            writer to copy scans to.
     * @param level
     *            MS-level of scans to copy.
     * @throws IOException
     *             if there are i/o problems.
     */
    private void copyScansToWriter(final RawDataFile origDataFile,
            final RawDataFileWriter writer, final int level) throws IOException {

        LOG.finest("Copy scans");

        // Get scan numbers for MS-level.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Create copy of scans.
        for (int scanIndex = 0; !isAborted(origDataFile)
                && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Get data points (m/z and intensity pairs) of the original scan
            final DataPoint[] origDataPoints = origScan.getDataPoints();
            final DataPoint[] newDataPoints = new DataPoint[origDataPoints.length];

            // Copy original data points.
            int i = 0;
            for (final DataPoint dp : origDataPoints) {
                newDataPoints[i++] = new SimpleDataPoint(dp);
            }

            // Create new copied scan.
            final SimpleScan newScan = new SimpleScan(origScan);
            newScan.setDataPoints(newDataPoints);
            writer.addScan(newScan);
            progressMap.get(origDataFile)[0]++;
        }
    }

    /**
     * Correct the baselines (using base peak chromatograms).
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param writer
     *            data file writer.
     * @param level
     *            the MS level.
     * @param numBins
     *            number of m/z bins.
     * @param parameters
     *            parameters specific to the actual method for baseline
     *            computing.
     * @throws IOException
     *             if there are i/o problems.
     * @throws RSessionWrapperException
     * @throws BaselineCorrectionException
     * @throws InterruptedException
     */
    private void correctBasePeakBaselines(final RSessionWrapper rSession,
            final RawDataFile origDataFile, final RawDataFileWriter writer,
            final int level, final int numBins, final ParameterSet parameters)
            throws IOException, RSessionWrapperException {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Build chromatograms.
        LOG.finest("Building base peak chromatograms.");
        final double[][] baseChrom = buildBasePeakChromatograms(origDataFile,
                level, numBins);

        // Calculate baselines: done in-place, i.e. overwrite chromatograms to
        // save memory.
        LOG.finest("Calculating baselines.");
        for (int binIndex = 0; !isAborted(origDataFile) && binIndex < numBins; binIndex++) {
            baseChrom[binIndex] = computeBaseline(rSession, origDataFile,
                    baseChrom[binIndex], parameters);
            progressMap.get(origDataFile)[0]++;
        }

        // Subtract baselines.
        LOG.finest("Subtracting baselines.");
        for (int scanIndex = 0; !isAborted(origDataFile)
                && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Get data points (m/z and intensity pairs) of the original scan
            final DataPoint[] origDataPoints = origScan.getDataPoints();

            // Create and write new corrected scan.
            final SimpleScan newScan = new SimpleScan(origScan);
            newScan.setDataPoints(subtractBasePeakBaselines(origDataFile,
                    origDataPoints, baseChrom, numBins, scanIndex));
            writer.addScan(newScan);
            progressMap.get(origDataFile)[0]++;
        }
    }

    /**
     * Correct the baselines (using TIC chromatograms).
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param writer
     *            data file writer.
     * @param level
     *            the MS level.
     * @param numBins
     *            number of m/z bins.
     * @param parameters
     *            parameters specific to the actual method for baseline
     *            computing.
     * @throws IOException
     *             if there are i/o problems.
     * @throws RSessionWrapperException
     * @throws BaselineCorrectionException
     */
    private void correctTICBaselines(final RSessionWrapper rSession,
            final RawDataFile origDataFile, final RawDataFileWriter writer,
            final int level, final int numBins, final ParameterSet parameters)
            throws IOException, RSessionWrapperException {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Build chromatograms.
        LOG.finest("Building TIC chromatograms.");
        final double[][] baseChrom = buildTICChromatograms(origDataFile, level,
                numBins);

        // Calculate baselines: done in-place, i.e. overwrite chromatograms to
        // save memory.
        LOG.finest("Calculating baselines.");
        for (int binIndex = 0; !isAborted(origDataFile) && binIndex < numBins; binIndex++) {

            // Calculate baseline.
            // final double[] baseline = asymBaseline(baseChrom[binIndex]);
            final double[] baseline = computeBaseline(rSession, origDataFile,
                    baseChrom[binIndex], parameters);

            // Normalize the baseline w.r.t. chromatogram (TIC).
            for (int scanIndex = 0; !isAborted(origDataFile)
                    && scanIndex < numScans; scanIndex++) {
                final double bc = baseChrom[binIndex][scanIndex];
                if (bc != 0.0) {
                    baseChrom[binIndex][scanIndex] = baseline[scanIndex] / bc;
                }
            }
            progressMap.get(origDataFile)[0]++;
        }

        // Subtract baselines.
        LOG.finest("Subtracting baselines.");
        for (int scanIndex = 0; !isAborted(origDataFile)
                && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Get data points (m/z and intensity pairs) of the original scan
            final DataPoint[] origDataPoints = origScan.getDataPoints();

            // Create and write new corrected scan.
            final SimpleScan newScan = new SimpleScan(origScan);
            newScan.setDataPoints(subtractTICBaselines(origDataFile,
                    origDataPoints, baseChrom, numBins, scanIndex));
            writer.addScan(newScan);
            progressMap.get(origDataFile)[0]++;
        }

    }

    /**
     * Constructs base peak (max) chromatograms - one for each m/z bin.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param level
     *            the MS level.
     * @param numBins
     *            number of m/z bins.
     * @return the chromatograms as double[number of bins][number of scans].
     */
    private double[][] buildBasePeakChromatograms(
            final RawDataFile origDataFile, final int level, final int numBins) {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Determine MZ range.
        final Range<Double> mzRange = origDataFile.getDataMZRange();

        // Create chromatograms.
        final double[][] chromatograms = new double[numBins][numScans];

        for (int scanIndex = 0; !isAborted(origDataFile)
                && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Process data points.
            for (final DataPoint dataPoint : scan.getDataPoints()) {

                final int bin = RangeUtils.binNumber(mzRange, numBins,
                        dataPoint.getMZ());

                final double value = chromatograms[bin][scanIndex];
                chromatograms[bin][scanIndex] = Math.max(value,
                        dataPoint.getIntensity());
            }
            progressMap.get(origDataFile)[0]++;
        }

        return chromatograms;
    }

    /**
     * Constructs TIC (sum) chromatograms - one for each m/z bin.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param level
     *            the MS level.
     * @param numBins
     *            number of m/z bins.
     * @return the chromatograms as double[number of bins][number of scans].
     */
    private double[][] buildTICChromatograms(final RawDataFile origDataFile,
            final int level, final int numBins) {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Determine MZ range.
        final Range<Double> mzRange = origDataFile.getDataMZRange();

        // Create chromatograms.
        final double[][] chromatograms = new double[numBins][numScans];

        for (int scanIndex = 0; !isAborted(origDataFile)
                && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Process data points.
            for (final DataPoint dataPoint : scan.getDataPoints()) {

                chromatograms[RangeUtils.binNumber(mzRange, numBins,
                        dataPoint.getMZ())][scanIndex] += dataPoint
                        .getIntensity();
            }
            progressMap.get(origDataFile)[0]++;
        }

        return chromatograms;
    }

    /**
     * Perform baseline correction in bins (base peak).
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param dataPoints
     *            input data points to correct.
     * @param baselines
     *            the baselines - one per m/z bin.
     * @param numBins
     *            the number of m/z bins.
     * @param scanIndex
     *            the current scan index that these data points come from.
     * @return the corrected data points.
     */
    private DataPoint[] subtractBasePeakBaselines(
            final RawDataFile origDataFile, final DataPoint[] dataPoints,
            final double[][] baselines, final int numBins, final int scanIndex) {

        // Create an ArrayList for new data points.
        final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

        // Determine MZ range.
        final Range<Double> mzRange = origDataFile.getDataMZRange();

        // Loop through all original data points.
        int i = 0;
        for (final DataPoint dp : dataPoints) {

            // Subtract baseline.
            final double mz = dp.getMZ();
            final int bin = RangeUtils.binNumber(mzRange, numBins, mz);
            final double baselineIntenstity = baselines[bin][scanIndex];
            newDataPoints[i++] = baselineIntenstity <= 0.0 ? new SimpleDataPoint(
                    dp) : new SimpleDataPoint(mz, Math.max(0.0,
                    dp.getIntensity() - baselineIntenstity));
        }

        // Return the new data points.
        return newDataPoints;
    }

    /**
     * Perform baseline correction in bins (TIC).
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param dataPoints
     *            input data points to correct.
     * @param baselines
     *            the baselines - one per m/z bin.
     * @param numBins
     *            the number of m/z bins.
     * @param scanIndex
     *            the current scan index that these data points come from.
     * @return the corrected data points.
     */
    private DataPoint[] subtractTICBaselines(final RawDataFile origDataFile,
            final DataPoint[] dataPoints, final double[][] baselines,
            final int numBins, final int scanIndex) {

        // Create an ArrayList for new data points.
        final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

        // Determine MZ range.
        final Range<Double> mzRange = origDataFile.getDataMZRange();

        // Loop through all original data points.
        int i = 0;
        for (final DataPoint dp : dataPoints) {

            // Subtract baseline.
            final double mz = dp.getMZ();
            final int bin = RangeUtils.binNumber(mzRange, numBins, mz);
            final double baselineIntenstity = baselines[bin][scanIndex];
            newDataPoints[i++] = baselineIntenstity <= 0.0 ? new SimpleDataPoint(
                    dp) : new SimpleDataPoint(mz, Math.max(0.0,
                    dp.getIntensity() * (1.0 - baselineIntenstity)));
        }

        // Return the new data points.
        return newDataPoints;
    }

    // Correction progress stuffs (to be called from mother Task)
    /**
     * Initializing progress info.
     * 
     * @param origDataFile
     *            dataFile of concern.
     */
    public void initProgress(final RawDataFile origDataFile) {
        progressMap.put(origDataFile, new int[] { 0, 0, 0 });
    }

    /**
     * Getting progress.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @return progress.
     */
    private int getProgress(final RawDataFile origDataFile) {
        if (progressMap.containsKey(origDataFile))
            return progressMap.get(origDataFile)[0]; // progress;
        else
            return 0;
    }

    /**
     * Getting progressMax.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @return progressMax.
     */
    private int getProgressMax(final RawDataFile origDataFile) {
        if (progressMap.containsKey(origDataFile))
            return progressMap.get(origDataFile)[1]; // progressMax;
        else
            return 0;
    }

    /**
     * Getting global progress.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @return The finished percentage.
     */
    public double getFinishedPercentage(final RawDataFile origDataFile) {
        int progressMax = this.getProgressMax(origDataFile);
        int progress = this.getProgress(origDataFile);
        return (progressMax == 0 ? 0.0 : (double) progress
                / (double) progressMax);
    }

    /**
     * Releasing progress info.
     * 
     * @param origDataFile
     *            dataFile of concern.
     */
    public void clearProgress(final RawDataFile origDataFile) {
        progressMap.remove(origDataFile);
    }

    
    public REngineType getRengineType() {
        return this.rEgineType;
    }

    // Chromatogram type
    public ChromatogramType getChromatogramType() {
        // return
        // MZmineCore.getConfiguration().getModuleParameters(BaselineCorrectionModule.class)
        // .getParameter(BaselineCorrectionParameters.CHROMOTAGRAM_TYPE).getValue();
        return this.chromatogramType;
    }

    // Cancel processing features
    /**
     * Switch to abort processing (used from task mode)
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @param abort
     *            If we shall abort
     */
    public void setAbortProcessing(final RawDataFile origDataFile, boolean abort) {
        if (progressMap.containsKey(origDataFile))
            progressMap.get(origDataFile)[2] = 1;
    }

    /**
     * Check if dataFile processing has been canceled.
     * 
     * @param origDataFile
     *            dataFile of concern.
     * @return True if it has.
     */
    protected boolean isAborted(final RawDataFile origDataFile) {
        if (progressMap.containsKey(origDataFile))
            return (progressMap.get(origDataFile)[2] == 1);
        else
            return false;
    }

}
