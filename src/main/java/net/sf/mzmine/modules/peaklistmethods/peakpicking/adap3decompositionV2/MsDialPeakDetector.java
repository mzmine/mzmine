/*
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import com.google.common.collect.Range;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.workflow.decomposition.PeakDetector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.REngineType;
import net.sf.mzmine.util.R.RSessionWrapper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class implements a peak builder using a match score to link MzPeaks in
 * the axis of retention time. Also uses Savitzky-Golay coefficients to
 * calculate the first and second derivative (smoothed) of raw data points
 * (intensity) that conforms each peak. The first derivative is used to
 * determine the peak's range, and the second derivative to determine the
 * intensity of the peak.
 */
public class MsDialPeakDetector implements PeakResolver {

    public @Nonnull
    String getName() {
        return "MsDial";
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            ParameterSet parameters,
            RSessionWrapper rSession, double msmsRange, double rTRangeMSMS) {

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

        List<RetTimeClusterer.Item> peakRanges = findPeakRanges(
                new Chromatogram(retentionTimes, intensities), chromatogram.getMZ(), parameters);

        final List<Feature> resolvedPeaks = new ArrayList<>(peakRanges.size());
        for (RetTimeClusterer.Item range : peakRanges) {
            int peakStart = Arrays.binarySearch(retentionTimes, range.getInterval().lowerEndpoint());
            int peakEnd = Arrays.binarySearch(retentionTimes, range.getInterval().upperEndpoint());
            resolvedPeaks.add(new ResolvedPeak(chromatogram, peakStart, peakEnd, msmsRange, rTRangeMSMS));
        }

        return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
    }

    public static List<RetTimeClusterer.Item> findPeakRanges(@Nonnull Chromatogram c, double mz, @Nonnull ParameterSet ps) {
        Integer numSmoothingPoints = ps.getParameter(MsDialPeakDetectorParameters.NUM_SMOOTHING_POINTS).getValue();
        Double minAmplitude = ps.getParameter(MsDialPeakDetectorParameters.MIN_PEAK_HEIGHT).getValue();
        Range<Double> durationRange = ps.getParameter(MsDialPeakDetectorParameters.PEAK_DURATION).getValue();

        if (numSmoothingPoints != null && minAmplitude != null && durationRange != null)
            return new PeakDetector(numSmoothingPoints, minAmplitude, durationRange).run(c, mz);
        else
            return new ArrayList<>(0);
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return MsDialPeakDetectorParameters.class;
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

    @Override
    public REngineType getREngineType(@Nonnull ParameterSet parameters) {
        return REngineType.RSERVE;
    }
}
