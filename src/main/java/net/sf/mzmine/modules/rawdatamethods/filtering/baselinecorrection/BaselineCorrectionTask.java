/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RUtilities;
import net.sf.mzmine.util.Range;
import org.rosuda.JRI.Rengine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task that performs baseline correction.
 *
 * @author $Author$
 * @version $Revision$
 */
public class BaselineCorrectionTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(BaselineCorrectionTask.class.getName());

    // Original data file and newly created baseline corrected file.
    private final RawDataFile origDataFile;
    private RawDataFile correctedDataFile;

    // Progress counters.
    private int progress;
    private int progressMax;

    // Filename suffix.
    private final String suffix;

    // Remove original data file.
    private final boolean removeOriginal;

    // Smoothing and asymmetry parameters.
    private final double smoothing;
    private final double asymmetry;
    private final double binWidth;
    private final boolean useBins;
    private final int msLevel;
    private final ChromatogramType chromatogramType;

    /**
     * Creates the task.
     *
     * @param dataFile   raw data file on which to perform correction.
     * @param parameters correction parameters.
     */
    public BaselineCorrectionTask(final RawDataFile dataFile,
                                  final ParameterSet parameters) {

        // Initialize.
        origDataFile = dataFile;
        correctedDataFile = null;
        progressMax = 0;
        progress = 0;

        // Get parameters.
        suffix = parameters.getParameter(BaselineCorrectionParameters.SUFFIX).getValue();
        removeOriginal = parameters.getParameter(BaselineCorrectionParameters.REMOVE_ORIGINAL).getValue();
        smoothing = parameters.getParameter(BaselineCorrectionParameters.SMOOTHING).getValue();
        asymmetry = parameters.getParameter(BaselineCorrectionParameters.ASYMMETRY).getValue();
        binWidth = parameters.getParameter(BaselineCorrectionParameters.MZ_BIN_WIDTH).getValue();
        useBins = parameters.getParameter(BaselineCorrectionParameters.USE_MZ_BINS).getValue();
        msLevel = parameters.getParameter(BaselineCorrectionParameters.MS_LEVEL).getValue();
        chromatogramType = parameters.getParameter(BaselineCorrectionParameters.CHROMOTAGRAM_TYPE).getValue();
    }

    @Override
    public String getTaskDescription() {
        return "Correcting baseline for " + origDataFile;
    }

    @Override
    public double getFinishedPercentage() {
        return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
    }

    @Override
    public Object[] getCreatedObjects() {
        return new Object[]{correctedDataFile};
    }

    @Override
    public void run() {

        // Update the status of this task
        setStatus(TaskStatus.PROCESSING);

        try {
            // Create a new file
            final RawDataFileWriter rawDataFileWriter =
                    MZmineCore.createNewFile(origDataFile.getName() + ' ' + suffix);

            // Determine number of bins.
            final int numBins = useBins ? (int) Math.ceil(origDataFile.getDataMZRange().getSize() / binWidth) : 1;

            // Get MS levels.
            final int[] levels = origDataFile.getMSLevels();

            // Measure progress and find MS-level.
            boolean foundLevel = msLevel == 0;
            progressMax = 0;
            for (final int level : levels) {
                final boolean isMSLevel = msLevel == level;
                final int numScans = origDataFile.getScanNumbers(level).length;
                foundLevel |= isMSLevel;
                progressMax += isMSLevel || msLevel == 0 ? 2 * numScans + numBins : numScans;
            }

            // Is the specified MS-level present?
            if (!foundLevel) {
                throw new IllegalArgumentException("The data file doesn't contain data for MS-level " + msLevel + '.');
            }

            // Which chromatogram type.
            final boolean useTIC = chromatogramType == ChromatogramType.TIC;

            // Process each MS level.
            for (final int level : levels) {

                if (!isCanceled()) {
                    if (level == msLevel || msLevel == 0) {

                        // Correct baseline for this MS-level.
                        if (useTIC) {
                            correctTICBaselines(rawDataFileWriter, level, numBins);
                        } else {
                            correctBasePeakBaselines(rawDataFileWriter, level, numBins);
                        }
                    } else {

                        // Copy scans for this MS-level.
                        copyScansToWriter(rawDataFileWriter, level);
                    }
                }
            }

            // If this task was canceled, stop processing
            if (!isCanceled()) {

                // Finalize writing
                correctedDataFile = rawDataFileWriter.finishWriting();

                // Add the newly created file to the project
                final MZmineProject project = MZmineCore.getCurrentProject();
                project.addFile(correctedDataFile);

                // Remove the original data file if requested
                if (removeOriginal) {
                    project.removeFile(origDataFile);
                }

                // Set task status to FINISHED
                setStatus(TaskStatus.FINISHED);

                LOG.info("Baseline corrected " + origDataFile.getName());
            }
        }
        catch (Throwable t) {

            LOG.log(Level.SEVERE, "Baseline correction error", t);
            setStatus(TaskStatus.ERROR);
            errorMessage = t.getMessage();
        }
    }

    /**
     * Copy scans to RawDataFileWriter.
     *
     * @param writer writer to copy scans to.
     * @param level  MS-level of scans to copy.
     * @throws IOException if there are i/o problems.
     */
    private void copyScansToWriter(final RawDataFileWriter writer,
                                   final int level)
            throws IOException {

        LOG.finest("Copy scans");

        // Get scan numbers for MS-level.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Create copy of scans.
        for (int scanIndex = 0;
             !isCanceled() && scanIndex < numScans;
             scanIndex++) {

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
            progress++;
        }
    }

    /**
     * Correct the baselines (using base peak chromatograms).
     *
     * @param writer  data file writer.
     * @param level   the MS level.
     * @param numBins number of m/z bins.
     * @throws IOException if there are i/o problems.
     */
    private void correctBasePeakBaselines(final RawDataFileWriter writer, final int level, final int numBins)
            throws IOException {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Build chromatograms.
        LOG.finest("Building base peak chromatograms.");
        final double[][] baseChrom = buildBasePeakChromatograms(level, numBins);

        // Calculate baselines: done in-place, i.e. overwrite chromatograms to save memory.
        LOG.finest("Calculating baselines.");
        for (int binIndex = 0; !isCanceled() && binIndex < numBins; binIndex++) {
            baseChrom[binIndex] = asymBaseline(baseChrom[binIndex]);
            progress++;
        }

        // Subtract baselines.
        LOG.finest("Subtracting baselines.");
        for (int scanIndex = 0; !isCanceled() && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Get data points (m/z and intensity pairs) of the original scan
            final DataPoint[] origDataPoints = origScan.getDataPoints();

            // Create and write new corrected scan.
            final SimpleScan newScan = new SimpleScan(origScan);
            newScan.setDataPoints(subtractBasePeakBaselines(origDataPoints, baseChrom, numBins, scanIndex));
            writer.addScan(newScan);
            progress++;
        }
    }

    /**
     * Correct the baselines (using TIC chromatograms).
     *
     * @param writer  data file writer.
     * @param level   the MS level.
     * @param numBins number of m/z bins.
     * @throws IOException if there are i/o problems.
     */
    private void correctTICBaselines(final RawDataFileWriter writer, final int level, final int numBins)
            throws IOException {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Build chromatograms.
        LOG.finest("Building TIC chromatograms.");
        final double[][] baseChrom = buildTICChromatograms(level, numBins);

        // Calculate baselines: done in-place, i.e. overwrite chromatograms to save memory.
        LOG.finest("Calculating baselines.");
        for (int binIndex = 0; !isCanceled() && binIndex < numBins; binIndex++) {

            // Calculate baseline.
            final double[] baseline = asymBaseline(baseChrom[binIndex]);

            // Normalize the baseline w.r.t. chromatogram (TIC).
            for (int scanIndex = 0; scanIndex < numScans; scanIndex++) {
                final double bc = baseChrom[binIndex][scanIndex];
                if (bc != 0.0) {
                    baseChrom[binIndex][scanIndex] = baseline[scanIndex] / bc;
                }
            }
            progress++;
        }

        // Subtract baselines.
        LOG.finest("Subtracting baselines.");
        for (int scanIndex = 0; !isCanceled() && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Get data points (m/z and intensity pairs) of the original scan
            final DataPoint[] origDataPoints = origScan.getDataPoints();

            // Create and write new corrected scan.
            final SimpleScan newScan = new SimpleScan(origScan);
            newScan.setDataPoints(subtractTICBaselines(origDataPoints, baseChrom, numBins, scanIndex));
            writer.addScan(newScan);
            progress++;
        }
    }

    /**
     * Constructs base peak (max) chromatograms - one for each m/z bin.
     *
     * @param level   the MS level.
     * @param numBins number of m/z bins.
     * @return the chromatograms as double[number of bins][number of scans].
     */
    private double[][] buildBasePeakChromatograms(final int level, final int numBins) {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Determine MZ range.
        final Range mzRange = origDataFile.getDataMZRange();

        // Create chromatograms.
        final double[][] chromatograms = new double[numBins][numScans];

        for (int scanIndex = 0; !isCanceled() && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Process data points.
            for (final DataPoint dataPoint : scan.getDataPoints()) {

                final int bin = mzRange.binNumber(numBins, dataPoint.getMZ());
                final double value = chromatograms[bin][scanIndex];
                chromatograms[bin][scanIndex] = Math.max(value, dataPoint.getIntensity());
            }
            progress++;
        }

        return chromatograms;
    }

    /**
     * Constructs TIC (sum) chromatograms - one for each m/z bin.
     *
     * @param level   the MS level.
     * @param numBins number of m/z bins.
     * @return the chromatograms as double[number of bins][number of scans].
     */
    private double[][] buildTICChromatograms(final int level, final int numBins) {

        // Get scan numbers from original file.
        final int[] scanNumbers = origDataFile.getScanNumbers(level);
        final int numScans = scanNumbers.length;

        // Determine MZ range.
        final Range mzRange = origDataFile.getDataMZRange();

        // Create chromatograms.
        final double[][] chromatograms = new double[numBins][numScans];

        for (int scanIndex = 0; !isCanceled() && scanIndex < numScans; scanIndex++) {

            // Get original scan.
            final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

            // Process data points.
            for (final DataPoint dataPoint : scan.getDataPoints()) {

                chromatograms[mzRange.binNumber(numBins, dataPoint.getMZ())][scanIndex] += dataPoint.getIntensity();
            }
            progress++;
        }

        return chromatograms;
    }

    /**
     * Determine the baseline via Asymmetric Least Squares (in R's parametric time warping PTW package).
     *
     * @param chromatogram the chromatogram whose baseline is to be determined.
     * @return the baseline.
     */
    private double[] asymBaseline(final double[] chromatogram) {

        // Get R engine.
        final Rengine rEngine;
        try {
            rEngine = RUtilities.getREngine();
        }
        catch (Throwable t) {
            throw new IllegalStateException(
                    "Baseline correction requires R but it couldn't be loaded (" + t.getMessage() + ')');
        }

        final double[] baseline;
        synchronized (RUtilities.R_SEMAPHORE) {

            // Load PTW library.
            if (rEngine.eval("require(ptw)").asBool().isFALSE()) {

                throw new IllegalStateException("The \"ptw\" R package couldn't be loaded - is it installed in R?");
            }

            try {

                // Set chromatogram.
                rEngine.assign("chromatogram", chromatogram);

                // Calculate baseline.
                baseline = rEngine.eval("asysm(chromatogram," + smoothing + ',' + asymmetry + ')').asDoubleArray();

            }
            catch (Throwable t) {

                throw new IllegalStateException("R error during baseline correction", t);
            }
        }
        return baseline;
    }

    /**
     * Perform baseline correction in bins.
     *
     * @param dataPoints input data points to correct.
     * @param baselines  the baselines - one per m/z bin.
     * @param numBins    the number of m/z bins.
     * @param scanIndex  the current scan index that these data points come from.
     * @return the corrected data points.
     */
    private DataPoint[] subtractBasePeakBaselines(final DataPoint[] dataPoints,
                                                  final double[][] baselines,
                                                  final int numBins,
                                                  final int scanIndex) {

        // Create an ArrayList for new data points.
        final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

        // Determine MZ range.
        final Range mzRange = origDataFile.getDataMZRange();

        // Loop through all original data points.
        int i = 0;
        for (final DataPoint dp : dataPoints) {

            // Subtract baseline.
            final double mz = dp.getMZ();
            final int bin = mzRange.binNumber(numBins, mz);
            final double baselineIntenstity = baselines[bin][scanIndex];
            newDataPoints[i++] = baselineIntenstity <= 0.0 ?
                                 new SimpleDataPoint(dp) :
                                 new SimpleDataPoint(mz, Math.max(0.0, dp.getIntensity() - baselineIntenstity));
        }

        // Return the new data points.
        return newDataPoints;
    }

    /**
     * Perform baseline correction in bins.
     *
     * @param dataPoints input data points to correct.
     * @param baselines  the baselines - one per m/z bin.
     * @param numBins    the number of m/z bins.
     * @param scanIndex  the current scan index that these data points come from.
     * @return the corrected data points.
     */
    private DataPoint[] subtractTICBaselines(final DataPoint[] dataPoints,
                                             final double[][] baselines,
                                             final int numBins,
                                             final int scanIndex) {

        // Create an ArrayList for new data points.
        final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

        // Determine MZ range.
        final Range mzRange = origDataFile.getDataMZRange();

        // Loop through all original data points.
        int i = 0;
        for (final DataPoint dp : dataPoints) {

            // Subtract baseline.
            final double mz = dp.getMZ();
            final int bin = mzRange.binNumber(numBins, mz);
            final double baselineIntenstity = baselines[bin][scanIndex];
            newDataPoints[i++] = baselineIntenstity <= 0.0 ?
                                 new SimpleDataPoint(dp) :
                                 new SimpleDataPoint(mz, Math.max(0.0, dp.getIntensity() * (1.0 - baselineIntenstity)));
        }

        // Return the new data points.
        return newDataPoints;
    }
}
