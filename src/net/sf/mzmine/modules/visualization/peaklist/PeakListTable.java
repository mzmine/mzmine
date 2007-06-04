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

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableColumnModel.CommonColumns;
import net.sf.mzmine.userinterface.components.PopupListener;

import com.sun.java.TableSorter;

public class PeakListTable extends JTable {

    static final String UNKNOWN_IDENTITY = "Unknown";
    
    private TableSorter sorter;
    private PeakListTableModel tableModel;
    private PeakList peakList;
    private PeakListTableColumnModel cm;
    
    public PeakListTable(
            PeakListTableParameters parameters,
            PeakList peakList) {

        //ComponentCellRenderer rend = new ComponentCellRenderer();
        //setDefaultRenderer(JComponent.class, rend);
        
        this.peakList = peakList;

        cm = new PeakListTableColumnModel(this, parameters, peakList);
        cm.setColumnMargin(0);
        this.setColumnModel(cm);

        
        this.setAutoCreateColumnsFromModel(false);
        
        this.tableModel = new PeakListTableModel(peakList);
        
        
        // Initialize sorter
        sorter = new TableSorter(tableModel, cm.getTableHeader());

        setModel(sorter);




        //sorter.addMouseListenerToHeaderInTable(this);
        

        
        PeakListTablePopupMenu popupMenu = new PeakListTablePopupMenu(this, peakList);
        addMouseListener(new PopupListener(popupMenu));

        setRowHeight(20);
        


    }
    
    public TableSorter getModel() {
        return sorter;
    }
    
    public PeakList getPeakList() {
        return peakList;
    }
    
    public TableCellEditor getCellEditor(int row,
            int column) {
        
        CommonColumns commonColumn = tableModel.getCommonColumn(column);
        if (commonColumn == CommonColumns.IDENTITY) {
            int peakListRowIndex = sorter.modelIndex(row);
            PeakListRow peakListRow = peakList.getRow(peakListRowIndex);
            
            CompoundIdentity identities[] = peakListRow.getCompoundIdentities();
            CompoundIdentity preferredIdentity = peakListRow.getPreferredCompoundIdentity();
            if ((identities == null) || (identities.length == 0)) return null;
            JComboBox combo = new JComboBox(identities);
            combo.addItem(UNKNOWN_IDENTITY);
            if (preferredIdentity == null) {
                combo.setSelectedItem(UNKNOWN_IDENTITY);
            } else combo.setSelectedItem(preferredIdentity);
            
            DefaultCellEditor cellEd = new DefaultCellEditor(combo);
            return cellEd;
        }
        
        return super.getCellEditor(row, column);
    
    }

}