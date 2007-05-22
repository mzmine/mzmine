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

package net.sf.mzmine.modules.visualization.alignmentresult;

import javax.swing.JTable;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.userinterface.components.ComponentCellRenderer;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelectionAcceptor;
import sunutils.TableSorter;

public class AlignmentResultTable extends JTable implements
        AlignmentResultColumnSelectionAcceptor {

    private AlignmentResult alignmentResult;
    private AlignmentResultTableModel tableModel;
    private TableSorter sorter;

    public AlignmentResultTable(
            AlignmentResultTableVisualizerWindow masterFrame,
            AlignmentResult alignmentResult) {
        this.alignmentResult = alignmentResult;

        AlignmentResultColumnSelection columnSelection = new AlignmentResultColumnSelection();
        columnSelection.setAllColumns();

        initializeTableModel(columnSelection);
        
        ComponentCellRenderer rend = new ComponentCellRenderer();
        setDefaultRenderer(Object.class, rend);
        
        setRowHeight(32);
    }

    private void initializeTableModel(
            AlignmentResultColumnSelection columnSelection) {

        tableModel = new AlignmentResultTableModel(alignmentResult,
                columnSelection);

        // Initialize sorter
        sorter = new TableSorter(tableModel);
        getTableHeader().setReorderingAllowed(false);
        sorter.addMouseListenerToHeaderInTable(this);
        setModel(sorter);

    }

    public void setColumnSelection(
            AlignmentResultColumnSelection columnSelection) {
        initializeTableModel(columnSelection);
    }

    public AlignmentResultColumnSelection getColumnSelection() {
        return tableModel.getColumnSelection();
    }

}