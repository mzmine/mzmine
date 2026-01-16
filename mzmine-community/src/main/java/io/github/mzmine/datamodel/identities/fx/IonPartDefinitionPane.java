/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import static io.github.mzmine.javafx.components.factories.FxTextFields.newNumberField;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_PADDING_INSETS;
import static io.github.mzmine.javafx.components.util.FxLayout.gridRow;
import static io.github.mzmine.util.FormulaUtils.getFormulaString;
import static io.github.mzmine.util.StringUtils.isBlank;
import static io.github.mzmine.util.StringUtils.requireValueOrElse;
import static io.github.mzmine.util.components.FormulaTextField.newFormulaTextField;

import io.github.mzmine.datamodel.identities.iontype.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxLayout.GridColumnGrow;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.util.FormulaStringFlavor;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.maths.Precision;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.controlsfx.validation.ValidationSupport;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Pane to show a {@link IonPartDefinition} that acts as a definition of alternative names for ion
 * parts for parsing. Like ACN is a chemical name synonym for a formula.
 */
public class IonPartDefinitionPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(IonPartDefinitionPane.class.getName());

  private final ValidationSupport validator = FxValidation.newValidationSupport();

  // creating a new IonPart
  private final StringProperty name = new SimpleStringProperty();
  private final ObjectProperty<Integer> charge = new SimpleObjectProperty<>(0);
  private final ObjectProperty<Number> mass = new SimpleObjectProperty<>(0);
  private final StringProperty rawFormulaStr = new SimpleStringProperty();
  private final ReadOnlyObjectProperty<IMolecularFormula> singleFormula;

  // parsed from fields
  private final ReadOnlyObjectWrapper<IonPartDefinition> part = new ReadOnlyObjectWrapper<>();
  @NotNull
  private final Consumer<IonPartDefinition> onAddPart;


  public IonPartDefinitionPane(@NotNull Consumer<IonPartDefinition> onAddPart,
      boolean showAddAndClear) {
    this.onAddPart = onAddPart;
    setPadding(FxLayout.DEFAULT_PADDING_INSETS);

    var mzFormat = new DecimalFormat("0.########");

    var txtName = newTextField(8, name, "Optional name (use formula)", "Optional name or empty to use the formula");
    FxTextFields.autoGrowFitText(txtName, 12, 100);

    var txtFormula = newFormulaTextField(FormulaStringFlavor.DEFAULT_NO_CHARGE, false, rawFormulaStr);
//    FxTextFields.autoGrowFitText(txtFormula, 12, 100);
    singleFormula = txtFormula.formulaProperty();

    var spinCharge = newSpinner(charge,
        "The charge of a single ion part. +2Cl- has charge -1 because Cl has single charge.");
    spinCharge.getEditor().setPrefColumnCount(4);

    var txtMass = disableIf(
        newNumberField(10, mzFormat, mass, "Enter absolute mass of single change"),
        singleFormula.isNotNull());
    FxTextFields.autoGrowFitText(txtMass, 12, 100);

    var lbParsingResult = newBoldLabel(
        part.map(p -> p.toString(IonPartStringFlavor.FULL_WITH_MASS)).orElse("Cannot parse input"));

    var btnAdd = createDisabledButton("Add", FxIcons.ADD,
        "Add new ion part based on name, formula, and charge", part.isNull(),
        () -> addPart(false));
    var btnAddAndClear = createDisabledButton("Add & clear", FxIcons.ADD,
        "Add new ion part based on name, formula, and charge, then clear inputs",
        part.isNull(), () -> addPart(true));
    FxLayout.bindManagedToVisible(btnAddAndClear);
    btnAddAndClear.setVisible(showAddAndClear);

    // layout in grid
    final GridPane ionCreationGrid = FxLayout.newGrid2Col(GridColumnGrow.NONE,
        DEFAULT_PADDING_INSETS,
        // formula is most important, name optional
        newLabel("Formula: "), txtFormula, //
        newLabel("Name: "), txtName, //
        newLabel("Charge: "), spinCharge, //
        newLabel("Δmass: "), txtMass, //
        new Separator(), //
        newLabel("Result:"), lbParsingResult,
        // add
        gridRow(btnAdd, btnAddAndClear) //
    );

    // on any change - update part
    PauseTransition partUpdateDelay = new PauseTransition(Duration.millis(500));
    partUpdateDelay.setOnFinished(_ -> updateCurrentPart());
    PropertyUtils.onChange(partUpdateDelay::playFromStart, name, singleFormula, charge, mass);

    final StringBinding formulaNameError = Bindings.createStringBinding(() -> {
      String name = this.name.getValue();
      IMolecularFormula singleFormula = this.singleFormula.getValue();
      if (isBlank(name) && singleFormula == null) {
        return "Enter a name or a valid formula, one is required, both are possible";
      }
      return null; // no error
    }, singleFormula, name);

    // use the same for both singleFormula and name
    FxValidation.registerErrorValidator(validator, txtName, formulaNameError);

    final StringBinding formulaMassError = Bindings.createStringBinding(() -> {
      Number mass = this.mass.getValue();
      IMolecularFormula singleFormula = this.singleFormula.getValue();
      if (singleFormula != null) {
        return null;
      }

      if (mass == null) {
        return """
            Enter a valid formula or neutral mass.
            Mass input is only available if formula field is empty.
            Otherwise the mass is calculated from the formula.""";
      } else if (Precision.equalFloatSignificance(mass, 0d)) {
        return """
            Enter a valid formula or neutral mass.
            Neutral mass must be >0""";
      }
      return null; // no error
    }, singleFormula, mass);

    // use the same for both formula and name
    FxValidation.registerErrorValidator(validator, txtMass, formulaMassError);

    setCenter(ionCreationGrid);
  }


  public void setNameOnly(String name) {
    clearInputs();
    this.name.setValue(name);
  }


  /**
   * Intermediate parsing step to validate and set part property
   */
  private void updateCurrentPart() {
    try {
      if (validator.isInvalid() == true) {
        part.set(null);
        return;
      }
      IMolecularFormula singleFormula = this.singleFormula.get();
      // if name is blank - fill in singleFormula as name
      final String formulaString =
          singleFormula == null ? null : getFormulaString(singleFormula, false);
      var name = requireValueOrElse(this.name.get(), formulaString);
      Number mass = this.mass.get();
      int charge = this.charge.get();
      if (StringUtils.isBlank(name) || (singleFormula == null && mass == null)) {
        return;
      }

      IonPartDefinition part = null;
      if (singleFormula == null) {
        if (Precision.equalFloatSignificance(mass, 0d)) {
          // do not allow 0 mass as this is reserved for undefined
          return;
        }
        // parse with mass instead
        part = IonPartDefinition.ofNamed(name, mass.doubleValue(), charge);
      } else {
        // has singleFormula so parse singleFormula and calc mass from there
        try {
          part = IonPartDefinition.ofFormula(name, formulaString, charge);
          this.mass.set(part.absSingleMass());
        } catch (Exception e) {
          // cannot parse formula
        }
      }
      this.part.set(part);
    } catch (Exception ex) {
      logger.fine("Cannot parse ion part: " + ex.getMessage());
    }
  }

  /**
   * Adds the part to the final list
   */
  private void addPart(final boolean clearInputs) {
    var part = this.part.get();

    if (clearInputs) {
      clearInputs();
    }
    if (part == null) {
      return;
    }

    onAddPart.accept(part);
  }

  private void clearInputs() {
    name.set("");
    mass.set(null);
    rawFormulaStr.set(null);
    charge.set(0);
    this.part.set(null);
  }

}
