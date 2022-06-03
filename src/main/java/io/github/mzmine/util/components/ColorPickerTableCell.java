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
