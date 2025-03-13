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

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.util.Comparators;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class EdgeTable extends TableView<SubFormulaEdge> {

  public EdgeTable() {
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    TableColumn<SubFormulaEdge, SubFormulaEdge> visible = TableColumns.createColumn("", 15, 35,
        SimpleObjectProperty::new);
    visible.setCellFactory(_ -> new CheckTableCell<>(SubFormulaEdge::visibleProperty));

    final CheckBox visibleCheckbox = new CheckBox();
    visibleCheckbox.setSelected(true);
    visibleCheckbox.selectedProperty().addListener((_, _, n) -> {
      for (int i = 0; i < this.getItems().size(); i++) {
        visible.getCellData(i).visibleProperty().set(n);
      }
    });
    visible.setGraphic(visibleCheckbox);

    NumberFormats formats = ConfigService.getGuiFormats();
    TableColumn<SubFormulaEdge, Number> signal1 = TableColumns.createColumn("Signal 1", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, edge -> edge.smaller().calculatedMzProperty());

    TableColumn<SubFormulaEdge, Number> signal2 = TableColumns.createColumn("Signal 2", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, edge -> edge.larger().calculatedMzProperty());

    TableColumn<SubFormulaEdge, String> formulaDifference = TableColumns.createColumn(
        "Formula\ndiff.", 85,
        edge -> edge.lossFormulaStringProperty().map(str -> "-[" + str + "]"));

    TableColumn<SubFormulaEdge, Number> massDifferenceAbs = TableColumns.createColumn(
        "Mass diff.\n(meas.)", 85, formats.mzFormat(), ColumnAlignment.RIGHT,
        SubFormulaEdge::measuredMassDiffProperty);

    TableColumn<SubFormulaEdge, Double> massErrorAbs = TableColumns.createColumn("Δm/z\n(abs.)", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_DOUBLE,
        SubFormulaEdge::massErrorAbsProperty);

    TableColumn<SubFormulaEdge, Double> massErrorPpm = TableColumns.createColumn("Δm/z\n(ppm)", 70,
        formats.ppmFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_DOUBLE,
        SubFormulaEdge::massErrorPpmProperty);

    getColumns().addAll(visible, signal1, signal2, formulaDifference, massDifferenceAbs,
        massErrorAbs, massErrorPpm);
  }

}
