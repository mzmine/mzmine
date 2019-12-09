/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.components;

import org.controlsfx.glyphfont.Glyph;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

/**
 * @author akshaj This class represents the eye button in the table of
 *         Fx3DVisualizer.
 * @param <T>
 */
public class ButtonCell<T> extends TableCell<T, Boolean> {
    ToggleButton button;

    public ButtonCell(TableColumn<T, Boolean> column, Glyph onGraphic,
            Glyph offGraphic) {
        button = new ToggleButton();
        button.setGraphic(onGraphic);
        button.setSelected(true);
        button.setOnMouseClicked(event -> {
            final TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(),
                    column);
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
