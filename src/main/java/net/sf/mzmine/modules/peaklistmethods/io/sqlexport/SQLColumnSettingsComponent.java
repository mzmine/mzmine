/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.sqlexport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.util.GUIUtils;

public class SQLColumnSettingsComponent extends JPanel implements
	ActionListener {

    private final JTable columnsTable;
    private final JButton addColumnButton, removeColumnButton;

    @Nonnull
    private SQLColumnSettings value;

    public SQLColumnSettingsComponent() {

	super(new BorderLayout());

	value = new SQLColumnSettings();

	columnsTable = new JTable(value);
	columnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	columnsTable.setRowSelectionAllowed(true);
	columnsTable.setColumnSelectionAllowed(false);
	columnsTable.getTableHeader().setReorderingAllowed(false);
	columnsTable.getTableHeader().setResizingAllowed(true);
	columnsTable.setPreferredScrollableViewportSize(new Dimension(400, 80));

	JComboBox dataTypeCombo = new JComboBox(SQLExportDataType.values());
	DefaultCellEditor dataTypeEditor = new DefaultCellEditor(dataTypeCombo);
	columnsTable.setDefaultEditor(SQLExportDataType.class, dataTypeEditor);

	JScrollPane elementsScroll = new JScrollPane(columnsTable);
	add(elementsScroll, BorderLayout.CENTER);

	// Add buttons
	JPanel buttonsPanel = new JPanel();
	BoxLayout buttonsPanelLayout = new BoxLayout(buttonsPanel,
		BoxLayout.Y_AXIS);
	buttonsPanel.setLayout(buttonsPanelLayout);
	addColumnButton = GUIUtils.addButton(buttonsPanel, "Add", null, this);
	removeColumnButton = GUIUtils.addButton(buttonsPanel, "Remove", null,
		this);
	add(buttonsPanel, BorderLayout.EAST);

    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == addColumnButton) {
	    value.addNewRow();
	}

	if (src == removeColumnButton) {
	    int selectedRow = columnsTable.getSelectedRow();
	    if (selectedRow < 0)
		return;
	    value.removeRow(selectedRow);
	}
    }

    void setValue(@Nonnull SQLColumnSettings newValue) {

	// Clear the table
	this.value = newValue;
	columnsTable.setModel(newValue);
    }

    @Nonnull
    synchronized SQLColumnSettings getValue() {
	return value;
    }

}
