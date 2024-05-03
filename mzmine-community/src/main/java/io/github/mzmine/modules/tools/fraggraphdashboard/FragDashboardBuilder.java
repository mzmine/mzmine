/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxLabels.*;
import static io.github.mzmine.javafx.components.util.FxLayout.*;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.formulatable.FormulaTable;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.NodeTable;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.components.FormulaTextField;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class FragDashboardBuilder extends FxViewBuilder<FragDashboardModel> {

  private static Logger logger = Logger.getLogger(FragDashboardBuilder.class.getName());

  private final NumberFormats format = ConfigService.getGuiFormats();

  private final Region fragmentGraph;
  private final Region ms2Chart;
  private final Region isotopeChart;
  private final Runnable updateGraphMethod;
  private final Runnable calculateFormulaeMethod;

  protected FragDashboardBuilder(FragDashboardModel model, @NotNull Region fragmentGraph,
      @NotNull Region ms2Chart, @NotNull Region isotopeChart, Runnable updateGraphMethod,
      Runnable calculateFormulaeMethod) {
    super(model);
    this.fragmentGraph = fragmentGraph;
    this.ms2Chart = ms2Chart;
    this.isotopeChart = isotopeChart;
    this.updateGraphMethod = updateGraphMethod;
    this.calculateFormulaeMethod = calculateFormulaeMethod;
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();

    NodeTable nodeTable = new NodeTable();
    nodeTable.itemsProperty().bindBidirectional(model.allNodesProperty());
    model.selectedNodesProperty().bindBidirectional(
        new SimpleListProperty<>(nodeTable.getSelectionModel().getSelectedItems()));

    // bind formula table to model
    final FormulaTable formulaTable = new FormulaTable();
    formulaTable.itemsProperty().bind(model.precursorFormulaeProperty());
    final var formulaWrap = new BorderPane(formulaTable,
        FxButtons.createButton("Calculate formulae", calculateFormulaeMethod), null,
        newFlowPane(Pos.CENTER_LEFT, FxButtons.createButton("Select formula", () -> {
          final ResultFormula selected = formulaTable.getSelectionModel().getSelectedItem();
          if (selected == null) {
            logger.fine(() -> "Select button clicked, but no formula selected.");
            return;
          }
          model.setPrecursorFormula(selected.getFormulaAsObject());
        })), null);

    final SplitPane nodeSpectraFormulaSplit = new SplitPane(nodeTable,
        new TabPane(new Tab("MS2 spectrum", ms2Chart), new Tab("Isotope spectrum", isotopeChart),
            new Tab("Precursor formulae", formulaWrap)));
    nodeSpectraFormulaSplit.setOrientation(Orientation.VERTICAL);

    // set up a summary for the precursor formula. the text field can be edited directly and is bound
    // to formula property of the model. Same goes for the exact mass
    final FlowPane formulaSummaryBar = createFormulaSummaryBar();

    final SplitPane graphSplit = new SplitPane(fragmentGraph,
        new BorderPane(nodeSpectraFormulaSplit, formulaSummaryBar, null, null, null));
    graphSplit.setOrientation(Orientation.HORIZONTAL);

    mainPane.setCenter(graphSplit);
    return mainPane;
  }

  @NotNull
  private FlowPane createFormulaSummaryBar() {

    final Button updateGraph = FxButtons.createButton("Update graph", updateGraphMethod);
    updateGraph.disableProperty().bind(model.allowGraphRecalculationProperty().not());

    FormulaTextField selectedFormulaField = new FormulaTextField();
    selectedFormulaField.formulaProperty().bindBidirectional(model.precursorFormulaProperty());
    selectedFormulaField.formulaProperty().addListener((_, _, f) -> {
      if (f != null) {
        // allow recalc if a new valid formula was set.
        model.allowGraphRecalculationProperty().set(true);
      }
    });

    final Label formulaExactMassLabel = newLabel("");
    formulaExactMassLabel.textProperty().bind(Bindings.createStringBinding(
        () -> model.getPrecursorFormula() != null ? format.mz(
            FormulaUtils.calculateMzRatio(model.getPrecursorFormula())) : "",
        model.precursorFormulaProperty()));
    final Label precursorMzLabel = newBoldLabel("Precursor m/z:");
    TextField mzField = new TextField();
    mzField.textProperty().bindBidirectional(model.precursorMzProperty(), format.mzFormat());

    return newFlowPane(updateGraph,
        newHBox(newLabel("Selected precursor formula:"), selectedFormulaField),
        newHBox(newLabel("Exact mass:"), formulaExactMassLabel),
        newHBox(precursorMzLabel, mzField));
  }
}
