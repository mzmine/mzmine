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

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import net.sf.mzmine.modules.visualization.alignmentresult.AlignmentResultTableColumns.RawDataColumnType;
import net.sf.mzmine.userinterface.components.ComponentCellRenderer;
import net.sf.mzmine.userinterface.components.GroupableTableHeader;
import sunutils.TableSorter;

public class AlignmentResultTable extends JTable {

    private TableSorter sorter;
    private AlignmentResultTableModel tableModel;
    private GroupableTableHeader header;

    public AlignmentResultTable(
            AlignmentResultTableVisualizerWindow masterFrame,
            AlignmentResultTableModel tableModel) {

        ComponentCellRenderer rend = new ComponentCellRenderer();
        setDefaultRenderer(JComponent.class, rend);
        
        
        this.tableModel = tableModel;
        // Initialize sorter
        sorter = new TableSorter(tableModel);

        setModel(sorter);

        TableColumnModel cm = this.getColumnModel();

        header = new GroupableTableHeader(cm);

        this.setTableHeader(header);
        cm.setColumnMargin(0);

        
        tableModel.createGroups(header, columnModel);

        sorter.addMouseListenerToHeaderInTable(this);

        setRowHeight(20);

    }

    public void createDefaultColumnsFromModel() {
        super.createDefaultColumnsFromModel();
        TableColumnModel columnModel = this.getColumnModel();
        if ((columnModel != null) && (tableModel != null)){
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (tableModel.getColumnName(i).equals(RawDataColumnType.STATUS.getColumnName()))
                    columnModel.getColumn(i).setWidth(50);
            }
        }
        if ((tableModel != null) && (header!= null)) tableModel.createGroups(header, columnModel);


    }

}