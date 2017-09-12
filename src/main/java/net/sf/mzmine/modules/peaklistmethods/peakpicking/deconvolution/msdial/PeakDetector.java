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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.msdial;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.util.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class PeakDetector {

    final double minDataPoints;
    final double minAmplitude;
    final double amplitudeNoiseFold;
    final double slopeNoiseFold;
    final double peakTopNoiseFold;

    public PeakDetector(double minDataPoints, double minAmplitude, double amplitudeNoiseFold,
                        double slopeNoiseFold, double peakTopNoiseFold)
    {
        this.minDataPoints = minDataPoints;
        this.minAmplitude = minAmplitude;
        this.amplitudeNoiseFold = amplitudeNoiseFold;
        this.slopeNoiseFold = slopeNoiseFold;
        this.peakTopNoiseFold = peakTopNoiseFold;
    }

    public List<Range<Integer>> run(double[] intensities, double[] retTimes)
    {
        final int numPoints = intensities.length;

        if (retTimes.length != numPoints)
            throw new IllegalArgumentException("The sizes of intensity and retention time arrays do not match");

        double[] firstDerivatives = new double[numPoints];
        double[] secondDerivatives = new double[numPoints];

        final double[] firstDiffCoeff = new double[] { -0.2, -0.1, 0, 0.1, 0.2 };
        final double[] secondDiffCoeff = new double[] { 0.14285714, -0.07142857, -0.1428571, -0.07142857, 0.14285714 };
        final int halfPoints = firstDiffCoeff.length / 2;

        double maxFirstDerivative = Double.MIN_VALUE;
        double maxSecondDerivative = Double.MIN_VALUE;
        double maxAmplitudeDerivative = Double.MIN_VALUE;

        for (int i = halfPoints; i < numPoints - halfPoints; ++i)
        {
            for (int j = 0; j < firstDiffCoeff.length; ++j)
            {
                firstDerivatives[i] += firstDiffCoeff[j] * intensities[i + j - halfPoints];
                secondDerivatives[i] += secondDiffCoeff[j] * intensities[i + j - halfPoints];
            }

            if (Math.abs(firstDerivatives[i]) > maxFirstDerivative)
                maxFirstDerivative = Math.abs(firstDerivatives[i]);
            if (secondDerivatives[i] < 0 && maxSecondDerivative < -secondDerivatives[i])
                maxSecondDerivative = -secondDerivatives[i];
            if (Math.abs(intensities[i] - intensities[i - 1]) > maxAmplitudeDerivative)
                maxAmplitudeDerivative = Math.abs(intensities[i] - intensities[i - 1]);
        }

        Noise noise = estimateNoise(intensities, firstDerivatives, secondDerivatives,
                new NoiseThresholds(maxAmplitudeDerivative, maxFirstDerivative, maxSecondDerivative));

        List<Range<Integer>> peakRanges = new ArrayList<>();

        for (int i = 0; i < numPoints; ++i)
        {
            System.out.println(i);

            if (i >= numPoints - 1 - minDataPoints) break;

            int peakStart;
            int peakEnd;

            // Left edge criteria
            if (firstDerivatives[i] > noise.slope * slopeNoiseFold
                    && firstDerivatives[i + 1] > noise.slope * slopeNoiseFold)
            {
                List<double[]> dataPoints = new ArrayList<>();
                dataPoints.add(new double[] {intensities[i], firstDerivatives[i], secondDerivatives[i]});
                peakStart = i;

                // Search real left edge within 5 data points
                for (int j = 0; j <= 5; ++j) {
                    if (i - j - 1 < 0) break;
                    if (intensities[i - j] <= intensities[i - j - 1]) break;
                    if (intensities[i - j] > intensities[i - j - 1]) {
                        dataPoints.set(0, new double[]{intensities[i - j - 1], firstDerivatives[i - j - 1], secondDerivatives[i - j - 1]});
                        peakStart = i - j - 1;
                    }
                }

                // Right edge criteria
                boolean peakTopCheck = false;
                int peakTopCheckPoint = i;
                while (true)
                {
                    if (i + 1 == numPoints - 1) break;

                    ++i;
                    dataPoints.add(new double[] {intensities[i], firstDerivatives[i], secondDerivatives[i]});

                    if (!peakTopCheck && firstDerivatives[i - 1] > 0.0 && firstDerivatives[i] < 0.0
                            && secondDerivatives[i] < -noise.peakTop * peakTopNoiseFold)
                    {
                        peakTopCheck = true;
                        peakTopCheckPoint = i;
                    }

                    if (peakTopCheck && peakTopCheckPoint + 2 + minDataPoints / 2 <= i - 1
                            && firstDerivatives[i - 1] > -noise.slope * slopeNoiseFold
                            && firstDerivatives[i] > -noise.slope * slopeNoiseFold)
                        break;
                }

                peakEnd = i;
                // Search real right edge within 5 data points
                // Case: wrong edge is in left of real edge
                boolean rightCheck = false;
                for (int j = 0; j <= 5; ++j) {
                    if (i + j + 1 > numPoints - 1) break;
                    if (intensities[i + j] <= intensities[i + j + 1]) break;
                    if (intensities[i + j] > intensities[i + j + 1]) {
                        dataPoints.add(new double[]{intensities[i + j + 1], firstDerivatives[i + j + 1], secondDerivatives[i + j + 1]});
                        rightCheck = true;
                        peakEnd = i + j + 1;
                    }
                }
                // Case: wrong edge is in right of real edge
                if (!rightCheck) {
                    for (int j = 0; j <= 5; ++j) {
                        if (i - j - 1 < 0) break;
                        if (intensities[i - j] <= intensities[i - j - 1]) break;
                        if (intensities[i - j] > intensities[i - j - 1]) {
                            dataPoints.remove(dataPoints.size() - 1);
                            peakEnd = i - j - 1;
                        }
                    }
                }

                // Check minimum datapoint criteria
                if (dataPoints.size() < minDataPoints) continue;

                // Check peak criteria
                double peakTopIntensity = Double.MIN_VALUE;
                int peakTopId = -1;
                for (int j = 0; j < dataPoints.size(); ++j)
                    if (peakTopIntensity < dataPoints.get(j)[0]) {
                        peakTopIntensity = dataPoints.get(j)[0];
                        peakTopId = j;
                    }
                if (dataPoints.get(peakTopId)[0] - dataPoints.get(0)[0] < Math.max(minAmplitude, noise.amplitude * amplitudeNoiseFold)
                        || dataPoints.get(peakTopId)[0] - dataPoints.get(dataPoints.size() - 1)[0] < Math.max(minAmplitude, noise.amplitude * amplitudeNoiseFold))
                    continue;

                peakRanges.add(Range.closed(peakStart, peakEnd));
            }
        }
        return peakRanges;
    }

    private Noise estimateNoise(double[] intensities, double[] firstDerivatives, double[] secondDerivatives,
                                NoiseThresholds thresholds)
    {
        final int numPoints = intensities.length;

        List<Double> amplitudeNoiseCandidates = new ArrayList<>();
        List<Double> slopeNoiseCandidates = new ArrayList<>();
        List<Double> peakTopNoiseCandidates = new ArrayList<>();

        for (int i = 2; i < numPoints - 2; ++i)
        {
            double amplitudeDifference = Math.abs(intensities[i + 1] - intensities[i]);
            if (amplitudeDifference < thresholds.amplitude && amplitudeDifference > 0.0)
                amplitudeNoiseCandidates.add(amplitudeDifference);

            if (Math.abs(firstDerivatives[i]) < thresholds.slope && Math.abs(firstDerivatives[i]) > 0.0)
                slopeNoiseCandidates.add(Math.abs(firstDerivatives[i]));

            if (secondDerivatives[i] < 0.0 && Math.abs(secondDerivatives[i]) < thresholds.peakTop)
                peakTopNoiseCandidates.add(Math.abs(secondDerivatives[i]));
        }

        Collections.sort(amplitudeNoiseCandidates);
        Collections.sort(slopeNoiseCandidates);
        Collections.sort(peakTopNoiseCandidates);

        Noise noise = new Noise();
        noise.amplitude = amplitudeNoiseCandidates.isEmpty() ?
                0.0001 :
                amplitudeNoiseCandidates.get(amplitudeNoiseCandidates.size() / 2);
        noise.slope = slopeNoiseCandidates.isEmpty() ?
                0.0001 :
                slopeNoiseCandidates.get(slopeNoiseCandidates.size() / 2);
        noise.peakTop = peakTopNoiseCandidates.isEmpty() ?
                0.0001 :
                peakTopNoiseCandidates.get(peakTopNoiseCandidates.size() / 2);

        return noise;
    }

    private class Noise {
        double amplitude;
        double slope;
        double peakTop;
    }

    private class NoiseThresholds {
        double amplitude;
        double slope;
        double peakTop;

        NoiseThresholds(double maxAmplitudeDerivative, double maxFirstDerivative, double maxSecondDerivative) {
            amplitude = 0.05 * maxAmplitudeDerivative;
            slope = 0.05 * maxFirstDerivative;
            peakTop = 0.05 * maxSecondDerivative;
        }
    }
}
