/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;

public class ManualLoessSmoothingParameters extends SimpleParameterSet {

  public static final DoubleParameter bandwidth = new DoubleParameter("LOESS bandwidth",
      "Fraction of usable QCs in each local fit, rounded up to a whole neighborhood. At least "
          + "polynomial degree + 2 neighbors are needed. Larger values give smoother curves.",
      new DecimalFormat("0.###"), .75, 0d, 1d);
  public static final IntegerParameter polynomialDegree = new IntegerParameter("Polynomial degree",
      "1 for local linear or 2 for local quadratic fitting.", 1, 1, 2);
  public static final IntegerParameter robustnessIterations = new IntegerParameter(
      "Robustness iterations",
      "Number of robust bisquare reweighting iterations that down-weight outlier QCs. "
          + "0 disables the reweighting.", 2, 0, Integer.MAX_VALUE);

  public ManualLoessSmoothingParameters() {
    super(bandwidth, polynomialDegree, robustnessIterations);
  }

  public static @NotNull ManualLoessSmoothingParameters create(final double bandwidthValue,
      final int degree, final int robustness) {
    final ManualLoessSmoothingParameters parameters = (ManualLoessSmoothingParameters) new ManualLoessSmoothingParameters().cloneParameterSet();
    parameters.setParameter(bandwidth, bandwidthValue);
    parameters.setParameter(polynomialDegree, degree);
    parameters.setParameter(robustnessIterations, robustness);
    return parameters;
  }
}
