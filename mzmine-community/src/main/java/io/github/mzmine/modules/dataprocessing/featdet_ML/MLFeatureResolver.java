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
    private final PeakPickingModel model;
    double[] xBuffer;
    double[] yBuffer;

    public MLFeatureResolver(ParameterSet parameterSet, ModularFeatureList flist) {
        super(parameterSet, flist);
        this.parameters = parameterSet;
        // THRESHOLD FiXED FOR DEBUGGING ONLY!
        this.threshold = 0.8;
        this.regionSize = 128;
        this.overlap = 32;
        this.model = new PeakPickingModel();
    }

    @Override
    public Class<? extends MZmineProcessingModule> getModuleClass() {
        return MLFeatureResolverModule.class;
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
        List<double[]> predProbs = resolvedRegions.stream().map(r -> r.prob())
                .collect(Collectors.toList());

        List<double[]> predPeaks = resolvedRegions.stream().map(r -> r.peak())
                .collect(Collectors.toList());

        // Rounds the predictions to Indices and make sure they are insde the bounds
        List<int[]> leftIndices = resolvedRegions.stream().map(
                r -> Arrays.stream(r.left()).mapToInt(d -> (int) Math.round(d))
                        .map(i -> Math.max(Math.min(i, this.regionSize - 1), 0)).toArray())
                .collect(Collectors.toList());
        List<int[]> rightIndices = resolvedRegions.stream().map(
                r -> Arrays.stream(r.right()).mapToInt(d -> (int) Math.round(d))
                        .map(i -> Math.max(Math.min(i, this.regionSize - 1), 0)).toArray())
                .collect(Collectors.toList());

        // Converts indices to retention times and writes times into regions
        // Getting the compiler to infer the correct types here is horrible
        int lenList = predProbs.size();
        int lenFeatures = predProbs.get(0).length;

        // iteraltes over lenList and lenFeatures and creates a range if the prediction
        // is greater than the threshold and left<right (the latter is just a safety
        // check)
        List<Range<Double>> resolved = new ArrayList<>();
        for (int i = 0; i < lenList; i++) {
            for (int j = 0; j < lenFeatures; j++) {
                // only selects peaks that are in the inside of the selected region
                if ((predProbs.get(i)[j] > this.threshold) && (this.overlap / 2) < predPeaks.get(i)[j]
                        && predPeaks.get(i)[j] < (this.regionSize - this.overlap / 2)) {
                    Double left = Double.valueOf(standardRegionsRT.get(i)[leftIndices.get(i)[j]]);
                    Double right = Double.valueOf(standardRegionsRT.get(i)[rightIndices.get(i)[j]]);
                    if (left < right) {
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
