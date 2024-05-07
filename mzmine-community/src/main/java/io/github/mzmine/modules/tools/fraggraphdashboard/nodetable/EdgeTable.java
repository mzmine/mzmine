/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.id_fraggraph.graphstream.SubFormulaEdge;
import java.text.ParseException;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public class EdgeTable extends TableView<SubFormulaEdge> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public EdgeTable() {
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
    massDifferenceAbs.setCellValueFactory(
        cell -> cell.getValue().measuredMassDiffProperty().map(formats::mz));
    massDifferenceAbs.setMinWidth(85);
    massDifferenceAbs.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    TableColumn<SubFormulaEdge, String> massErrorAbs = new TableColumn<>("Δm/z\n(abs.)");
    massErrorAbs.setCellValueFactory(
        cell -> cell.getValue().massErrorAbsProperty().map(formats::mz));
    massErrorAbs.setMinWidth(70);
    massErrorAbs.setComparator(Comparator.comparingDouble(this::mzDoubleParser));

    TableColumn<SubFormulaEdge, String> massErrorPpm = new TableColumn<>("Δm/z\n(ppm)");
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
