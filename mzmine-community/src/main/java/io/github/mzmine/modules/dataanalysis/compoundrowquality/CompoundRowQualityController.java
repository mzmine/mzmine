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
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller for the CompoundRow quality pane. Watches the selected compound row + threshold
 * properties and re-runs the {@link CompoundRowQualityInteractor} on a background thread,
 * publishing results to the model.
 */
public class CompoundRowQualityController extends FxController<CompoundRowQualityModel> implements
    SelectedCompoundRowBinding, SelectedFeatureListsBinding {

  // 150 ms debounce: coalesces rapid selection changes (e.g. arrow-key navigation through a table).
  private static final Duration RECOMPUTE_DEBOUNCE = Duration.millis(150);

  private final CompoundRowQualityViewBuilder builder;
  private final CompoundRowQualityInteractor interactor = new CompoundRowQualityInteractor();

  public CompoundRowQualityController() {
    super(new CompoundRowQualityModel());
    // The model self-seeds the persisted check-configuration + color palette in its field
    // initializers, so by the time we wire the recompute subscription both properties already hold
    // non-null values. The view's gear button and in-pane source ComboBox write a fresh reference
    // back on every change, which is what actually fires the recompute.
    builder = new CompoundRowQualityViewBuilder(model);
    PropertyUtils.onChangeDelayedSubscription(this::scheduleRecompute, RECOMPUTE_DEBOUNCE,
        model.selectedCompoundRowProperty(), model.rtStabilityToleranceProperty(),
        model.mzToleranceProperty(), model.ms2ToleranceProperty(), model.colorPaletteProperty(),
        model.checkParametersProperty());
  }

  @Override
  protected @NotNull FxViewBuilder<CompoundRowQualityModel> getViewBuilder() {
    return builder;
  }

  @Override
  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return model.selectedCompoundRowProperty();
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.selectedFeatureListsProperty();
  }

  /**
   * Palette used to derive per-member-row colors. Bind to the host dashboard's palette so this
   * pane's row chips match the dashboard's EIC / mobilogram / MS1 plot colors. Nullable; when
   * unset, checks render member references as plain text.
   */
  public ObjectProperty<@Nullable SimpleColorPalette> colorPaletteProperty() {
    return model.colorPaletteProperty();
  }

  /**
   * The currently selected member row. A click on any member-row chip writes to this property;
   * chips listen and switch between bold and regular styling so the selection is reflected
   * everywhere in the pane.
   * <p>
   * Hosts that own their own row-selection (e.g. {@code CompoundDashboardController}) should
   * {@code bindBidirectional} their selection property to this one so the dashboard and the
   * quality pane stay in sync.
   */
  public ObjectProperty<@Nullable FeatureListRow> selectedMemberRowProperty() {
    return model.selectedMemberRowProperty();
  }

  /**
   * Callback invoked when a check publishes a {@link QualityCheckEvent} (e.g. a fragment-scan
   * group click in the MS2-available check). The host (e.g. CompoundDashboardController)
   * typically subscribes via a {@code switch} on the sealed {@link QualityCheckEvent} permits.
   * Nullable; when unset, events are silently dropped.
   */
  public ObjectProperty<@Nullable Consumer<@NotNull QualityCheckEvent>> onQualityCheckEventProperty() {
    return model.onQualityCheckEventProperty();
  }

  /**
   * Set the row whose quality should be displayed. Clearing or selecting a non-CompoundRow can be
   * done by passing {@code null}.
   */
  public void setSelectedCompoundRow(@Nullable CompoundRow row) {
    onGuiThread(() -> model.selectedCompoundRowProperty().set(row));
  }

  private void scheduleRecompute() {
    final CompoundRow row = model.getSelectedCompoundRow();
    if (row == null) {
      onGuiThread(() -> {
        model.getResults().clear();
        model.computingProperty().set(false);
      });
      return;
    }

    // capture immutable snapshot of inputs
    final RTTolerance rtTol = model.getRtStabilityTolerance();
    final MZTolerance mzTol = model.getMzTolerance();
    final MZTolerance ms2Tol = model.getMs2Tolerance();
    final @NotNull SimpleColorPalette palette = model.getColorPalette();
    // The selection + event callbacks are captured by reference (the property itself, not a
    // snapshot of its value). Chips installed by the background-built result hook into these so
    // clicks on the FX thread can both read and write the live selection.
    final ObjectProperty<@Nullable FeatureListRow> selectedMemberRow = model.selectedMemberRowProperty();
    final Consumer<@NotNull QualityCheckEvent> onEvent = model.getOnQualityCheckEvent();
    final @NotNull ParameterSet checkParameters = model.getCheckParameters();
    // Callback that lets in-card controls (e.g. the annotation-agreement source ComboBox) write
    // back a new ParameterSet — same code path as the view's gear button: persist into
    // MZmineConfiguration, then replace the model's property reference so the recompute
    // subscription fires and re-runs every check.
    final Consumer<@NotNull ParameterSet> onCheckParametersUpdate = this::applyCheckParameters;

    onGuiThread(() -> model.computingProperty().set(true));
    // PropertyUtils.onChangeDelayedSubscription already debounces; FxUpdateTask cancels prior runs by name.
    onTaskThread(
        new RecomputeTask(model, interactor, row, rtTol, mzTol, ms2Tol, palette, selectedMemberRow,
            onEvent, checkParameters, onCheckParametersUpdate));
  }

  /// Persist {@code params} into {@code MZmineConfiguration} under
  /// {@link CompoundRowQualityCheckModule} and push the same reference onto the model so the
  /// recompute subscription fires. Marshals onto the FX thread because callers (combo-box change
  /// listeners inside a check sub pane) may run from arbitrary threads.
  private void applyCheckParameters(@NotNull ParameterSet params) {
    onGuiThread(() -> {
      ConfigService.getConfiguration()
          .setModuleParameters(CompoundRowQualityCheckModule.class, params);
      model.checkParametersProperty().set(params);
    });
  }

}
