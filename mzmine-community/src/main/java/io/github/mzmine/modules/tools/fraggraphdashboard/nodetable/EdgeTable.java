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
import javafx.beans.binding.Bindings;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public class EdgeTable extends TableView<SubFormulaEdge> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public EdgeTable() {
    setRowFactory(table -> new TableRow<>() {
      { // unfortunately this does not listen to changes when the valid property changes
        disableProperty().bind(
            Bindings.createBooleanBinding(() -> getItem() != null ? getItem().isValid() : true,
                itemProperty()));
      }
    });

    TableColumn<SubFormulaEdge, String> signal1 = new TableColumn<>("Signal 1");
    signal1.getStyleClass().add("align-right-column");
    signal1.setCellValueFactory(cell -> cell.getValue().smaller().calculatedMzProperty().map(formats::mz));
    signal1.setComparator(Comparator.comparingDouble(this::mzDoubleParser));
    signal1.setMinWidth(70);

    TableColumn<SubFormulaEdge, String> signal2 = new TableColumn<>("Signal 2");
    signal2.getStyleClass().add("align-right-column");
    signal2.setCellValueFactory(cell -> cell.getValue().larger().calculatedMzProperty().map(formats::mz));
    signal2.setComparator(Comparator.comparingDouble(this::mzDoubleParser));
    signal2.setMinWidth(70);

  }

  private double mzDoubleParser(String diffStr) {
    try {
      return formats.mzFormat().parse(diffStr).doubleValue();
    } catch (ParseException e) {
      return 0.0d;
    }
  }
}
