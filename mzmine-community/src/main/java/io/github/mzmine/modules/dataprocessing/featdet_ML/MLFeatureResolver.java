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
import io.github.mzmine.util.scans.PeakPickingModel.PeakPickingModel;
import io.github.mzmine.util.scans.PeakPickingModel.PeakPickingOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MLFeatureResolver extends AbstractResolver {

    private final ParameterSet parameters;
    private final double threshold;
    private final int regionSize;
    private final int overlap;
    private final int minWidth;
    private final boolean correctRanges;
    private final PeakPickingModel model;
    double[] xBuffer;
    double[] yBuffer;

    public MLFeatureResolver(ParameterSet parameterSet, ModularFeatureList flist) {
        super(parameterSet, flist);
        this.parameters = parameterSet;
        // THRESHOLD FiXED FOR DEBUGGING ONLY!
        this.threshold = 0.5;
        this.regionSize = 128;
        this.overlap = 32;
        this.minWidth = 2;
        this.correctRanges = true;
        this.model = new PeakPickingModel();
    }

    @Override
    public Class<? extends MZmineProcessingModule> getModuleClass() {
        return MLFeatureResolverModule.class;
    }

    public boolean isValidRange(double prob, int indexLeft, int indexPeak, int indexRight, double valuePeak) {
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

    // This should probably be located somewhere else
    public int[] roundFloatArray(float[] input, int min, int max) {
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(Math.min(Math.round(input[i]), max), min);
        }
        return output;
    }

    public List<Range<Double>> resolve(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new AssertionError("Lengths of x, y and indices array do not match.");
        }

        // Efficiency can be improved by having less overlap (and theorecically by
        // having longer regions)
        List<double[]> standardRegions = SplitSeries.extractRegionBatch(y, this.regionSize, this.overlap, "zero");
        List<double[]> standardRegionsRT = SplitSeries.extractRegionBatch(x, this.regionSize, this.overlap,
                "lastValue");
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
        List<int[]> peakIndices = resolvedRegions.stream().map(
                r -> roundFloatArray(r.peak(), 0, this.regionSize - 1)).collect(Collectors.toList());
        List<int[]> leftIndices = resolvedRegions.stream().map(
                r -> roundFloatArray(r.left(), 0, this.regionSize - 1))
                .collect(Collectors.toList());
        List<int[]> rightIndices = resolvedRegions.stream().map(
                r -> roundFloatArray(r.right(), 0, this.regionSize -1))
                .collect(Collectors.toList());

        int lenList = predProbs.size();
        int lenFeatures = predProbs.get(0).length;

        // iteraltes over lenList and lenFeatures and creates a range if the prediction
        // is greater than the threshold and left<right (the latter is just a safety
        // check)
        List<Range<Double>> resolved = new ArrayList<>();
        for (int i = 0; i < lenList; i++) {
            for (int j = 0; j < lenFeatures; j++) {
                double prob = predProbs.get(i)[j];
                int indexLeft = leftIndices.get(i)[j];
                int indexRight = rightIndices.get(i)[j];
                int indexPeak = peakIndices.get(i)[j];
                double peakValue = standardRegions.get(i)[indexPeak];
                if (this.isValidRange(prob, indexLeft, indexPeak, indexRight, peakValue)) {
                    Double left = Double.POSITIVE_INFINITY;
                    Double right = Double.POSITIVE_INFINITY;
                    if (this.correctRanges) {
                        int correctionRegion = 1;
                        for (int n = 0; n < correctionRegion + 1; n++) {
                            int currentLeftIndex = Math.max(
                                    Math.min(
                                            Math.min(leftIndices.get(i)[j] - n, indexPeak),
                                            this.regionSize - 1),
                                    0);
                            int currentRightIndex = Math.max(Math.min(
                                    Math.max(rightIndices.get(i)[j] + n, indexPeak), this.regionSize - 1), 0);
                            if (standardRegions.get(i)[currentLeftIndex] < left) {
                                left = standardRegions.get(i)[currentLeftIndex];
                                indexLeft = currentLeftIndex;
                            }
                            if (standardRegions.get(i)[currentRightIndex] < right) {
                                right = standardRegions.get(i)[currentRightIndex];
                                indexRight = currentRightIndex;
                            }
                        }
                    }
                    if (indexLeft + this.minWidth <= indexRight) {
                        left = Double.valueOf(standardRegionsRT.get(i)[indexLeft]);
                        right = Double.valueOf(standardRegionsRT.get(i)[indexRight]);
                        Range<Double> nextRange = Range.closed(left, right);
                        resolved.add(nextRange);
                    }
                }
            }
        }

        return resolved;
    }

    public void closeModel() {
        if (model != null) {
            model.closeModel();
        }
    }

}
