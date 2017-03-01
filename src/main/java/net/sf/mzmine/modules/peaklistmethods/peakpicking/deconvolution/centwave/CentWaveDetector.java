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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.INTEGRATION_METHOD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PEAK_DURATION;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PEAK_SCALES;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.SN_THRESHOLD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PeakIntegrationMethod;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.google.common.collect.Range;

/**
 * Use XCMS findPeaks.centWave to identify peaks.
 */
public class CentWaveDetector implements PeakResolver {

    // Logger.
    private static final Logger LOG = Logger.getLogger(CentWaveDetector.class
            .getName());

    // Name.
    private static final String NAME = "Wavelets (XCMS)";

    // Minutes <-> seconds.
    private static final double SECONDS_PER_MINUTE = 60.0;

    // Required minimum version of XCMS.
    private static final String XCMS_VERSION = "1.33.2";

    @Nonnull
    @Override
    public String getName() {

        return NAME;
    }

    @Nonnull
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {

        return CentWaveDetectorParameters.class;
    }

    @Override
    public boolean getRequiresR() {
        return true;
    }

    @Override
    public String[] getRequiredRPackages() {
        return new String[] { "xcms" };
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return new String[] { XCMS_VERSION };
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            final ParameterSet parameters,
            RSessionWrapper rSession) throws RSessionWrapperException {
        
        int scanNumbers[] = chromatogram.getScanNumbers();
        final int scanCount = scanNumbers.length;
        double retentionTimes[] = new double[scanCount];
        double intensities[] = new double[scanCount];
        RawDataFile dataFile = chromatogram.getDataFile();
        for (int i = 0; i < scanCount; i++) {
            final int scanNum = scanNumbers[i];
            retentionTimes[i] = dataFile.getScan(scanNum).getRetentionTime();
            DataPoint dp = chromatogram.getDataPoint(scanNum);
            if (dp != null)
                intensities[i] = dp.getIntensity();
            else
                intensities[i] = 0.0;
        }
        
        // Call findPeaks.centWave.
        double[][] peakMatrix = null;

        peakMatrix = centWave(rSession, retentionTimes, intensities,
                chromatogram.getMZ(), parameters.getParameter(SN_THRESHOLD)
                        .getValue(), parameters.getParameter(PEAK_SCALES)
                        .getValue(), parameters
                        .getParameter(INTEGRATION_METHOD).getValue());

        final List<ResolvedPeak> resolvedPeaks;
        if (peakMatrix == null) {

            resolvedPeaks = new ArrayList<ResolvedPeak>(0);

        } else {

            LOG.finest("Processing peak matrix...");

            final Range<Double> peakDuration = parameters.getParameter(
                    PEAK_DURATION).getValue();

            // Process peak matrix.
            resolvedPeaks = new ArrayList<ResolvedPeak>(peakMatrix.length);

            for (final double[] peakRow : peakMatrix) {

                // Get peak start and end.
                final int peakLeft = findRTIndex(retentionTimes, peakRow[4]);
                final int peakRight = findRTIndex(retentionTimes, peakRow[5]);

                // Partition into sections bounded by null data points, creating
                // a peak for each.
                for (int start = peakLeft; start < peakRight; start++) {

                    if (chromatogram.getDataPoint(scanNumbers[start]) != null) {

                        int end = start;
                        
                        while (end < peakRight
                                && chromatogram
                                        .getDataPoint(scanNumbers[end + 1]) != null) {

                            end++;
                        }

                        if ((end > start) && (peakDuration.contains(retentionTimes[end]
                                - retentionTimes[start]))) {

                            resolvedPeaks.add(new ResolvedPeak(chromatogram,
                                    start, end));
                        }

                        start = end;
                    }
                }
            }
        }

        return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
    }

    private static int findRTIndex(final double[] rtMinutes, final double rtSec) {

        final int i = Arrays
                .binarySearch(rtMinutes, rtSec / SECONDS_PER_MINUTE);
        return i >= 0 ? i : -i - 2;
    }

    /**
     * Do peak picking using xcms::findPeaks.centWave.
     * 
     * @param scanTime
     *            retention times (for each scan).
     * @param intensity
     *            intensity values (for each scan).
     * @param mz
     *            fixed m/z value for EIC.
     * @param snrThreshold
     *            signal:noise ratio threshold.
     * @param peakWidth
     *            peak width range.
     * @param integrationMethod
     *            integration method.
     * @return a matrix with a row for each detected peak.
     * @throws RSessionWrapperException
     */
    private static double[][] centWave(RSessionWrapper rSession,
            final double[] scanTime, final double[] intensity, final double mz,
            final double snrThreshold, final Range<Double> peakWidth,
            final PeakIntegrationMethod integrationMethod)
            throws RSessionWrapperException {

        LOG.finest("Detecting peaks.");

        final double[][] peaks;

        // Set vectors.
        rSession.assign("scantime", scanTime);
        rSession.assign("intensity", intensity);

        // Initialize.
        rSession.eval("mz <- " + mz);
        rSession.eval("numPoints <- length(intensity)");

        // Construct xcmsRaw object
        rSession.eval("xRaw <- new(\"xcmsRaw\")");
        rSession.eval("xRaw@tic <- intensity");
        rSession.eval("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE);
        rSession.eval("xRaw@scanindex <- 1:numPoints");
        rSession.eval("xRaw@env$mz <- rep(mz, numPoints)");
        rSession.eval("xRaw@env$intensity <- intensity");

        // Construct ROIs.
        rSession.eval("ROIs <- list()");
        int roi = 1;
        for (int start = 0; start < intensity.length; start++) {

            // Found non-zero section.
            if (intensity[start] > 0.0) {

                // Look for end.
                int end = start + 1;
                while (end < intensity.length && intensity[end] > 0.0) {

                    end++;
                }

                // Add ROI to list.
                rSession.eval("ROIs[[" + roi + "]] <- list('scmin'="
                        + (start + 1) + ", 'scmax'=" + end
                        + ", 'mzmin'=mz, 'mzmax'=mz)");

                // Next ROI.
                start = end;
                roi++;

            }
        }

        // Do peak picking.
        final Object centWave = roi <= 1 ? null : (double[][]) rSession
                .collect(
                        "findPeaks.centWave(xRaw, ppm=0, mzdiff=0, verbose=TRUE"
                                + ", peakwidth=c(" + peakWidth.lowerEndpoint()
                                * SECONDS_PER_MINUTE + ", "
                                + peakWidth.upperEndpoint()
                                * SECONDS_PER_MINUTE + ')' + ", snthresh="
                                + snrThreshold + ", integrate="
                                + integrationMethod.getIndex()
                                + ", ROI.list=ROIs)", false);

        peaks = (centWave == null) ? null : (double[][]) centWave;

        return peaks;
    }
}
