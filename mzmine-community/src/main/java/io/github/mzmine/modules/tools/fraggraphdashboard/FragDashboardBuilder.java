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

import static io.github.mzmine.javafx.components.util.FxLayout.*;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.formulatable.FormulaTable;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.NodeTable;
import java.util.logging.Logger;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class FragDashboardBuilder extends FxViewBuilder<FragDashboardModel> {

  private static Logger logger = Logger.getLogger(FragDashboardBuilder.class.getName());

  private final Region fragmentGraph;
  private final Region ms2Chart;
  private final Region isotopeChart;

  protected FragDashboardBuilder(FragDashboardModel model, @NotNull Region fragmentGraph,
      @NotNull Region ms2Chart, @NotNull Region isotopeChart) {
    super(model);
    this.fragmentGraph = fragmentGraph;
    this.ms2Chart = ms2Chart;
    this.isotopeChart = isotopeChart;
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
    final var formulaWrap = new BorderPane(formulaTable, null, null,
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

    final SplitPane graphSplit = new SplitPane(fragmentGraph, nodeSpectraFormulaSplit);
    graphSplit.setOrientation(Orientation.HORIZONTAL);

    mainPane.setCenter(graphSplit);
    return mainPane;
  }
}
