/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.other_detectors;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherTraceSelectionComponent extends VBox implements
    ValuePropertyComponent<OtherTraceSelection> {

  private final ObjectProperty<OtherTraceSelection> value = new SimpleObjectProperty<>(
      OtherTraceSelection.rawUv());

  private final ObjectProperty<ChromatogramTypeChoices> chromType = new SimpleObjectProperty<>(
      ChromatogramTypeChoices.ALL);
  private final StringProperty rangeUnitFilter = new SimpleStringProperty();
  private final StringProperty rangeLabelFilter = new SimpleStringProperty();
  private final StringProperty descriptionFilter = new SimpleStringProperty();
  private final ObjectProperty<OtherRawOrProcessed> rawOrProcessed = new SimpleObjectProperty<>(
      OtherRawOrProcessed.RAW);

  public OtherTraceSelectionComponent() {
    this(List.of(OtherRawOrProcessed.values()));
  }

  public OtherTraceSelectionComponent(Collection<OtherRawOrProcessed> otherRawOrProcessedChoices) {
    // use default values that are actually available
    if (!otherRawOrProcessedChoices.contains(OtherRawOrProcessed.RAW)) {
      rawOrProcessed.set(otherRawOrProcessedChoices.iterator().next());
    }

    setFillWidth(true);
    GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    getChildren().add(grid);

    final ComboBox<ChromatogramTypeChoices> typeCombo = FxComboBox.createComboBox(
        "Select the type of chromatograms you want to process.",
        List.of(ChromatogramTypeChoices.values()), chromType);
    grid.add(FxLabels.newLabel("Chromatogram type:"), 0, 0);
    grid.add(typeCombo, 1, 0);

    final TextField rangeUnitFilterField = FxTextFields.newTextField(20, rangeUnitFilter,
        "range unit filter",
        "Set an optional range unit filter using a wildcard '*' to match anything.");
    grid.add(FxLabels.newLabel("Range unit filter: "), 0, 1);
    grid.add(rangeUnitFilterField, 1, 1);

    final TextField rangeLabelFilterField = FxTextFields.newTextField(20, rangeLabelFilter,
        "range label filter",
        "Set an optional range unit filter using a wildcard '*' to match anything.");
    grid.add(FxLabels.newLabel("Range label filter: "), 0, 2);
    grid.add(rangeLabelFilterField, 1, 2);

    final TextField descriptionFilterField = FxTextFields.newTextField(20, descriptionFilter,
        "description filter",
        "Set an optional description filter using a wildcard '*' to match anything.");
    grid.add(FxLabels.newLabel("Description filter: "), 0, 3);
    grid.add(descriptionFilterField, 1, 3);

    final ComboBox<OtherRawOrProcessed> rawOrProcessedCombo = FxComboBox.createComboBox(
        "Select the type of chromatograms you want to process.", otherRawOrProcessedChoices,
        this.rawOrProcessed);
    grid.add(FxLabels.newLabel("Trace type:"), 0, 4);
    grid.add(rawOrProcessedCombo, 1, 4);

    grid.getColumnConstraints().add(new ColumnConstraints(120));
    grid.getColumnConstraints().add(
        new ColumnConstraints(80, USE_PREF_SIZE, USE_COMPUTED_SIZE, Priority.ALWAYS, HPos.LEFT,
            true));

    value.bind(Bindings.createObjectBinding(this::createValue, chromType, rangeLabelFilter,
        rangeUnitFilter, descriptionFilter, rawOrProcessed));
  }

  private OtherTraceSelection createValue() {
    final ChromatogramType chrom = chromType.get().toChromatogramType();
    final String uf = StringUtils.isBlank(rangeUnitFilter.get()) ? null : rangeUnitFilter.get();
    final String lf = StringUtils.isBlank(rangeLabelFilter.get()) ? null : rangeLabelFilter.get();
    final String desc = StringUtils.isBlank(rangeLabelFilter.get()) ? null : rangeLabelFilter.get();
    final OtherRawOrProcessed raw = rawOrProcessed.get();

    return new OtherTraceSelection(chrom, uf, lf, desc, raw);
  }

  @Override
  public Property<OtherTraceSelection> valueProperty() {
    return value;
  }

  public OtherTraceSelection getValue() {
    return value.get();
  }

  public void setValue(final OtherTraceSelection value) {
    chromType.set(ChromatogramTypeChoices.fromChromatogramType(value.chromatogramType()));
    // replace the wildcard filter with the gui representation
    rangeUnitFilter.set(
        value.rangeUnitFilter() != null ? value.rangeUnitFilter().replaceAll("\\*\\.", "*") : "");
    rangeLabelFilter.set(
        value.rangeLabelFilter() != null ? value.rangeLabelFilter().replaceAll("\\*\\.", "*") : "");
    descriptionFilter.set(
        value.descriptionFilter() != null ? value.descriptionFilter().replaceAll("\\*\\.", "*")
            : "");
    rawOrProcessed.set(value.rawOrProcessed());
  }

  /**
   * Reasonable choices for the gui and ALL option.
   */
  private enum ChromatogramTypeChoices {
    ELECTROMAGNETIC_RADIATION, ABSORPTION, EMISSION, ION_CURRENT, UNKNOWN, MRM_SRM, ALL;

    static ChromatogramTypeChoices fromChromatogramType(final @Nullable ChromatogramType type) {
      return switch (type) {
        case TIC, SIM, SIC, BPC, PRESSURE, FLOW_RATE, UNKNOWN -> UNKNOWN;
        case ELECTROMAGNETIC_RADIATION -> ELECTROMAGNETIC_RADIATION;
        case ABSORPTION -> ABSORPTION;
        case EMISSION -> EMISSION;
        case ION_CURRENT -> ION_CURRENT;
        case MRM_SRM -> MRM_SRM;
        case null -> ALL;
      };
    }

    @Nullable ChromatogramType toChromatogramType() {
      return switch (this) {
        case ELECTROMAGNETIC_RADIATION -> ChromatogramType.ELECTROMAGNETIC_RADIATION;
        case ABSORPTION -> ChromatogramType.ABSORPTION;
        case EMISSION -> ChromatogramType.EMISSION;
        case ION_CURRENT -> ChromatogramType.ION_CURRENT;
        case MRM_SRM -> ChromatogramType.MRM_SRM;
        case UNKNOWN -> ChromatogramType.UNKNOWN;
        case ALL -> null;
      };
    }
  }

}
