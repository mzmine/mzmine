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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.MIN_ABSOLUTE_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.MIN_RATIO;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.MIN_RELATIVE_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.PEAK_DURATION;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.SEARCH_RT_RANGE;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.R.RSessionWrapper;

/**
 * This peak recognition method searches for local minima in the chromatogram.
 * If a local minimum is a local minimum even at a given retention time range,
 * it is considered a border between two peaks.
 */
public class MinimumSearchPeakDetector implements PeakResolver {

    @Override
    public @Nonnull String getName() {
        return "Local minimum search";
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            ParameterSet parameters, RSessionWrapper rSession) {
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

        final int lastScan = scanCount - 1;

        assert scanCount > 0;

        final Range<Double> peakDuration = parameters
                .getParameter(PEAK_DURATION).getValue();
        final double searchRTRange = parameters.getParameter(SEARCH_RT_RANGE)
                .getValue();

        final double minRatio = parameters.getParameter(MIN_RATIO).getValue();
        final double minHeight = Math.max(
                parameters.getParameter(MIN_ABSOLUTE_HEIGHT).getValue(),
                parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue()
                        * chromatogram.getHeight());

        final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>();

        // First, remove all data points below chromatographic threshold.
        final double chromatographicThresholdLevel = MathUtils.calcQuantile(
                intensities,
                parameters.getParameter(CHROMATOGRAPHIC_THRESHOLD_LEVEL)
                        .getValue());
        for (int i = 0; i < intensities.length; i++) {
            if (intensities[i] < chromatographicThresholdLevel) {
                intensities[i] = 0.0;
            }
        }

        // Current region is a region between two minima, representing a
        // candidate for a resolved peak.
        startSearch: for (int currentRegionStart = 0; currentRegionStart < lastScan
                - 2; currentRegionStart++) {

            // Find at least two consecutive non-zero data points
            if (intensities[currentRegionStart] == 0.0
                    || intensities[currentRegionStart + 1] == 0.0)
                continue;

            double currentRegionHeight = intensities[currentRegionStart];

            endSearch: for (int currentRegionEnd = currentRegionStart
                    + 1; currentRegionEnd < scanCount; currentRegionEnd++) {

                // Update height of current region.
                currentRegionHeight = Math.max(currentRegionHeight,
                        intensities[currentRegionEnd]);

                // If we reached the end, or if the next intensity is 0, we
                // have to stop here.
                if (currentRegionEnd == lastScan
                        || intensities[currentRegionEnd + 1] == 0.0) {

                    // Find the intensity at the sides (lowest data points).
                    final double peakMinLeft = intensities[currentRegionStart];
                    final double peakMinRight = intensities[currentRegionEnd];

                    // Check the shape of the peak.
                    if (currentRegionHeight >= minHeight
                            && currentRegionHeight >= peakMinLeft * minRatio
                            && currentRegionHeight >= peakMinRight * minRatio
                            && peakDuration
                                    .contains(retentionTimes[currentRegionEnd]
                                            - retentionTimes[currentRegionStart])) {

                        resolvedPeaks.add(new ResolvedPeak(chromatogram,
                                currentRegionStart, currentRegionEnd));
                    }

                    // Set the next region start to current region end - 1
                    // because it will be immediately
                    // increased +1 as we continue the for-cycle.
                    currentRegionStart = currentRegionEnd - 1;
                    continue startSearch;
                }

                // Minimum duration of peak must be at least searchRTRange.
                if (retentionTimes[currentRegionEnd]
                        - retentionTimes[currentRegionStart] >= searchRTRange) {

                    // Set the RT range to check
                    final Range<Double> checkRange = Range.closed(
                            retentionTimes[currentRegionEnd] - searchRTRange,
                            retentionTimes[currentRegionEnd] + searchRTRange);

                    // Search if there is lower data point on the left from
                    // current peak i.
                    for (int i = currentRegionEnd - 1; i > 0; i--) {

                        if (!checkRange.contains(retentionTimes[i]))
                            break;

                        if (intensities[i] < intensities[currentRegionEnd]) {

                            continue endSearch;
                        }
                    }

                    // Search on the right from current peak i.
                    for (int i = currentRegionEnd + 1; i < scanCount; i++) {

                        if (!checkRange.contains(retentionTimes[i]))
                            break;

                        if (intensities[i] < intensities[currentRegionEnd]) {

                            continue endSearch;
                        }
                    }

                    // Find the intensity at the sides (lowest data points).
                    final double peakMinLeft = intensities[currentRegionStart];
                    final double peakMinRight = intensities[currentRegionEnd];

                    // If we have reached a minimum which is non-zero, but
                    // the peak shape would not fulfill the
                    // ratio condition, continue searching for next minimum.
                    if (currentRegionHeight >= peakMinRight * minRatio) {

                        // Check the shape of the peak.
                        if (currentRegionHeight >= minHeight
                                && currentRegionHeight >= peakMinLeft * minRatio
                                && currentRegionHeight >= peakMinRight
                                        * minRatio
                                && peakDuration.contains(
                                        retentionTimes[currentRegionEnd]
                                                - retentionTimes[currentRegionStart])) {

                            resolvedPeaks.add(new ResolvedPeak(chromatogram,
                                    currentRegionStart, currentRegionEnd));
                        }

                        // Set the next region start to current region end-1
                        // because it will be immediately
                        // increased +1 as we continue the for-cycle.
                        currentRegionStart = currentRegionEnd - 1;
                        continue startSearch;
                    }
                }
            }
        }

        return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return MinimumSearchPeakDetectorParameters.class;
    }

    @Override
    public boolean getRequiresR() {
        return false;
    }

    @Override
    public String[] getRequiredRPackages() {
        return null;
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return null;
    }

}
