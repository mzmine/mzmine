/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.util.GUIUtils;

import com.sun.java.TableSorter;

/**
 * 
 */
public class PeakListTablePopupMenu extends JPopupMenu implements
        ActionListener {

    private PeakListTable table;
    private PeakList peakList;

    private JMenuItem deleteRowsItem;
    private JMenuItem plotRowsItem;
    private JMenuItem showXICItem;

    public PeakListTablePopupMenu(PeakListTableWindow window, PeakListTable table, PeakList peakList) {

        this.table = table;
        this.peakList = peakList;



        GUIUtils.addMenuItem(this, "Set properties", window, "PROPERTIES");
        
        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows",
                this);
        
        plotRowsItem = GUIUtils.addMenuItem(this,
                "Plot selected rows using Intensity Plot module", this);

        showXICItem = GUIUtils.addMenuItem(this, "Show XIC of this peak", this);

    }

    public void show(Component invoker, int x, int y) {

        int selectedRows[] = table.getSelectedRows();

        deleteRowsItem.setEnabled(selectedRows.length > 0);
        plotRowsItem.setEnabled(selectedRows.length > 0);

        int clickedRow = table.rowAtPoint(new Point(x, y));
        int clickedColumn = table.columnAtPoint(new Point(x, y));
        if ((clickedRow >= 0) && (clickedColumn >= 0)) {
            showXICItem.setEnabled(clickedColumn >= DataFileColumnType.values().length);
        }

        super.show(invoker, x, y);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == plotRowsItem) {

            int rowsToDelete[] = table.getSelectedRows();
            // sort row indices
            Arrays.sort(rowsToDelete);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();

            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                int unsordedIndex = sorterModel.modelIndex(rowsToDelete[i]);
                peakList.removeRow(unsordedIndex);
                originalModel.fireTableRowsDeleted(unsordedIndex, unsordedIndex);
            }
            
            table.clearSelection();

        }
        
        if (src == deleteRowsItem) {

            int rowsToDelete[] = table.getSelectedRows();
            // sort row indices
            Arrays.sort(rowsToDelete);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();

            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                int unsordedIndex = sorterModel.modelIndex(rowsToDelete[i]);
                peakList.removeRow(unsordedIndex);
                originalModel.fireTableRowsDeleted(unsordedIndex, unsordedIndex);
            }
            
            table.clearSelection();

        }
        
        
        if (src == deleteRowsItem) {

            int rowsToDelete[] = table.getSelectedRows();
            // sort row indices
            Arrays.sort(rowsToDelete);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();

            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                int unsordedIndex = sorterModel.modelIndex(rowsToDelete[i]);
                peakList.removeRow(unsordedIndex);
                originalModel.fireTableRowsDeleted(unsordedIndex, unsordedIndex);
            }
            
            table.clearSelection();

        }

    }

}
