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
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.util.Comparators;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class NodeTable extends TableView<SignalFormulaeModel> {

  public NodeTable() {
    super();
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    NumberFormats formats = ConfigService.getGuiFormats();
    final TableColumn<SignalFormulaeModel, Number> mzColumn = TableColumns.createColumn("m/z", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, SignalFormulaeModel::calculatedMzProperty);

    final TableColumn<SignalFormulaeModel, SignalFormulaeModel> formulaColumn = new TableColumn<>(
        "Formula");
    formulaColumn.setCellFactory(_ -> new FormulaComboCell());
    formulaColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    formulaColumn.setMinWidth(150);

    final TableColumn<SignalFormulaeModel, Number> deltaMzColumn = TableColumns.createColumn(
        "Δm/z (abs)", 90, formats.mzFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_NUMBER,
        SignalFormulaeModel::deltaMzAbsProperty);

    final TableColumn<SignalFormulaeModel, Number> ppm = TableColumns.createColumn("Δm/z (ppm)", 90,
        formats.ppmFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_NUMBER,
        SignalFormulaeModel::deltaMzPpmProperty);

    getColumns().add(formulaColumn);
    getColumns().add(mzColumn);
    getColumns().add(deltaMzColumn);
    getColumns().add(ppm);
    for (final TableColumn<SignalFormulaeModel, ?> col : getColumns()) {
      col.setReorderable(false);
    }
  }
}
