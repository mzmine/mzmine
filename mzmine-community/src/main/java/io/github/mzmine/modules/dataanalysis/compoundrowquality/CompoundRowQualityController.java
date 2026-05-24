package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
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
    builder = new CompoundRowQualityViewBuilder(model);
    PropertyUtils.onChangeDelayedSubscription(this::scheduleRecompute, RECOMPUTE_DEBOUNCE,
        model.selectedCompoundRowProperty(), model.rtStabilityToleranceProperty(),
        model.mzToleranceProperty(), model.ms2ToleranceProperty(), model.colorPaletteProperty());
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
   * Callback invoked when a member-row chip is clicked in a check's sub pane. Typically wired by
   * the host dashboard to focus that row (e.g. set its {@code selectedAdductRow}). Nullable; when
   * unset, chips render but are not clickable.
   */
  public ObjectProperty<@Nullable Consumer<@NotNull FeatureListRow>> onMemberRowClickProperty() {
    return model.onMemberRowClickProperty();
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
    final SimpleColorPalette palette = model.getColorPalette();
    final Consumer<@NotNull FeatureListRow> onRowClick = model.getOnMemberRowClick();

    onGuiThread(() -> model.computingProperty().set(true));
    // PropertyUtils.onChangeDelayedSubscription already debounces; FxUpdateTask cancels prior runs by name.
    onTaskThread(
        new RecomputeTask(model, interactor, row, rtTol, mzTol, ms2Tol, palette, onRowClick));
  }

}
