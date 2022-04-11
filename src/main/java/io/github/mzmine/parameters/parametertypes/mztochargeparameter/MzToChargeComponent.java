/*
 * Copyright 2006-2021 The MZmine Development Team
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
 */

package io.github.mzmine.parameters.parametertypes.mztochargeparameter;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.text.ParseException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class MzToChargeComponent extends BorderPane {

  private final NumberFormat mzFormat;

  private TableView<MzRangeToCharge> table;

  private TableColumn<MzRangeToCharge, Double> lower;
  private TableColumn<MzRangeToCharge, Double> upper;
  private TableColumn<MzRangeToCharge, Integer> charge;

  public MzToChargeComponent() {
    setPadding(new Insets(5));
    setPrefWidth(300);

    mzFormat = MZmineCore.getConfiguration().getMZFormat();

    lower = new TableColumn<>("Lower m/z");
    upper = new TableColumn<>("Upper m/z");
    charge = new TableColumn<>("Charge");
    lower.setEditable(true);
    upper.setEditable(true);
    charge.setEditable(true);

    lower.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
      @Override
      public String toString(Double object) {
        return mzFormat.format(object);
      }

      @Override
      public Double fromString(String string) {
        try {
          return mzFormat.parse(string).doubleValue();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        return Double.NaN;
      }
    }));

    upper.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
      @Override
      public String toString(Double object) {
        return mzFormat.format(object);
      }

      @Override
      public Double fromString(String string) {
        try {
          return mzFormat.parse(string).doubleValue();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        return Double.NaN;
      }
    }));

    charge.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
      @Override
      public String toString(Integer object) {
        return String.valueOf(object);
      }

      @Override
      public Integer fromString(String string) {
        try {
          return Integer.parseInt(string);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        return 0;
      }
    }));

    lower.setCellValueFactory(param -> param.getValue().lowerProperty().asObject());
    upper.setCellValueFactory(param -> param.getValue().upperProperty().asObject());
    charge.setCellValueFactory(param -> param.getValue().chargeProperty().asObject());

    table = new TableView<>();
    table.getColumns().addAll(lower, upper, charge);
    table.setEditable(true);
    table.setMaxHeight(200);

    setCenter(table);

    FlowPane flowPane = new FlowPane();
    flowPane.setHgap(5);
    flowPane.setAlignment(Pos.TOP_CENTER);

    Button addRow = new Button("Add row");
    Button removeRow = new Button("Remove row");

    addRow.setOnAction(e -> {
      table.getItems().add(new MzRangeToCharge(0, 1, 1));
    });
    removeRow.setOnAction(e -> {
      MzRangeToCharge row = table.getSelectionModel().getSelectedItem();
      if (row != null) {
        table.getItems().remove(row);
      }
    });
    flowPane.getChildren().addAll(addRow, removeRow);
    flowPane.setPadding(new Insets(5));
    setBottom(flowPane);
  }

  public RangeMap<Double, Integer> getValue() {
    RangeMap<Double, Integer> value = TreeRangeMap.create();
    for (MzRangeToCharge row : table.getItems()) {
      value.put(Range.closedOpen(row.getLower(), row.getUpper()), row.getCharge());
    }

    return value;
  }

  public void setValue(RangeMap<Double, Integer> value) {
    table.getItems().clear();
    value.asMapOfRanges().entrySet().forEach(entry -> table.getItems().add(
        new MzRangeToCharge(entry.getKey().lowerEndpoint(), entry.getKey().upperEndpoint(),
            entry.getValue())));
  }
}
