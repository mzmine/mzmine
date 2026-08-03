/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Developer-only, on-demand recompute of the isotope finder scoring diagnostics for a single row.
 * Reconstructs the engine from the feature list's most recent {@link IsotopeFinderModule}
 * applied-method parameters and re-runs detection with {@code keepDiagnostics} on, so the compound
 * dashboard can visualise how the algorithm scored a charge hypothesis (predicted envelope, sliding
 * placement, per-signal element attribution, per-charge scores) without persisting anything.
 * <p>
 * Note: this is a single-scan re-run and therefore does not reproduce the optional FWHM cross-scan
 * refinement that the processing run may have applied to the stored pattern; the recomputed numbers
 * can differ slightly from the persisted pattern.
 */
public final class IsotopeFinderDiagnostics {

  private IsotopeFinderDiagnostics() {
  }

  /**
   * Recompute the detection result (with diagnostics) for {@code row} on {@code scan}.
   *
   * @param row  the feature list row to score.
   * @param scan the MS1 scan to score against (typically the row's representative scan).
   * @return the detection result with populated {@link DetectionResult#diagnostics()}, or null when
   * the feature list carries no isotope finder run, the engine has no isotope differences, the row
   * has no feature, or nothing was detected.
   */
  public static @Nullable DetectionResult recompute(@NotNull final FeatureListRow row,
      @NotNull final Scan scan) {
    final FeatureList flist = row.getFeatureList();
    if (flist == null) {
      return null;
    }
    final ParameterSet params = findIsotopeFinderParameters(flist);
    if (params == null) {
      return null;
    }
    final RawDataFile file = scan.getDataFile();
    Feature feature = file != null ? row.getFeature(file) : null;
    if (feature == null) {
      feature = row.getBestFeature();
    }
    if (feature == null) {
      return null;
    }
    final double mz = feature.getMZ();
    final Float heightValue = feature.getHeight();
    final double height = heightValue == null ? 0d : heightValue;
    PolarityType polarity = feature.getRepresentativePolarity();
    if (polarity == null) {
      polarity = PolarityType.UNKNOWN;
    }
    final IsotopeFinderEngine engine = IsotopeFinderEngineFactory.createForDiagnostics(params, true);
    if (!engine.hasIsotopeDiffs()) {
      return null;
    }
    return engine.detect(scan, mz, height, polarity);
  }

  /**
   * @return the parameter set of the most recent {@link IsotopeFinderModule} applied method on
   * {@code flist}, or null when the isotope finder was never run on it.
   */
  public static @Nullable ParameterSet findIsotopeFinderParameters(
      @NotNull final FeatureList flist) {
    ParameterSet found = null;
    // keep the last matching applied method so a re-run's parameters win over an earlier one
    for (final FeatureListAppliedMethod m : flist.getAppliedMethods()) {
      if (m.getModule() instanceof IsotopeFinderModule) {
        found = m.getParameters();
      }
    }
    return found;
  }
}
