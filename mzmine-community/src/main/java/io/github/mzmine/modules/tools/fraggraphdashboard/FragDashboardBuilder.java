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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.gui.preferences.NumberFormats;
import static io.github.mzmine.javafx.components.factories.FxButtons.createButton;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.util.FxLayout.newFlowPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxTabs.newTab;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.EdgeTable;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.FormulaTable;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.NodeTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.components.FormulaTextField;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

public class FragDashboardBuilder extends FxViewBuilder<FragDashboardModel> {

  private static final Logger logger = Logger.getLogger(FragDashboardBuilder.class.getName());

  private final NumberFormats format = ConfigService.getGuiFormats();

  private final Region fragmentGraph;
  private final Region ms2Chart;
  private final Region isotopeChart;
  private final ParameterSet parameters;
  private final Runnable updateGraphMethod;
  private final Runnable calculateFormulaeMethod;
  private final ComboComponent<PolarityType> polarityCombo = new ComboComponent<>(
      FXCollections.observableArrayList(PolarityType.POSITIVE, PolarityType.NEGATIVE));
  private final Runnable saveToRowAction;

  protected FragDashboardBuilder(FragDashboardModel model, @NotNull Region fragmentGraph,
      @NotNull Region ms2Chart, @NotNull Region isotopeChart, Runnable updateGraphMethod,
      Runnable calculateFormulaeMethod, Runnable saveToRowAction, ParameterSet parameters) {
    super(model);
    this.fragmentGraph = fragmentGraph;
    this.ms2Chart = ms2Chart;
    this.isotopeChart = isotopeChart;
    this.saveToRowAction = saveToRowAction;
    this.parameters = parameters;
    this.updateGraphMethod = () -> {
      model.setAllowGraphRecalculation(false);
      updateGraphMethod.run();
    };
    this.calculateFormulaeMethod = calculateFormulaeMethod;
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();

    NodeTable nodeTable = new NodeTable();
    allListenersToTable(nodeTable, model.allNodesProperty(), model.selectedNodesProperty());
    EdgeTable edgeTable = new EdgeTable();
    allListenersToTable(edgeTable, model.allEdgesProperty(), model.selectedEdgesProperty());

    // bind formula table to model
    final FormulaTable formulaTable = new FormulaTable();
    formulaTable.itemsProperty().bind(model.precursorFormulaeProperty());
    final var formulaWrap = new BorderPane(formulaTable, newFlowPane(Pos.CENTER_LEFT,
        createButton("Calculate formulae", () -> clearTableAndStartFormulaCalc(formulaTable)),
        createButton("Select formula", () -> selectFormulaFromTable(formulaTable))), null, null,
        null);

    final SplitPane nodeTableGraphSplit = new SplitPane(fragmentGraph,
        new TabPane(newTab("Fragments", nodeTable), newTab("Neutral losses", edgeTable)));
    nodeTableGraphSplit.setDividerPositions(0.6);
    nodeTableGraphSplit.setOrientation(Orientation.HORIZONTAL);

    final TabPane spectraFormulaTab = new TabPane(newTab("Precursor formulae", formulaWrap),
        newTab("Fragmentation spectrum", ms2Chart), newTab("Isotopes", isotopeChart));

    // set up a summary for the precursor formula. the text field can be edited directly and is bound
    // to formula property of the model. Same goes for the exact mass
    final FlowPane formulaSummaryBar = createFormulaSummaryBar();

    final SplitPane tabAndGraphSplit = new SplitPane(nodeTableGraphSplit,
        new BorderPane(spectraFormulaTab, formulaSummaryBar, null, null, null));
    tabAndGraphSplit.setOrientation(Orientation.VERTICAL);
    tabAndGraphSplit.setDividerPositions(0.65);

    mainPane.setCenter(tabAndGraphSplit);

    final Button settingsButton = createSettingsButton();
    StackPane stack = new StackPane(mainPane, settingsButton);
    stack.setAlignment(Pos.TOP_RIGHT);
    return stack;
  }

  @NotNull
  private Button createSettingsButton() {
    return createButton("Settings", null, FxIconUtil.getFontIcon(FxIcons.GEAR_PREFERENCES, 20),
        () -> {
          final ExitCode exitCode = parameters.showSetupDialog(true);
          if (exitCode == ExitCode.OK) {
            polarityCombo.setValue(parameters.getValue(FragmentGraphCalcParameters.polarity));
          }
        });
  }

  private <T> void allListenersToTable(TableView<T> table, ListProperty<T> allElements,
      ListProperty<T> selectedElements) {
    table.itemsProperty().bindBidirectional(allElements);
//    model.selectedNodesProperty().bindContentBidirectional(
//        new SimpleListProperty<>(nodeTable.getSelectionModel().getSelectedItems()));
    selectedElements.addListener((_, _, n) -> {
      if (n.isEmpty() || (table.getSelectionModel().getSelectedItem() != null
                          && table.getSelectionModel().getSelectedItem().equals(n.getFirst()))) {
        return;
      }
      table.getSelectionModel().clearAndSelect(table.getItems().indexOf(n.getFirst()));
    });
    table.getSelectionModel().selectedItemProperty().addListener((_, _, n) -> {
      if (n == null || (!selectedElements.isEmpty() && selectedElements.getFirst().equals(n))) {
        return;
      }
      selectedElements.setAll(List.of(n));
    });
  }

  private void clearTableAndStartFormulaCalc(FormulaTable formulaTable) {
    formulaTable.getItems().clear();
    calculateFormulaeMethod.run();
  }

  private void selectFormulaFromTable(FormulaTable formulaTable) {
    final ResultFormula selected = formulaTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      logger.fine(() -> "Select button clicked, but no formula selected.");
      return;
    }
    model.setPrecursorFormula(selected.getFormulaAsObject());
  }

  @NotNull
  private FlowPane createFormulaSummaryBar() {

    final Button updateGraph = createButton("Update graph", updateGraphMethod);
    updateGraph.disableProperty().bind(model.allowGraphRecalculationProperty().not());

    FormulaTextField selectedFormulaField = new FormulaTextField();
    // add listener first and then bind so we enable the graph if the formula is already set
    selectedFormulaField.formulaProperty().addListener((_, _, f) -> {
      if (f != null) {
        // allow recalc if a new valid formula was set.
        model.allowGraphRecalculationProperty().set(true);
        if (f.getCharge() != null) {
          polarityCombo.setValue(PolarityType.fromInt(f.getCharge()));
        }
      }
    });
    selectedFormulaField.formulaProperty().bindBidirectional(model.precursorFormulaProperty());

    final Label formulaExactMassLabel = newLabel("");
    formulaExactMassLabel.textProperty().bind(Bindings.createStringBinding(
        () -> model.getPrecursorFormula() != null ? format.mz(
            FormulaUtils.calculateMzRatio(model.getPrecursorFormula())) : "formula not set",
        model.precursorFormulaProperty()));
    final Label precursorMzLabel = newBoldLabel("Precursor m/z:");
    TextField mzField = new TextField();
    mzField.textProperty().bindBidirectional(model.precursorMzProperty(), format.mzFormat());

    polarityCombo.setValue(parameters.getValue(FragmentGraphCalcParameters.polarity));
    polarityCombo.valueProperty().addListener((_, _, n) -> {
      if (n != null) {
        parameters.setParameter(FragmentGraphCalcParameters.polarity, n);
      }
    });

    final Button saveButton = createButton("Save formula to row",
        "Saves the selected formula to the selected row. (%s)".formatted(
            model.getRow() != null ? model.getRow().toString() : "none selected"), saveToRowAction);
    saveButton.disableProperty()
        .bind(Bindings.createBooleanBinding(() -> model.getRow() == null, model.rowProperty()));

    return newFlowPane(updateGraph, newHBox(newLabel("Precursor formula:"), selectedFormulaField),
        newHBox(newLabel("Exact mass:"), formulaExactMassLabel), newHBox(precursorMzLabel, mzField),
        newHBox(newLabel("Polarity:"), polarityCombo), saveButton);
  }
}
