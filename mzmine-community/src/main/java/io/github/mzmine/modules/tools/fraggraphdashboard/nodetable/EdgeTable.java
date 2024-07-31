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
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import java.text.ParseException;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public class EdgeTable extends TableView<SubFormulaEdge> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public EdgeTable() {
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    setRowFactory(table -> new TableRow<>() {
      {
        // unbind from old property, bind to new one to properly reflect the disable status
        itemProperty().addListener((_, old, n) -> {
          if (old != null) {
            disableProperty().unbind();
          }
          if (n != null) {
            disableProperty().bind(n.validProperty().not());
          }
        });
      }
    });

    TableColumn<SubFormulaEdge, SubFormulaEdge> visible = new TableColumn<>("");
    visible.setMinWidth(15);
    visible.setMaxWidth(35);
    visible.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
    visible.setCellFactory(col -> new CheckTreeCell<>(SubFormulaEdge::visibleProperty));

    TableColumn<SubFormulaEdge, String> signal1 = new TableColumn<>("Signal 1");
    signal1.getStyleClass().add("align-right-column");
    signal1.setCellValueFactory(
        cell -> cell.getValue().smaller().calculatedMzProperty().map(formats::mz));
    signal1.setComparator(Comparator.comparingDouble(this::mzDoubleParser));
    signal1.setMinWidth(70);

    TableColumn<SubFormulaEdge, String> signal2 = new TableColumn<>("Signal 2");
    signal2.getStyleClass().add("align-right-column");
    signal2.setCellValueFactory(
        cell -> cell.getValue().larger().calculatedMzProperty().map(formats::mz));
    signal2.setComparator(Comparator.comparingDouble(this::mzDoubleParser));
    signal2.setMinWidth(70);

    TableColumn<SubFormulaEdge, String> formulaDifference = new TableColumn<>("Formula\ndiff.");
    formulaDifference.setCellValueFactory(
        cell -> cell.getValue().lossFormulaStringProperty().map(str -> STR."-[\{str}]"));
    formulaDifference.setMinWidth(85);

    TableColumn<SubFormulaEdge, String> massDifferenceAbs = new TableColumn<>(
        "Mass diff.\n(meas.)");
    massDifferenceAbs.getStyleClass().add("align-right-column");
    massDifferenceAbs.setCellValueFactory(
        cell -> cell.getValue().measuredMassDiffProperty().map(formats::mz));
    massDifferenceAbs.setMinWidth(85);
    massDifferenceAbs.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    TableColumn<SubFormulaEdge, String> massErrorAbs = new TableColumn<>("Δm/z\n(abs.)");
    massErrorAbs.getStyleClass().add("align-right-column");
    massErrorAbs.setCellValueFactory(
        cell -> cell.getValue().massErrorAbsProperty().map(formats::mz));
    massErrorAbs.setMinWidth(70);
    massErrorAbs.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    TableColumn<SubFormulaEdge, String> massErrorPpm = new TableColumn<>("Δm/z\n(ppm)");
    massErrorPpm.getStyleClass().add("align-right-column");
    massErrorPpm.setCellValueFactory(
        cell -> cell.getValue().massErrorPpmProperty().map(formats::ppm));
    massErrorPpm.setMinWidth(70);
    massErrorPpm.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    getColumns().addAll(visible, signal1, signal2, formulaDifference, massDifferenceAbs,
        massErrorAbs, massErrorPpm);
  }

  private double mzDoubleParser(String diffStr) {
    try {
      return formats.mzFormat().parse(diffStr).doubleValue();
    } catch (ParseException e) {
      return 0.0d;
    }
  }
}
