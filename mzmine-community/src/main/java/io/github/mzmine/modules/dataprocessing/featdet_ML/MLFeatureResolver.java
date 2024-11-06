/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_ML;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.WriterOptions;
import io.github.mzmine.util.scans.PeakPickingModel.PeakPickingModel;
import io.github.mzmine.util.scans.PeakPickingModel.PeakPickingOutput;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import org.jdom2.internal.ArrayCopy;

public class MLFeatureResolver extends AbstractResolver {

    private final ParameterSet parameters;
    private final double threshold;
    private final int regionSize;
    private final int overlap;
    private final boolean withOffset;
    private final int minWidth;
    // private final boolean resizeRanges;
    private final boolean correctRanges;
    private final boolean correctIntersections;
    private final float minSlope;
    private final PeakPickingModel model;

    private final Logger logger;

    // for debugging
    private final int numFeaturesOffset;

    double[] xBuffer;
    double[] yBuffer;

    public MLFeatureResolver(ParameterSet parameterSet, ModularFeatureList flist) {
        super(parameterSet, flist);
        this.numFeaturesOffset = 8;

        this.parameters = parameterSet;
        this.threshold = parameterSet.getParameter(MLFeatureResolverParameters.threshold).getValue();
        this.regionSize = 128;
        this.overlap = 32;
        // this is for debugging purposes. In the final version this should either
        // alawys be true or false, depending on what works best.
        this.withOffset = parameterSet.getParameter(MLFeatureResolverParameters.withOffset).getValue();
        this.minWidth = parameterSet.getParameter(MLFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS).getValue();
        // this.resizeRanges =
        // parameterSet.getParameter(MLFeatureResolverParameters.resizeRanges).getValue();
        this.correctRanges = parameterSet.getParameter(MLFeatureResolverParameters.correctRanges).getValue();
        this.correctIntersections = parameterSet.getParameter(MLFeatureResolverParameters.correctIntersections)
                .getValue();
        //minimal slope (normalized with respect to intensity) that has to be present before range correction stops
        // I.e. the next intensity has to be at least 20% less than the previous
        this.minSlope = (float) 0.0;
        this.model = new PeakPickingModel(this.withOffset, this.numFeaturesOffset);

        this.logger = Logger.getLogger("MLFeatureResolverLogger");

    }

    @Override
    public Class<? extends MZmineProcessingModule> getModuleClass() {
        return MLFeatureResolverModule.class;
    }

    private boolean isValidRange(double prob, int indexLeft, int indexPeak, int indexRight,
            double valuePeak) {
        if (prob < this.threshold) {
            return false;
        }
        if (indexLeft >= indexPeak) {
            return false;
        }
        if (indexRight <= indexPeak) {
            return false;
        }
        if (valuePeak == 0.0) {
            return false;
        }
        if (indexPeak < this.overlap / 2) {
            return false;
        }
        if (indexPeak > (this.regionSize - this.overlap / 2)) {
            return false;
        }
        return true;
    }

    private double calculateDerivative(double[] intensity, int index, int epsilon) {
        int indexEpsilon = Math.min(Math.max(index + epsilon, 0), intensity.length);
        return (intensity[indexEpsilon] - intensity[index]) / epsilon;
    }

    // corrects bounds of ranges in case they overlap
    // this assumes that there are at most two ranges overlapping at the same point
    // (which I hope is reasonable)
    private List<Range<Double>> correctInter(List<Range<Double>> ranges, double[] times, double[] intensities) {
        int numRanges = ranges.size();
        // if there are less than two ranges there is nothing to do
        if (numRanges < 2) {
            return ranges;
        }
        List<Range<Double>> correctedRanges = new ArrayList<>();
        Range<Double> currentRange = ranges.get(0);
        for (int i = 1; i < numRanges; i++) {
            Range<Double> nextRange = ranges.get(i);
            Double currentRight = currentRange.upperEndpoint();
            Double nextLeft = nextRange.lowerEndpoint();
            // use currentRange.isConnected(nextRanges) or something
            if (currentRight <= nextLeft) {
                correctedRanges.add(currentRange);
                currentRange = nextRange;
                continue;
            }
            int currentRightIndex = Arrays.binarySearch(times, currentRight);
            int nextLeftIndex = Arrays.binarySearch(times, nextLeft);

            int minIndex = 0;
            double minIntensity = Double.POSITIVE_INFINITY;
            for (int j = nextLeftIndex; j <= currentRightIndex; j++) {
                if (intensities[j] < minIntensity) {
                    minIntensity = intensities[j];
                    minIndex = j;
                }
            }
            double minIntensityTime = times[minIndex];
            if (currentRange.lowerEndpoint() >= minIntensityTime) {
                logger.finer("Incompatible range endpoints. Continue with processing.");
            } else {
                Range<Double> updatedCurrentRange = Range.closed(currentRange.lowerEndpoint(), minIntensityTime);
                correctedRanges.add(updatedCurrentRange);
            }
            // updates for the next iteration. Next range (with corrected left bound) is
            // now
            // the new current range
            if (minIntensityTime >= nextRange.upperEndpoint()) {
                logger.finer("Incompatible range endpoints when setting next current range. Continue with processing.");
                i++;
                if (i < numRanges) {
                    currentRange = ranges.get(i);
                }
            } else {
                currentRange = Range.closed(minIntensityTime, nextRange.upperEndpoint());
            }
        }
        // need to add last Range because it has no next range and the previous step
        // does not apply
        correctedRanges.add(currentRange);
        return correctedRanges;
    }

    // This should probably be located somewhere else
    public int[] roundFloatArray(float[] input, int min, int max) {
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(Math.min(Math.round(input[i]), max), min);
        }
        return output;
    }

    private double findTime(double[] timeArray, double[] intensityArray, double intensityToFind) {
        if (timeArray.length != intensityArray.length) {
            System.out.println(
                    "Arrays lenghts to not match when looking for retention time corresponding to intensity value");
            return 0.;
        }
        for (int i = 0; i < timeArray.length; i++) {
            if (intensityArray[i] != intensityToFind) {
                continue;
            }
            return timeArray[i];
        }
        System.out.println("Intensity could not be found");
        return 0.;
    }

    public String[] doubleArrayToString(double[] time, double[] intensity) {
        String[] outputArray = new String[time.length + 1];
        String header = "x\ty\n";
        outputArray[0] = header;
        for (int i = 1; i < time.length + 1; i++) {
            String nextString = String.valueOf((float) time[i - 1]) + "\t" + String.valueOf((float) intensity[i - 1])
                    + "\n";
            outputArray[i] = nextString;
        }
        return outputArray;
    }

    public List<Range<Double>> resolve(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new AssertionError("Lengths of x, y and indices array do not match.");
        }

        // Efficiency can be improved by having less overlap (and theorecically by
        // having longer regions)
        List<double[]> standardRegions = SplitSeries.extractRegionBatch(y, this.regionSize,
                this.overlap, "zero", true);
        List<double[]> standardRegionsRT = SplitSeries.extractRegionBatch(x, this.regionSize,
                this.overlap, "time", false);
        // try (var writer = new FileWriter("/home/max/Programming/testFeature.tsv")){
        // double[] time = standardRegionsRT.get(0);
        // double[] intensity = standardRegions.get(0);
        // String[] rows = doubleArrayToString(time , intensity);
        // for(int i=0; i<rows.length;i++){
        // writer.write(rows[i]);
        // }
        // } catch(Exception e) {
        // e.printStackTrace();
        // throw new Error("Error instanciating writer");
        // }
        List<PeakPickingOutput> resolvedRegions;
        try {
            resolvedRegions = model.predictor.batchPredict(standardRegions);
        } catch (Exception e) {
            System.out.println("Error during prediction.");
            e.printStackTrace();
            return null;
        }
        // extracts the different predictions and peaks from PeakPickingOutput
        List<float[]> predProbs = resolvedRegions.stream().map(r -> r.prob())
                .collect(Collectors.toList());

        // List<double[]> predPeaks = resolvedRegions.stream().map(r -> r.peak())
        // .collect(Collectors.toList());

        // Rounds the predictions to Indices and make sure they are insde the bounds
        List<int[]> peakIndices = resolvedRegions.stream()
                .map(r -> roundFloatArray(r.peak(), 0, this.regionSize - 1)).collect(Collectors.toList());
        List<int[]> leftIndices = resolvedRegions.stream()
                .map(r -> roundFloatArray(r.left(), 0, this.regionSize - 1)).collect(Collectors.toList());
        List<int[]> rightIndices = resolvedRegions.stream()
                .map(r -> roundFloatArray(r.right(), 0, this.regionSize - 1)).collect(Collectors.toList());

        // number of standard regions
        int lenList = predProbs.size();
        // number of predictions per standard region
        int lenFeatures = predProbs.get(0).length;

        // iteraltes over lenList and lenFeatures and creates a range if the prediction
        // is greater than the threshold and left<right (the latter is just a safety
        // check)
        List<Range<Double>> resolved = new ArrayList<>();
        List<Double> peakTimes = new ArrayList<>();
        for (int i = 0; i < lenList; i++) {
            for (int j = 0; j < lenFeatures; j++) {
                double prob = predProbs.get(i)[j];
                int indexLeft = leftIndices.get(i)[j];
                int indexRight = rightIndices.get(i)[j];
                int indexPeak = peakIndices.get(i)[j];
                double peakValue = standardRegions.get(i)[indexPeak];
                if (!this.isValidRange(prob, indexLeft, indexPeak, indexRight, peakValue)) {
                    continue;
                }

                if (this.correctRanges) {
                    int currentLeftIndex = indexLeft;
                    while (currentLeftIndex > 0) {
                        // left derivative at currentLeftIndex normalized by intensity at
                        // currentLeftIndex
                        if (this.calculateDerivative(standardRegions.get(i), currentLeftIndex, -1) > this.minSlope
                                * standardRegions.get(i)[currentLeftIndex]) {
                            // positive derivative means intensity if increasing with increasing retention
                            // time.
                            // if the derivative is not small enough anymore (in relation to the current
                            // intensity) we stop the process.
                            currentLeftIndex--;
                        } else if (this.calculateDerivative(standardRegions.get(i), currentLeftIndex, 1)<= -this.minSlope * standardRegions.get(i)[currentLeftIndex]){
                            currentLeftIndex++;
                        }
                        break;
                    }
                    indexLeft = currentLeftIndex;

                    int currentRightIndex = indexRight;
                    while (currentRightIndex < this.regionSize - 1) {
                        // right derivative at currentRightIndex normalized by intensity at
                        // currentRightIndex
                        if (this.calculateDerivative(standardRegions.get(i), currentRightIndex, 1) < -this.minSlope
                                * standardRegions.get(i)[currentRightIndex]) {
                            // negative derivative means intensity is falling with increasing retention
                            // time.
                            // if the derivative is not small enough anymore (in relation to the current
                            // intensity) we stop the process.
                            currentRightIndex++;
                        } else if (this.calculateDerivative(standardRegions.get(i), currentRightIndex, -1)>= this.minSlope * standardRegions.get(i)[currentRightIndex]){
                            currentRightIndex --;
                        }
                        break;
                    }
                    indexRight = currentRightIndex;
                }
                double[] currentRegion = Arrays.copyOfRange(standardRegions.get(i), indexLeft, indexRight + 1);
                double currentMaxIntensity = Arrays.stream(currentRegion).max().orElse(0);
                double currentMaxRT = findTime(standardRegionsRT.get(i), standardRegions.get(i), currentMaxIntensity);
                // checks if the current peak is a duplicate of the previous one by
                // comparing the retention times at which the maximal intensity is reached
                if (peakTimes.contains(currentMaxRT)) {
                    continue;
                }
                if (indexLeft + this.minWidth <= indexRight) {
                    peakTimes.add(currentMaxRT);
                    double left = Double.valueOf(standardRegionsRT.get(i)[indexLeft]);
                    double right = Double.valueOf(standardRegionsRT.get(i)[indexRight]);
                    Range<Double> nextRange = Range.closed(left, right);
                    resolved.add(nextRange);
                }
            }
        }
        if (this.correctIntersections) {
            resolved = correctInter(resolved, x, y);
        }
        return resolved;
    }

    public void closeModel() {
        if (model != null) {
            model.closeModel();
        }
    }

}
