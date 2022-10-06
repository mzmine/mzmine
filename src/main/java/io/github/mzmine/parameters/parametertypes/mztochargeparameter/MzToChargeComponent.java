/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
