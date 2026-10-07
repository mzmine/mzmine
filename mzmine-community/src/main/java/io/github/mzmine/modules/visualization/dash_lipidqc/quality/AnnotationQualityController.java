/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.dash_lipidqc.quality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickReviewMode;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller for the annotation quality pane. Public API for the dashboard controller to bind
 * data and register callbacks. Has no direct reference to the dashboard model.
 */
public class AnnotationQualityController extends FxController<AnnotationQualityModel> {

  private final AnnotationQualityInteractor interactor;

  public AnnotationQualityController() {
    super(new AnnotationQualityModel());
    interactor = new AnnotationQualityInteractor(model, this);
    // Recompute whenever any input property changes; enables bidirectional binding from outside
    model.featureListProperty().subscribe(_ -> scheduleUpdate());
    model.rowProperty().subscribe(_ -> scheduleUpdate());
    model.retentionTimeAnalysisEnabledProperty().subscribe(_ -> scheduleUpdate());
    model.kendrickReviewModeProperty().subscribe(_ -> scheduleUpdate());
  }

  // ── Exposed properties for bidirectional binding ──────────────────────────

  public ObjectProperty<@NotNull ModularFeatureList> featureListProperty() {
    return model.featureListProperty();
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return model.rowProperty();
  }

  public BooleanProperty retentionTimeAnalysisEnabledProperty() {
    return model.retentionTimeAnalysisEnabledProperty();
  }

  // ── Convenience setters (for standalone use without bindings) ─────────────

  public void setFeatureList(final @NotNull ModularFeatureList flist) {
    onGuiThread(() -> model.setFeatureList(flist));
  }

  public void setRow(final @Nullable FeatureListRow row) {
    onGuiThread(() -> model.setRow(row));
  }

  public void setRetentionTimeAnalysisEnabled(final boolean enabled) {
    onGuiThread(() -> model.setRetentionTimeAnalysisEnabled(enabled));
  }

  public void setKendrickReviewMode(final @NotNull KendrickReviewMode mode) {
    onGuiThread(() -> model.setKendrickReviewMode(mode));
  }

  public void requestUpdate() {
    scheduleUpdate();
  }

  public void setOnAnnotationsChanged(final @Nullable Runnable callback) {
    onGuiThread(() -> model.setOnAnnotationsChanged(callback));
  }

  public void setOnReselectRow(final @Nullable Consumer<@Nullable FeatureListRow> callback) {
    onGuiThread(() -> model.setOnReselectRow(callback));
  }

  public void setOnFeatureTableRefresh(final @Nullable Runnable callback) {
    onGuiThread(() -> model.setOnFeatureTableRefresh(callback));
  }

  /**
   * Called by the interactor after any annotation mutation. Rescores, refreshes the feature table,
   * reselects the preferred row, notifies the dashboard, then immediately recomputes quality
   * cards.
   */
  void refreshAfterAnnotationDelete(final @Nullable FeatureListRow preferredRow) {
    onGuiThread(() -> {
      LipidQcScoringUtils.rescoreOverallQualityScores(model.getFeatureList());
      final @Nullable Runnable refreshCallback = model.getOnFeatureTableRefresh();
      if (refreshCallback != null) {
        refreshCallback.run();
      }
      final @Nullable Consumer<@Nullable FeatureListRow> reselectCallback = model.getOnReselectRow();
      if (reselectCallback != null) {
        reselectCallback.accept(preferredRow);
      }
      final @Nullable Runnable annotationsChangedCallback = model.getOnAnnotationsChanged();
      if (annotationsChangedCallback != null) {
        annotationsChangedCallback.run();
      }
      // immediate recompute - bypass debounce
      onTaskThread(new QualityComputationTask(model));
    });
  }

  private void scheduleUpdate() {
    onTaskThreadDelayed(new QualityComputationTask(model));
  }

  @Override
  protected @NotNull FxViewBuilder<AnnotationQualityModel> getViewBuilder() {
    return new AnnotationQualityViewBuilder(model, interactor);
  }
}
