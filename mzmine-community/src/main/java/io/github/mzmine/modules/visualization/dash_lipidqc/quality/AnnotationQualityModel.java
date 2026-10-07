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
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickReviewMode;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data model for the annotation quality MVCI component. Holds all state needed to compute and
 * display per-annotation quality cards. Callbacks for dashboard-level side effects are stored here
 * so the interactor can call them without referencing the dashboard model directly.
 */
class AnnotationQualityModel {

  private final ObjectProperty<@NotNull KendrickReviewMode> kendrickReviewMode = new SimpleObjectProperty<>(
      KendrickReviewMode.NONE);
  private final ObjectProperty<@Nullable QualityComputationResult> qualityResult = new SimpleObjectProperty<>(
      null);
  private final ObjectProperty<@Nullable FeatureListRow> row = new SimpleObjectProperty<>(null);
  private final ObjectProperty<@NotNull ModularFeatureList> featureList = new SimpleObjectProperty<>(
      new ModularFeatureList("flist", null, List.of()));
  private final BooleanProperty retentionTimeAnalysisEnabled = new SimpleBooleanProperty(true);
  private final ObjectProperty<@Nullable Runnable> onAnnotationsChanged = new SimpleObjectProperty<>(
      null);
  // Set by dashboard controller; called by interactor after annotation mutations
  private final ObjectProperty<@Nullable Consumer<@Nullable FeatureListRow>> onReselectRow = new SimpleObjectProperty<>(
      null);
  private final ObjectProperty<@Nullable Runnable> onFeatureTableRefresh = new SimpleObjectProperty<>(
      null);

  @NotNull KendrickReviewMode getKendrickReviewMode() {
    return kendrickReviewMode.get();
  }

  void setKendrickReviewMode(final @NotNull KendrickReviewMode mode) {
    kendrickReviewMode.set(mode);
  }

  ObjectProperty<@NotNull KendrickReviewMode> kendrickReviewModeProperty() {
    return kendrickReviewMode;
  }

  @Nullable QualityComputationResult getQualityResult() {
    return qualityResult.get();
  }

  void setQualityResult(final @Nullable QualityComputationResult result) {
    qualityResult.set(result);
  }

  ObjectProperty<@Nullable QualityComputationResult> qualityResultProperty() {
    return qualityResult;
  }

  @Nullable FeatureListRow getRow() {
    return row.get();
  }

  void setRow(final @Nullable FeatureListRow row) {
    this.row.set(row);
  }

  ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return row;
  }

  @NotNull ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  void setFeatureList(final @NotNull ModularFeatureList flist) {
    featureList.set(flist);
  }

  ObjectProperty<@NotNull ModularFeatureList> featureListProperty() {
    return featureList;
  }

  boolean isRetentionTimeAnalysisEnabled() {
    return retentionTimeAnalysisEnabled.get();
  }

  void setRetentionTimeAnalysisEnabled(final boolean enabled) {
    retentionTimeAnalysisEnabled.set(enabled);
  }

  BooleanProperty retentionTimeAnalysisEnabledProperty() {
    return retentionTimeAnalysisEnabled;
  }

  @Nullable Runnable getOnAnnotationsChanged() {
    return onAnnotationsChanged.get();
  }

  void setOnAnnotationsChanged(final @Nullable Runnable callback) {
    onAnnotationsChanged.set(callback);
  }

  @Nullable Consumer<@Nullable FeatureListRow> getOnReselectRow() {
    return onReselectRow.get();
  }

  void setOnReselectRow(final @Nullable Consumer<@Nullable FeatureListRow> callback) {
    onReselectRow.set(callback);
  }

  @Nullable Runnable getOnFeatureTableRefresh() {
    return onFeatureTableRefresh.get();
  }

  void setOnFeatureTableRefresh(final @Nullable Runnable callback) {
    onFeatureTableRefresh.set(callback);
  }
}
