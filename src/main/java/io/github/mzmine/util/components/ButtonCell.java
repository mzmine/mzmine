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

import org.controlsfx.glyphfont.Glyph;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

/**
 * @author akshaj This class represents the eye button in the table of Fx3DVisualizer.
 * @param <T>
 */
public class ButtonCell<T> extends TableCell<T, Boolean> {
  ToggleButton button;

  public ButtonCell(TableColumn<T, Boolean> column, Glyph onGraphic, Glyph offGraphic) {
    button = new ToggleButton();
    button.setGraphic(onGraphic);
    button.setSelected(true);
    button.setOnMouseClicked(event -> {
      final TableView<T> tableView = getTableView();
      tableView.getSelectionModel().select(getTableRow().getIndex());
      tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
      if (button.isSelected()) {
        commitEdit(true);
        button.setGraphic(onGraphic);
      } else {
        commitEdit(false);
        button.setGraphic(offGraphic);
      }
    });

  }

  @Override
  protected void updateItem(Boolean item, boolean empty) {
    super.updateItem(item, empty);
    if (empty) {
      setGraphic(null);
    } else {
      setGraphic(button);
    }
  }

}
