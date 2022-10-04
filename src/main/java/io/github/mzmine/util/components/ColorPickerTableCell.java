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
package io.github.mzmine.util.components;

import io.github.mzmine.main.MZmineCore;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

/**
 * @param <T>
 * @author akshaj This class represents the color picker in the table of Fx3DVisualizer.
 */
public class ColorPickerTableCell<T> extends TableCell<T, Color> {

  private final ColorPicker colorPicker;

  public ColorPickerTableCell(TableColumn<T, Color> column) {
    colorPicker = new ColorPicker();
    colorPicker.editableProperty().bind(column.editableProperty());
    colorPicker.disableProperty().bind(column.editableProperty().not());
    colorPicker.setOnShowing(event -> {
      final TableView<T> tableView = getTableView();
      tableView.getSelectionModel().select(getTableRow().getIndex());
      tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
    });
    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      commitEdit(newValue);
    });
    colorPicker.getCustomColors().addAll(MZmineCore.getConfiguration().getDefaultColorPalette());
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
  }

  @Override
  protected void updateItem(Color item, boolean empty) {

    super.updateItem(item, empty);

    setText(null);
    if (empty) {
      setGraphic(null);
    } else {
      colorPicker.setValue(item);
      setGraphic(colorPicker);
    }
  }
}
