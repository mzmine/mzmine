/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RUtilities;
import net.sf.mzmine.util.Range;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.*;

/**
 * Use XCMS findPeaks.centWave to identify peaks.
 *
 * @author $Author$
 * @version $Revision$
 */
public class CentWaveDetector implements PeakResolver {

    // Logger.
    private static final Logger LOG = Logger.getLogger(CentWaveDetector.class.getName());

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
    public ChromatographicPeak[] resolvePeaks(final ChromatographicPeak chromatogram,
                                              final int[] scanNumbers,
                                              final double[] retentionTimes,
                                              final double[] intensities,
                                              final ParameterSet parameters) {
        // Call findPeaks.centWave.
        final double[][] peakMatrix = centWave(retentionTimes, intensities,
                                               chromatogram.getMZ(),
                                               parameters.getParameter(SN_THRESHOLD).getValue(),
                                               parameters.getParameter(PEAK_SCALES).getValue(),
                                               parameters.getParameter(INTEGRATION_METHOD).getValue());

        final List<ResolvedPeak> resolvedPeaks;
        if (peakMatrix == null) {

            resolvedPeaks = new ArrayList<ResolvedPeak>(0);

        } else {

            LOG.finest("Processing peak matrix...");

            final Range peakDuration = parameters.getParameter(PEAK_DURATION).getValue();

            // Process peak matrix.
            resolvedPeaks = new ArrayList<ResolvedPeak>(peakMatrix.length);

            for (final double[] peakRow : peakMatrix) {

                // Get peak start and end.
                final int peakLeft = findRTIndex(retentionTimes, peakRow[4]);
                final int peakRight = findRTIndex(retentionTimes, peakRow[5]);

                // Partition into sections bounded by null data points, creating a peak for each.
                for (int start = peakLeft;
                     start < peakRight;
                     start++) {

                    if (chromatogram.getDataPoint(scanNumbers[start]) != null) {

                        int end = start;
                        while (end < peakRight && chromatogram.getDataPoint(scanNumbers[end + 1]) != null) {

                            end++;
                        }

                        if (peakDuration.contains(retentionTimes[end] - retentionTimes[start])) {

                            resolvedPeaks.add(new ResolvedPeak(chromatogram, start, end));
                        }

                        start = end;
                    }
                }
            }
        }

        return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
    }

    private static int findRTIndex(final double[] rtMinutes, final double rtSec) {

        final int i = Arrays.binarySearch(rtMinutes, rtSec / SECONDS_PER_MINUTE);
        return i >= 0 ? i : -i - 2;
    }

    /**
     * Do peak picking using xcms::findPeaks.centWave.
     *
     * @param scanTime          retention times (for each scan).
     * @param intensity         intensity values (for each scan).
     * @param mz                fixed m/z value for EIC.
     * @param snrThreshold      signal:noise ratio threshold.
     * @param peakWidth         peak width range.
     * @param integrationMethod integration method.
     * @return a matrix with a row for each detected peak.
     */
    private static double[][] centWave(final double[] scanTime,
                                       final double[] intensity,
                                       final double mz, final double snrThreshold,
                                       final Range peakWidth, final PeakIntegrationMethod integrationMethod) {

        LOG.finest("Detecting peaks.");

        // Get R engine.
        final Rengine rEngine;
        try {
            rEngine = RUtilities.getREngine();
        }
        catch (Throwable t) {
            throw new IllegalStateException("XCMS requires R but it couldn't be loaded (" + t.getMessage() + ')');
        }

        final double[][] peaks;
        synchronized (RUtilities.R_SEMAPHORE) {

            // Load XCMS library.
            if (rEngine.eval("require(xcms)").asBool().isFALSE()) {

                throw new IllegalStateException("The \"xcms\" R package couldn't be loaded - is it installed in R?");
            }

            // Check version of XCMS.
            if (rEngine.eval("packageVersion('xcms') >= '" + XCMS_VERSION + '\'').asBool().isFALSE()) {

                throw new IllegalStateException(
                        "An old version of the XCMS package is installed in R - please update XCMS to version " +
                        XCMS_VERSION + " or later");
            }

            // Set vectors.
            rEngine.assign("scantime", scanTime);
            rEngine.assign("intensity", intensity);

            // Initialize.
            rEngine.eval("mz <- " + mz, false);
            rEngine.eval("numPoints <- length(intensity)", false);

            // Construct xcmsRaw object
            rEngine.eval("xRaw <- new(\"xcmsRaw\")", false);
            rEngine.eval("xRaw@tic <- intensity", false);
            rEngine.eval("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE, false);
            rEngine.eval("xRaw@scanindex <- 1:numPoints", false);
            rEngine.eval("xRaw@env$mz <- rep(mz, numPoints)", false);
            rEngine.eval("xRaw@env$intensity <- intensity", false);

            // Construct ROIs.
            rEngine.eval("ROIs <- list()", false);
            int roi = 1;
            for (int start = 0;
                 start < intensity.length;
                 start++) {

                // Found non-zero section.
                if (intensity[start] > 0.0) {

                    // Look for end.
                    int end = start + 1;
                    while (end < intensity.length && intensity[end] > 0.0) {

                        end++;
                    }

                    // Add ROI to list.
                    rEngine.eval("ROIs[[" + roi + "]] <- list('scmin'=" + (start + 1) + ", 'scmax'=" + end
                                 + ", 'mzmin'=mz, 'mzmax'=mz)",
                                 false);

                    // Next ROI.
                    start = end;
                    roi++;

                }
            }

            // Do peak picking.
            final REXP centWave =
                    roi <= 1 ? null :
                    rEngine.eval(
                            "findPeaks.centWave(xRaw, ppm=0, mzdiff=0, verbose=TRUE"
                            + ", peakwidth=c(" + peakWidth.getMin() * SECONDS_PER_MINUTE
                            + ", " + peakWidth.getMax() * SECONDS_PER_MINUTE + ')'
                            + ", snthresh=" + snrThreshold
                            + ", integrate=" + integrationMethod.getIndex()
                            + ", ROI.list=ROIs)");
            peaks = centWave == null ? null : centWave.asMatrix();
        }
        return peaks;
    }
}
