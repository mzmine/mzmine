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
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import java.text.ParseException;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class NodeTable extends TableView<SignalFormulaeModel> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public NodeTable() {
    super();
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    final TableColumn<SignalFormulaeModel, String> mzColumn = new TableColumn<>("m/z");
    mzColumn.setCellValueFactory(cellData -> cellData.getValue().calculatedMzProperty().map(formats::mz));
    mzColumn.setComparator(Comparator.comparingDouble(this::mzDoubleParser));
    mzColumn.getStyleClass().add("align-right-column");
    mzColumn.setMinWidth(70);
    mzColumn.setReorderable(false);

    final TableColumn<SignalFormulaeModel, SignalFormulaeModel> formulaColumn = new TableColumn<>(
        "Formula");
    formulaColumn.setCellFactory(_ -> new FormulaComboCell());
    formulaColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    formulaColumn.setMinWidth(150);
    formulaColumn.setReorderable(false);

    final TableColumn<SignalFormulaeModel, String> deltaMzColumn = new TableColumn<>("Δm/z (abs)");
    deltaMzColumn.setCellValueFactory(
        param -> param.getValue().deltaMzAbsProperty().map(formats::mz));
    deltaMzColumn.setMinWidth(90);
    deltaMzColumn.setReorderable(false);
    deltaMzColumn.getStyleClass().add("align-right-column");
    deltaMzColumn.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    final TableColumn<SignalFormulaeModel, String> ppm = new TableColumn<>("Δm/z (ppm)");
    ppm.setCellValueFactory(param -> param.getValue().deltaMzPpmProperty().map(formats::ppm));
    ppm.setMinWidth(90);
    ppm.setReorderable(false);
    ppm.getStyleClass().add("align-right-column");
    ppm.setComparator(Comparator.comparingDouble(ppmStr -> {
      try {
        return formats.ppmFormat().parse(ppmStr).doubleValue();
      } catch (ParseException e) {
        return 0.0d;
      }
    }));

    getColumns().add(formulaColumn);
    getColumns().add(mzColumn);
    getColumns().add(deltaMzColumn);
    getColumns().add(ppm);
  }

  private double mzDoubleParser(String diffStr) {
    try {
      return formats.mzFormat().parse(diffStr).doubleValue();
    } catch (ParseException e) {
      return 0.0d;
    }
  }
}
