package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.List;
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
}
