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

package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Observable state for the CompoundRow quality pane.
 */
public class CompoundRowQualityModel {

  // primary input: the CompoundRow whose quality we display
  private final ObjectProperty<@Nullable CompoundRow> selectedCompoundRow = new SimpleObjectProperty<>();

  // exposed for dashboard wiring (not consumed by the recompute itself)
  private final ObjectProperty<List<FeatureList>> selectedFeatureLists = new SimpleObjectProperty<>();

  // primary output: list of check results, mutated only on FX thread via setAll(...)
  private final ObservableList<@NotNull QualityCheckResult> results = FXCollections.observableArrayList();

  // thresholds — exposed as properties so they can be wired to a settings UI later
  private final ObjectProperty<@NotNull RTTolerance> rtStabilityTolerance = new SimpleObjectProperty<>(
      new RTTolerance(6f, Unit.SECONDS));
  private final ObjectProperty<@NotNull MZTolerance> mzTolerance = new SimpleObjectProperty<>(
      MZTolerance.NARROW_5_PPM_OR_1_MDA);
  private final ObjectProperty<@NotNull MZTolerance> ms2Tolerance = new SimpleObjectProperty<>(
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA);

  // Color palette used to derive per-member-row colors so the quality pane matches the host
  // dashboard (e.g. CompoundDashboardController). Nullable so the pane runs standalone without a
  // host providing a palette.
  private final ObjectProperty<@Nullable SimpleColorPalette> colorPalette = new SimpleObjectProperty<>();

  // The currently "selected" member row across all check sub panes. A click on any member-row
  // chip writes to this property; chips listen and toggle their bold-label style class so the
  // selection stays in sync everywhere. The host (e.g. CompoundDashboardController) typically
  // bind-bidirectionals this to its own selected-adduct-row property so the dashboard plots and
  // the quality pane share one selection.
  private final ObjectProperty<@Nullable FeatureListRow> selectedMemberRow = new SimpleObjectProperty<>();

  // Invoked when a check publishes a {@link QualityCheckEvent} (e.g. a fragment-scan group click in
  // the MS2-available check). The host typically subscribes to this and reacts via switch on the
  // sealed event permits. Nullable -> events are silently dropped.
  private final ObjectProperty<@Nullable Consumer<@NotNull QualityCheckEvent>> onQualityCheckEvent = new SimpleObjectProperty<>();

  // Persisted check configuration (e.g. AnnotationAgreementCheckType). Lives in MZmineConfiguration
  // via CompoundRowQualityCheckModule; the controller seeds it on startup and writes a new
  // reference on every dialog OK to fire the recompute subscription.
  private final ObjectProperty<@Nullable ParameterSet> checkParameters = new SimpleObjectProperty<>();

  // true while a recompute task is in flight
  private final BooleanProperty computing = new SimpleBooleanProperty(false);

  // Per-check-type expanded state. Persisted across view rebuilds so that when the results list is
  // recomputed (e.g. on selection change), each newly created QualityCheckItem can restore the last
  // expanded/collapsed state for its type instead of resetting to collapsed. All entries start as
  // false (collapsed) — entries are added lazily when an item first reads or writes its state.
  private final ObservableMap<@NotNull QualityCheckType, @NotNull Boolean> expandedStateByType = FXCollections.observableHashMap();

  {
    // Pre-populate with collapsed=false for every known check type so callers can rely on the map
    // containing all types from the start. Map is observable, so listeners (if any) still see the
    // change events when toggling later.
    for (final QualityCheckType type : QualityCheckType.values()) {
      expandedStateByType.put(type, false);
    }
  }

  public @Nullable CompoundRow getSelectedCompoundRow() {
    return selectedCompoundRow.get();
  }

  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return selectedCompoundRow;
  }

  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return selectedFeatureLists;
  }

  public @NotNull ObservableList<@NotNull QualityCheckResult> getResults() {
    return results;
  }

  public @NotNull RTTolerance getRtStabilityTolerance() {
    return rtStabilityTolerance.get();
  }

  public ObjectProperty<@NotNull RTTolerance> rtStabilityToleranceProperty() {
    return rtStabilityTolerance;
  }

  public @NotNull MZTolerance getMzTolerance() {
    return mzTolerance.get();
  }

  public ObjectProperty<@NotNull MZTolerance> mzToleranceProperty() {
    return mzTolerance;
  }

  public @NotNull MZTolerance getMs2Tolerance() {
    return ms2Tolerance.get();
  }

  public ObjectProperty<@NotNull MZTolerance> ms2ToleranceProperty() {
    return ms2Tolerance;
  }

  public BooleanProperty computingProperty() {
    return computing;
  }

  public @Nullable ParameterSet getCheckParameters() {
    return checkParameters.get();
  }

  public ObjectProperty<@Nullable ParameterSet> checkParametersProperty() {
    return checkParameters;
  }

  public @Nullable SimpleColorPalette getColorPalette() {
    return colorPalette.get();
  }

  public ObjectProperty<@Nullable SimpleColorPalette> colorPaletteProperty() {
    return colorPalette;
  }

  public @Nullable FeatureListRow getSelectedMemberRow() {
    return selectedMemberRow.get();
  }

  public void setSelectedMemberRow(@Nullable FeatureListRow row) {
    selectedMemberRow.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> selectedMemberRowProperty() {
    return selectedMemberRow;
  }

  public @Nullable Consumer<@NotNull QualityCheckEvent> getOnQualityCheckEvent() {
    return onQualityCheckEvent.get();
  }

  public ObjectProperty<@Nullable Consumer<@NotNull QualityCheckEvent>> onQualityCheckEventProperty() {
    return onQualityCheckEvent;
  }

  /**
   * Observable map of the per-{@link QualityCheckType} expanded state used by the view to keep the
   * last expand/collapse state across rebuilds. All types start as {@code false} (collapsed); a
   * card writes back into this map on toggle so the next rebuild restores its state.
   */
  public @NotNull ObservableMap<@NotNull QualityCheckType, @NotNull Boolean> expandedStateByTypeProperty() {
    return expandedStateByType;
  }
}
