/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.sqlexport;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

public class SQLStringCellEditor extends TableCell<SQLRowObject, String> {

	private TextField textField;
	private SQLColumnSettings value;

	public  SQLStringCellEditor(SQLColumnSettings value){
		this.value=value;
		textField = new TextField(getString());
		textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
		textField.setOnAction(event -> commitEdit(textField.getText()));

	}

	@Override
	public void startEdit() {
		super.startEdit();
		setGraphic(textField);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		textField.selectAll();
	}


	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		int column=this.getTableColumn().getText().equals(value.getColumnName(0))?0:2;
		if (empty) {
			setText(null);
			setGraphic(textField);
		} else {
			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setGraphic(textField);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			} else {
				setText(getString());
				setContentDisplay(ContentDisplay.TEXT_ONLY);
			}

			if(this.getTableRow()!=null) { // this becomes false only at the time of initialization
				value.setValueAt(getString(), this.getTableRow().getIndex(), column);
			}
		}
	}


	private String getString() {
		return getItem() == null ? "" : getItem();
	}
}