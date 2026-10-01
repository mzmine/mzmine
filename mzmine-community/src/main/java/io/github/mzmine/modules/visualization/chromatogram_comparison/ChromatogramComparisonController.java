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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compares the chromatograms of two feature lists of the same raw data file side by side, e.g., the
 * results of the ADAP and the fast chromatogram builder.
 */
public class ChromatogramComparisonController extends FxController<ChromatogramComparisonModel> {

  /**
   * Default tolerance of the chromatogram builders, used for lists without builder parameters
   */
  static final MZTolerance DEFAULT_TOLERANCE = new MZTolerance(0.002, 10);
  public ChromatogramComparisonController() {
    super(new ChromatogramComparisonModel());
    PropertyUtils.onChange(this::updateGroups, model.featureListAProperty(),
        model.featureListBProperty(), model.mzToleranceProperty(),
        model.minSignalIntensityProperty());
  }

  @Override
  protected @NotNull FxViewBuilder<ChromatogramComparisonModel> getViewBuilder() {
    return new ChromatogramComparisonViewBuilder(model);
  }

  /**
   * Sets the lists and resets the m/z tolerance and the signal threshold to the values of their
   * chromatogram builders.
   */
  public void setFeatureLists(@NotNull FeatureList a, @NotNull FeatureList b) {
    onGuiThread(() -> {
      model.setMzTolerance(extractGroupingTolerance(a, b));
      model.setMinSignalIntensity(Math.max(extractMinimumHeight(a), extractMinimumHeight(b)));
      model.setFeatureListA(a);
      model.setFeatureListB(b);
    });
  }

  private void updateGroups() {
    if (model.getFeatureListA() != null && model.getFeatureListB() != null) {
      model.setStatus("Comparing chromatograms...");
    }
    onTaskThreadDelayed(new ChromatogramComparisonUpdateTask(model));
  }

  /**
   * @return the larger tolerance of the chromatogram builders of both lists
   */
  @NotNull
  static MZTolerance extractGroupingTolerance(@NotNull FeatureList a, @NotNull FeatureList b) {
    final MZTolerance toleranceA = extractBuilderTolerance(a);
    final MZTolerance toleranceB = extractBuilderTolerance(b);
    if (toleranceA == null) {
      return toleranceB == null ? DEFAULT_TOLERANCE : toleranceB;
    }
    if (toleranceB == null) {
      return toleranceA;
    }
    return MZTolerance.max(toleranceA, toleranceB.getMzTolerance(), toleranceB.getPpmTolerance());
  }

  /**
   * @return the tolerance of the latest chromatogram builder, the fast builder stores the estimated
   * tolerance for auto
   */
  @Nullable
  private static MZTolerance extractBuilderTolerance(@NotNull FeatureList flist) {
    return ADAPChromatogramBuilderParameters.getAppliedSettings(flist.getAppliedMethods())
        .map(ChromatogramBuilderSettings::mzTolerance).orElse(null);
  }

  /**
   * @return the filter of the latest chromatogram builder of the list, null if not found
   */
  @Nullable
  static SegmentFilter extractSegmentFilter(@NotNull FeatureList flist) {
    return ADAPChromatogramBuilderParameters.getAppliedSettings(flist.getAppliedMethods())
        .map(s -> new SegmentFilter(s.minConsecutiveScans(), s.minGroupIntensity(), s.minHeight()))
        .orElse(null);
  }

  /**
   * @return the minimum height of the latest chromatogram builder, 0 if not found
   */
  private static double extractMinimumHeight(@NotNull FeatureList flist) {
    return ADAPChromatogramBuilderParameters.getAppliedSettings(flist.getAppliedMethods())
        .map(ChromatogramBuilderSettings::minHeight).orElse(0d);
  }
}
