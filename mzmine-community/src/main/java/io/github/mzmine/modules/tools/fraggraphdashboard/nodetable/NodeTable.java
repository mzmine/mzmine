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

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.id_fraggraph.graphstream.SignalFormulaeModel;
import java.text.ParseException;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class NodeTable extends TableView<SignalFormulaeModel> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public NodeTable() {
    super();

    final TableColumn<SignalFormulaeModel, SignalFormulaeModel> mzColumn = new TableColumn<>("m/z");
    mzColumn.setCellFactory(_ -> new MzCell());
    mzColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    mzColumn.setComparator(
        Comparator.comparingDouble(sfm -> sfm.getPeakWithFormulae().peak().getMZ()));
    mzColumn.setMinWidth(70);

    final TableColumn<SignalFormulaeModel, SignalFormulaeModel> formulaColumn = new TableColumn<>(
        "Formula");
    formulaColumn.setCellFactory(_ -> new FormulaComboCell());
    formulaColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    formulaColumn.setMinWidth(150);

    final TableColumn<SignalFormulaeModel, String> deltaMzColumn = new TableColumn<>("Δm/z");
    deltaMzColumn.setCellValueFactory(
        param -> param.getValue().deltaMzProperty().map(delta -> formats.mz(delta.doubleValue())));
    deltaMzColumn.setMinWidth(70);
    deltaMzColumn.setComparator(Comparator.comparingDouble(ppmStr -> {
      try {
        return formats.mzFormat().parse(ppmStr).doubleValue();
      } catch (ParseException e) {
        return 0.0d;
      }
    }));

    getColumns().add(mzColumn);
    getColumns().add(formulaColumn);
    getColumns().add(deltaMzColumn);
  }
}
