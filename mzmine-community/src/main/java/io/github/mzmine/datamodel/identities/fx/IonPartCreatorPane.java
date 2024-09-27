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

package io.github.mzmine.datamodel.identities.fx;

import static io.github.mzmine.javafx.components.factories.FxButtons.createDisabledButton;
import static io.github.mzmine.javafx.components.factories.FxButtons.disableIf;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxSpinners.newSpinner;
import static io.github.mzmine.javafx.components.factories.FxTextFields.applyToField;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newNumberField;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import static io.github.mzmine.util.components.FormulaTextField.newFormulaTextField;

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPart.IonStringFlavor;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.StringUtils;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.controlsfx.validation.ValidationSupport;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class IonPartCreatorPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(IonPartCreatorPane.class.getName());

  private final ValidationSupport validation = new ValidationSupport();

  private final ListView<IonPart> partListView;
  private final StringProperty parsedFormula = new SimpleStringProperty();

  // creating a new IonPart
  private final StringProperty name = new SimpleStringProperty();
  private final ObjectProperty<Integer> charge = new SimpleObjectProperty<>(0);
  private final ObjectProperty<Integer> count = new SimpleObjectProperty<>(1);
  private final ObjectProperty<Number> mass = new SimpleObjectProperty<>(0);
  private final ObjectProperty<@Nullable IMolecularFormula> formula = new SimpleObjectProperty<>();

  // parsed from fields
  private final ReadOnlyObjectWrapper<IonPart> part = new ReadOnlyObjectWrapper<>();

  public IonPartCreatorPane(ObservableList<IonPart> parts) {
    partListView = new ListView<>(parts);
    setLeft(partListView);
    var mzFormat = ConfigService.getExportFormats().mzFormat();

    setCenter(FxLayout.newGrid2Col(
        // parse formula and set other fields with button. Button disabled if formula invalid
        newTextField(8, parsedFormula,
            "Use formula to parse ion parts name and charge, e.g., Fe+2 for doubly charged iron"),
        createDisabledButton("Parse formula", "Parse formula and set other fields based on it",
            parsedFormula.isEmpty(), () -> parseFromFormula(parsedFormula.get())),
        // other fields
        newLabel("Name: "), newTextField(8, name, "Name, often the formula"), //
        newLabel("Formula: "),
        applyToField(newFormulaTextField(false, formula), 8, "Formula or empty if unknown"), //
        newLabel("Charge: "), newSpinner(charge), //
        newLabel("Count: "), newSpinner(count), //
        newLabel("Δmass: "), //
        disableIf(newNumberField(10, mzFormat, mass, "Enter absolute mass of single change"),
            formula.isNotNull()), //
        newBoldLabel("Result"),
        newBoldLabel(part.map(p -> p.toString(IonStringFlavor.FULL)).orElse("Cannot parse input")),
        // add
        createDisabledButton("Add", "Add new ion part based on name, formula, and charge",
            part.isNull(), () -> addPart(false)), createDisabledButton("Add & clear",
            "Add new ion part based on name, formula, and charge, then clear inputs", part.isNull(),
            () -> addPart(true))));

    // on any change - update part
    PauseTransition partUpdateDelay = new PauseTransition(Duration.millis(500));
    partUpdateDelay.setOnFinished(_ -> updateCurrentPart());
    PropertyUtils.onChange(partUpdateDelay::playFromStart, name, formula, charge, mass);

  }

  private void updateCurrentPart() {
    try {
      IMolecularFormula formula = this.formula.get();
      Number mass = this.mass.get();
      var name = this.name.get();
      var charge = this.charge.get();
      if (StringUtils.isBlank(name) || (formula == null && mass == null)) {
        return;
      }
      final IonPart part;
      if (formula == null) {
        // parse with mass instead
        part = new IonPart(name, null, mass.doubleValue(), charge, 1);
      } else {
        // has formula so parse formula and calc mass from there
        part = new IonPart(name, formula, charge, 1);
      }
      this.part.set(part);
    } catch (Exception ex) {
      logger.fine("Cannot parse ion part: " + ex.getMessage());
    }
  }

  private void addPart(final boolean clearInputs) {
    if (clearInputs) {
      name.set("");
      mass.set(0);
      formula.set(null);
    }
    var part = this.part.get();
    if (part == null) {
      return;
    }
    var items = partListView.getItems();
    if (!items.contains(part)) {
      items.add(part);
    }
  }

  private void parseFromFormula(final String formula) {
    if (StringUtils.isBlank(formula)) {
      return;
    }
    try {
      var part = IonPart.parse(formula);
      if (part == null) {

      }
      name.set(part.name());
    } catch (Exception ex) {

    }
  }

  public ReadOnlyObjectProperty<IonPart> selectedPartProperty() {
    return partListView.getSelectionModel().selectedItemProperty();
  }

}
