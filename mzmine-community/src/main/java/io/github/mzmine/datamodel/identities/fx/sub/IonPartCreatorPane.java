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

package io.github.mzmine.datamodel.identities.fx.sub;

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPart.IonStringFlavor;
import io.github.mzmine.datamodel.identities.IonPart.Type;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.FilterableListView.MenuControls;
import static io.github.mzmine.javafx.components.factories.FxButtons.createDisabledButton;
import static io.github.mzmine.javafx.components.factories.FxButtons.disableIf;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import io.github.mzmine.javafx.components.factories.FxListViews;
import static io.github.mzmine.javafx.components.factories.FxSpinners.newNonZeroSpinner;
import static io.github.mzmine.javafx.components.factories.FxSpinners.newSpinner;
import static io.github.mzmine.javafx.components.factories.FxTextFields.applyToField;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newNumberField;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import static io.github.mzmine.javafx.components.util.FxLayout.gridRow;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import static io.github.mzmine.util.FormulaUtils.getFormulaString;
import io.github.mzmine.util.StringUtils;
import static io.github.mzmine.util.StringUtils.isBlank;
import static io.github.mzmine.util.StringUtils.requireValueOrElse;
import static io.github.mzmine.util.components.FormulaTextField.newFormulaTextField;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import net.synedra.validatorfx.Validator;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class IonPartCreatorPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(IonPartCreatorPane.class.getName());

  private final FilterableListView<IonPart> partListView;
  private final StringProperty parsedIonPart = new SimpleStringProperty();

  // creating a new IonPart
  private final StringProperty name = new SimpleStringProperty();
  private final ObjectProperty<Integer> charge = new SimpleObjectProperty<>(0);
  private final ObjectProperty<Integer> count = new SimpleObjectProperty<>(1);
  private final ObjectProperty<Number> mass = new SimpleObjectProperty<>(0);
  private final ObjectProperty<@Nullable IMolecularFormula> formula = new SimpleObjectProperty<>();

  // parsed from fields
  private final ReadOnlyObjectWrapper<IonPart> part = new ReadOnlyObjectWrapper<>();
  private final Validator currentPartValidator = new Validator();

  // additional properties for the list view
  private final ObjectProperty<IonSorting> listSorting = new SimpleObjectProperty<>(
      IonSorting.CHARGE_THEN_MASS);


  public IonPartCreatorPane(ObservableList<IonPart> parts) {
    setTop(newBoldTitle("List of ion building blocks"));

    partListView = createListView(parts);

    partListView.setRight(createIonPartCreationPane());
    setCenter(partListView);
  }

  private @NotNull FilterableListView<IonPart> createListView(final ObservableList<IonPart> parts) {
    // create a list view with addtional controls for sorting and filtering
    // sorting:
    final HBox sortingCombo = FxComboBox.createLabeledComboBox("Sort by:",
        FXCollections.observableList(List.of(IonSorting.values())), listSorting);

    final CheckComboBox<Type> filterTypeCombo = FxComboBox.createCheckComboBox(
        "Show only elements that match the selected types.", List.of(Type.values()));

    final List<Node> additionalNodes = List.of(sortingCombo, filterTypeCombo);
    final List<MenuControls> stdButtons = List.of(MenuControls.CLEAR_BTN, MenuControls.REMOVE_BTN);

    // create the list view
    final FilterableListView<IonPart> partListView = FxListViews.newFilterableListView(parts, true,
        SelectionMode.MULTIPLE, Position.TOP, Pos.CENTER_LEFT, stdButtons, additionalNodes);

    // set comparator and filter
    listSorting.subscribe(
        nv -> partListView.sortingComparatorProperty().set(nv.createIonPartComparator()));

    // apply filter now:
    filterTypeCombo.getCheckModel().getCheckedItems().addListener(
        (ListChangeListener<? super Type>) _ -> applyFilter(filterTypeCombo, partListView));
    applyFilter(filterTypeCombo, partListView);

    return partListView;
  }

  /**
   * Grid that allows creation of new ion parts
   */
  private Node createIonPartCreationPane() {
    var mzFormat = ConfigService.getExportFormats().mzFormat();

    var txtParsedIonPart = newTextField(8, this.parsedIonPart, "Format: +Cu+2",
        "Use formula to parse ion parts name and charge, e.g., Fe+2 for doubly charged iron");
    var btnParseIonPart = createDisabledButton("Parse",
        "Parse ion part from formula and set other fields based on it, e.g., +Fe+3 or -2Cl-",
        this.parsedIonPart.isEmpty(), () -> parsePartStringFillFields(this.parsedIonPart.get()));
    var txtName = newTextField(8, name, "Name, often the formula");
    var txtFormula = applyToField(newFormulaTextField(false, false, formula), 8,
        "Formula or empty if unknown");
    var spinCount = newNonZeroSpinner(count, "Multiplier to add (>0) or remove (<0) parts.");
    var spinCharge = newSpinner(charge,
        "The charge of a single ion part. +2Cl- has charge -1 because Cl has single charge.");
    var txtMass = disableIf(
        newNumberField(10, mzFormat, mass, "Enter absolute mass of single change"),
        formula.isNotNull());

    var lbParsingResult = newBoldLabel(
        part.map(p -> p.toString(IonStringFlavor.FULL)).orElse("Cannot parse input"));

    var btnAdd = createDisabledButton("Add", "Add new ion part based on name, formula, and charge",
        part.isNull(), () -> addPart(false));
    var btnAddAndClear = createDisabledButton("Add & clear",
        "Add new ion part based on name, formula, and charge, then clear inputs", part.isNull(),
        () -> addPart(true));

    // layout in grid
    final GridPane ionCreationGrid = FxLayout.newGrid2Col(
        // parse formula and set other fields with button. Button disabled if formula invalid
        newBoldLabel("Ion part: "), new HBox(txtParsedIonPart, btnParseIonPart),
        // spacer
        new Separator(),
        // other fields
        newLabel("Name: "), txtName, //
        newLabel("Formula: "), txtFormula, //
        newLabel("Charge: "), spinCharge, //
        newLabel("Count: "), spinCount, //
        newLabel("Δmass: "), txtMass, //
        new Separator(), //
        newLabel("Result:"), lbParsingResult,
        // add
        gridRow(btnAdd, btnAddAndClear) //
    );

    // on any change - update part
    PauseTransition partUpdateDelay = new PauseTransition(Duration.millis(500));
    partUpdateDelay.setOnFinished(_ -> updateCurrentPart());
    PropertyUtils.onChange(partUpdateDelay::playFromStart, name, formula, charge, count, mass);

    // add validations
    Validator partParserValidator = new Validator();
    partParserValidator.createCheck().dependsOn("parsedIonPart", parsedIonPart) //
        .withMethod(c -> {
          String value = c.get("parsedIonPart");
          if (!StringUtils.isBlank(value) && IonPart.parse(value) == null) {
            c.error("Cannot parse ion part. Input correct format, e.g., +Fe+3 or -2Cl-");
          }
        }).decorates(txtParsedIonPart).immediate();

    // validate creation of ion part from all fields
    currentPartValidator.createCheck().dependsOn("count", count) //
        .withMethod(c -> {
          Integer value = c.get("count");
          if (value == 0) {
            c.error("Count needs to be different from 0");
          }
        }).decorates(spinCount).immediate();

    currentPartValidator.createCheck() //
        .dependsOn("formula", formula) //
        .dependsOn("name", name) //
        .withMethod(c -> {
          String name = c.get("name");
          IMolecularFormula formula = c.get("formula");
          if (isBlank(name) && formula == null) {
            c.error("Enter a name or a valid formula");
          }
        }).decorates(txtFormula).decorates(txtName).immediate();

    currentPartValidator.createCheck() //
        .dependsOn("formula", formula) //
        .dependsOn("mass", mass) //
        .withMethod(c -> {
          Number mass = c.get("mass");
          IMolecularFormula formula = c.get("formula");
          if (mass == null && formula == null) {
            c.error("""
                Enter a valid formula or mass.
                Mass input is only available if formula field is empty.""");
          }
        }).decorates(txtFormula).decorates(txtMass).immediate();
    return ionCreationGrid;
  }

  private void applyFilter(final CheckComboBox<Type> filterTypeCombo,
      final FilterableListView<IonPart> partListView) {
    var included = Set.copyOf(filterTypeCombo.getCheckModel().getCheckedItems());
    partListView.filterPredicateProperty().set(part -> included.contains(part.type()));
  }


  /**
   * Intermediate parsing step to validate and set part property
   */
  private void updateCurrentPart() {
    try {
      if (!currentPartValidator.validate()) {
        part.set(null);
        return;
      }
      IMolecularFormula formula = this.formula.get();
      // if name is blank - fill in formula as name
      var name = requireValueOrElse(this.name.get(), getFormulaString(formula, false));
      Number mass = this.mass.get();
      int charge = this.charge.get();
      int count = this.count.get();
      if (count == 0 || StringUtils.isBlank(name) || (formula == null && mass == null)) {
        return;
      }
      final IonPart part;
      if (formula == null) {
        // parse with mass instead
        part = new IonPart(name, null, mass.doubleValue(), charge, count);
      } else {
        // has formula so parse formula and calc mass from there
        part = new IonPart(name, formula, charge, count);
        this.mass.set(part.absSingleMass());
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
    var items = partListView.getOriginalItems();
    if (!items.contains(part)) {
      items.add(part);
    }
  }

  private void clearInputs() {
    name.set("");
    mass.set(0);
    formula.set(null);
    count.set(1);
    charge.set(0);
    this.part.set(null);
  }

  /**
   * Parse ion part from string and fill all fields
   *
   * @param ionPart ion part string as +Fe+2 or -2Cl-
   * @return true only if part was parsed successfully. Blank or null input results in false but is
   * considered a non-issue
   */
  private boolean parsePartStringFillFields(final String ionPart) {
    if (StringUtils.isBlank(ionPart)) {
      return false;
    }
    try {
      var part = IonPart.parse(ionPart);
      if (part == null) {
        return false;
      }
      name.set(part.name());
      charge.set(part.singleCharge());
      count.set(part.count());
      mass.set(part.absSingleMass());
      formula.set(part.singleFormula());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

}
