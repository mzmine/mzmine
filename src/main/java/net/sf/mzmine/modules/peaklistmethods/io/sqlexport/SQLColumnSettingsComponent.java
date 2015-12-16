/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Font;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

import net.sf.mzmine.util.GUIUtils;

public class SQLColumnSettingsComponent extends JPanel
        implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final JTable columnsTable;
    private final JButton addColumnButton, removeColumnButton;

    @Nonnull
    private SQLColumnSettings value;

    public SQLColumnSettingsComponent() {

        super(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

        value = new SQLColumnSettings();

        // columnsTable = new JTable(value);
        columnsTable = new JTable(value) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public Component prepareRenderer(TableCellRenderer renderer,
                    int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isCellEditable(row, column)) {
                    if (isCellSelected(row, column)) {
                        c.setBackground(Color.decode("#3399FF"));
                        c.setForeground(Color.white);
                    } else {
                        c.setBackground(Color.decode("#E3E3E3"));
                    }
                } else {
                    if (isCellSelected(row, column)) {
                        c.setBackground(Color.decode("#3399FF"));
                        c.setForeground(Color.white);
                    } else {
                        c.setBackground(Color.white);
                        c.setForeground(Color.black);
                    }
                }
                return c;
            }
        };

        columnsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        columnsTable.setRowSelectionAllowed(true);
        columnsTable.setColumnSelectionAllowed(false);
        columnsTable.getTableHeader().setReorderingAllowed(false);
        columnsTable.getTableHeader().setResizingAllowed(false);
        columnsTable
                .setPreferredScrollableViewportSize(new Dimension(550, 220));

        columnsTable.setRowHeight(columnsTable.getRowHeight() + 5);
        columnsTable.setFont(new Font(getFont().getName(), Font.PLAIN, 13));

        JComboBox<SQLExportDataType> dataTypeCombo = new JComboBox<SQLExportDataType>(
                SQLExportDataType.values());
        dataTypeCombo.setMaximumRowCount(22);
        DefaultCellEditor dataTypeEditor = new DefaultCellEditor(dataTypeCombo);
        columnsTable.setDefaultEditor(SQLExportDataType.class, dataTypeEditor);

        // Create an ItemListener for the JComboBox component.
        dataTypeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox<?> dataTypeCombo = (JComboBox<?>) e.getSource();
                Boolean selected = ((SQLExportDataType) dataTypeCombo
                        .getSelectedItem()).isSelectableValue();
                if (!selected && e.getStateChange() == 1) {
                    // Invalid selection - selection of title rows in JComboBox
                    // is not allowed
                    dataTypeCombo.setSelectedIndex(
                            dataTypeCombo.getSelectedIndex() + 1);
                }
            }
        });

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
