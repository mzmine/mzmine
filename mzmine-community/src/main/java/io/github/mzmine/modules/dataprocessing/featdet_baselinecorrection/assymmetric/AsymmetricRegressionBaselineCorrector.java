/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.assymmetric;

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsymmetricRegressionBaselineCorrector implements BaselineCorrector {

  private final MemoryMapStorage storage;
  private final double lambda;
  private final double p;
  private final int maxIter;
  private final String suffix;
  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];

  public AsymmetricRegressionBaselineCorrector() {
    this(null, 1E6, 0.001, 1, "");
  }

  public AsymmetricRegressionBaselineCorrector(MemoryMapStorage storage, double lambda, double p,
      int maxIter, String suffix) {
    this.storage = storage;
    this.lambda = lambda;
    this.p = p;
    this.maxIter = maxIter;
    this.suffix = suffix;
  }

  public static double[] asymmetricLeastSquares(double[] y, double lambda, double p, int maxIter) {
    int n = y.length;
    RealMatrix D = new Array2DRowRealMatrix(n, n);

    // Building the finite difference matrix D
    for (int i = 0; i < n - 2; i++) {
      D.setEntry(i, i, 1);
      D.setEntry(i, i + 1, -2);
      D.setEntry(i, i + 2, 1);
    }

    RealMatrix DTD = D.transpose().multiply(D).scalarMultiply(lambda);
    RealVector w = new ArrayRealVector(n, 1.0);
    RealVector z = new ArrayRealVector(y);

    for (int iter = 0; iter < maxIter; iter++) {
      RealMatrix W = new Array2DRowRealMatrix(n, n);
      for (int i = 0; i < n; i++) {
        W.setEntry(i, i, w.getEntry(i));
      }

      RealMatrix A = W.add(DTD);
      RealVector b = W.operate(z);

      DecompositionSolver solver = new LUDecomposition(A).getSolver();
      RealVector x = solver.solve(b);

      for (int i = 0; i < n; i++) {
        double residual = z.getEntry(i) - x.getEntry(i);
        w.setEntry(i, residual > 0 ? p : 1 - p);
      }
    }

    double[] baselineArray = w.toArray();
    double[] corrected = new double[n];
    for (int i = 0; i < n; i++) {
      corrected[i] = y[i] - baselineArray[i];
    }

    return corrected;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
    }

    for (int i = 0; i < numValues; i++) {
      xBuffer[i] = timeSeries.getRetentionTime(i);
      if (xBuffer[i] < xBuffer[Math.max(i - 1, 0)]) {
        throw new IllegalStateException();
      }
    }

    if (timeSeries instanceof FeatureDataAccess access) {
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        yBuffer[i] = access.getIntensity(i);
      }
    } else {
      yBuffer = timeSeries.getIntensityValues(yBuffer);
    }

    final double[] corrected = asymmetricLeastSquares(yBuffer, lambda, p, maxIter);

    return switch (timeSeries) {
      case IonSpectrumSeries<?> s -> (T) s.copyAndReplace(storage, s.getMzValues(new double[0]),
          Arrays.copyOfRange(corrected, 0, numValues));
      case OtherTimeSeries o -> (T) o.copyAndReplace(
          o.getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
              .getMemoryMapStorage(), Arrays.copyOfRange(corrected, 0, numValues),
          o.getName() + " " + suffix);
      default -> throw new IllegalStateException(
          "Unexpected time series: " + timeSeries.getClass().getName());
    };
  }

  @Override
  public BaselineCorrector newInstance(BaselineCorrectionParameters parameters,
      MemoryMapStorage storage, FeatureList flist) {
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    return new AsymmetricRegressionBaselineCorrector(storage,
        embedded.getValue(AsymmetricRegressionBaselineCorrectorParameters.lambda),
        embedded.getValue(AsymmetricRegressionBaselineCorrectorParameters.p),
        embedded.getValue(AsymmetricRegressionBaselineCorrectorParameters.maxIterations),
        parameters.getValue(BaselineCorrectionParameters.suffix));
  }

  @Override
  public @NotNull String getName() {
    return "Asymmetric regression baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AsymmetricRegressionBaselineCorrectorParameters.class;
  }
}
