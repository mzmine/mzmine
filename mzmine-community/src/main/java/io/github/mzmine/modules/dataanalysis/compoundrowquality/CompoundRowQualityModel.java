package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
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

  // Invoked when the user clicks a member-row chip in a check's sub pane. Typically wired by the
  // host to e.g. select that row in the dashboard. Nullable -> chips render but are not clickable.
  private final ObjectProperty<@Nullable Consumer<@NotNull FeatureListRow>> onMemberRowClick = new SimpleObjectProperty<>();

  // Invoked when a check publishes a {@link QualityCheckEvent} (e.g. a fragment-scan group click in
  // the MS2-available check). The host (e.g. CompoundDashboardController) typically subscribes to
  // this and reacts via switch on the sealed event permits. Nullable -> events are silently
  // dropped.
  private final ObjectProperty<@Nullable Consumer<@NotNull QualityCheckEvent>> onQualityCheckEvent = new SimpleObjectProperty<>();

  // true while a recompute task is in flight
  private final BooleanProperty computing = new SimpleBooleanProperty(false);

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

  public @Nullable SimpleColorPalette getColorPalette() {
    return colorPalette.get();
  }

  public ObjectProperty<@Nullable SimpleColorPalette> colorPaletteProperty() {
    return colorPalette;
  }

  public @Nullable Consumer<@NotNull FeatureListRow> getOnMemberRowClick() {
    return onMemberRowClick.get();
  }

  public ObjectProperty<@Nullable Consumer<@NotNull FeatureListRow>> onMemberRowClickProperty() {
    return onMemberRowClick;
  }

  public @Nullable Consumer<@NotNull QualityCheckEvent> getOnQualityCheckEvent() {
    return onQualityCheckEvent.get();
  }

  public ObjectProperty<@Nullable Consumer<@NotNull QualityCheckEvent>> onQualityCheckEventProperty() {
    return onQualityCheckEvent;
  }
}
