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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interactor for the lipid annotation QC dashboard. Handles business logic that must not live in
 * the ViewBuilder:
 * <ul>
 *   <li>Determining whether retention time analysis is applicable for the current feature list and
 *       writing the result to the model.</li>
 *   <li>Reordering lipid annotations on all rows to surface a preferred annotation level.</li>
 * </ul>
 */
class LipidAnnotationQCDashboardInteractor {

  private final LipidAnnotationQCDashboardModel model;

  LipidAnnotationQCDashboardInteractor(final @NotNull LipidAnnotationQCDashboardModel model) {
    this.model = model;
    model.featureListProperty().subscribe(this::onFeatureListChanged);
  }

  // ── model-property reactions ──────────────────────────────────────────────

  private void onFeatureListChanged(final @Nullable ModularFeatureList featureList) {
    model.setRetentionTimeAnalysisEnabled(shouldIncludeRetentionTimeAnalysis(featureList));
  }

  // ── business logic ────────────────────────────────────────────────────────

  /**
   * Determines whether retention time analysis should be included for the given feature list by
   * inspecting the last lipid annotation applied method's analysis type.
   */
  private static boolean shouldIncludeRetentionTimeAnalysis(
      final @Nullable ModularFeatureList featureList) {
    if (featureList == null) {
      return true;
    }
    final List<FeatureListAppliedMethod> appliedMethods = featureList.getAppliedMethods();
    for (int i = appliedMethods.size() - 1; i >= 0; i--) {
      final FeatureListAppliedMethod appliedMethod = appliedMethods.get(i);
      if (!(appliedMethod.getModule() instanceof LipidAnnotationModule)) {
        continue;
      }
      try {
        final LipidAnalysisType analysisType = appliedMethod.getParameters()
            .getParameter(LipidAnnotationParameters.lipidAnalysisType).getValue();
        return analysisType == null || analysisType.hasRetentionTimePattern();
      } catch (RuntimeException ignored) {
        return true;
      }
    }
    return true;
  }

}
