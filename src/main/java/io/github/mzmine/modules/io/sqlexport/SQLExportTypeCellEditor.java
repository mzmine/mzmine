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

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

public class SQLExportTypeCellEditor extends TableCell<SQLRowObject,SQLExportDataType> {
	private final ComboBox<SQLExportDataType> dataTypeEditor ;
	private SQLColumnSettings value;

	public SQLExportTypeCellEditor(SQLColumnSettings value) {
		this.value=value;
		dataTypeEditor = new ComboBox<>();
		dataTypeEditor.getItems().addAll(FXCollections.observableArrayList(SQLExportDataType.values()));
		dataTypeEditor.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
			Boolean selected = dataTypeEditor.getSelectionModel().getSelectedItem().isSelectableValue();
			SQLExportDataType item;
			if(!selected){
				item = dataTypeEditor.getItems().get(newValue.intValue()+1);
			}
			else {
				item= dataTypeEditor.getItems().get(newValue.intValue());
			}
			commitEdit(item);
		});

	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();
		setText(getItem().toString());
		setGraphic(null);
	}

	@Override
	public void commitEdit(SQLExportDataType item) {
		super.commitEdit(item);
		setGraphic(null);
		if(this.getTableRow()!=null) { // this becomes false only at the time of initialization
			value.setValueAt(item, this.getTableRow().getIndex(), 1);
		}
	}

	@Override
	public void startEdit() {
		super.startEdit();
		String value = getItem().toString();
		if (value != null) {
			setGraphic(dataTypeEditor);
			setText(null);
		}
	}

	@Override
	protected void updateItem(SQLExportDataType item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null || empty) {
			setText(null);

		} else {
			setText(item.toString());
			if(this.getTableRow()!=null) { // this becomes false only at the time of initialization
				value.setValueAt(item, this.getTableRow().getIndex(), 1);
			}
		}
	}
}
