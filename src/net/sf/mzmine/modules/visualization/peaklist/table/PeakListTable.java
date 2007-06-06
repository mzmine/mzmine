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

package net.sf.mzmine.modules.visualization.peaklist.table;

import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableParameters;
import net.sf.mzmine.userinterface.components.GroupableTableHeader;
import net.sf.mzmine.userinterface.components.PopupListener;

import com.sun.java.TableSorter;

public class PeakListTable extends JTable {

    static final String UNKNOWN_IDENTITY = "Unknown";
   
    private static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);
    
    private TableSorter sorter;
    private PeakListTableModel pkTableModel;
    private PeakList peakList;
    private PeakListTableColumnModel cm;
    
    public PeakListTable(
            PeakListTableParameters parameters,
            PeakList peakList) {

        this.peakList = peakList;

        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        cm = new PeakListTableColumnModel(this, parameters, peakList);
        
        
        this.setAutoCreateColumnsFromModel(false);
        
        cm.setColumnMargin(0);
        setColumnModel(cm);
        
        GroupableTableHeader header = new GroupableTableHeader(cm);
        setTableHeader(header);

        // create default columns
        cm.createColumns();
        
        
        this.pkTableModel = new PeakListTableModel(peakList);
        
        
        // Initialize sorter
        sorter = new TableSorter(pkTableModel, header);

        setModel(sorter);

        
        PeakListTablePopupMenu popupMenu = new PeakListTablePopupMenu(this, peakList);
        addMouseListener(new PopupListener(popupMenu));

        setRowHeight(parameters.getRowHeight());

    }
    
    public TableSorter getModel() {
        return sorter;
    }
    
    public PeakListTableColumnModel getColumnModel() {
        return cm;
    }
    
    public PeakList getPeakList() {
        return peakList;
    }
    
    public TableCellEditor getCellEditor(int row,
            int column) {
        
        CommonColumnType commonColumn = pkTableModel.getCommonColumn(column);
        if (commonColumn == CommonColumnType.IDENTITY) {
            int peakListRowIndex = sorter.modelIndex(row);
            PeakListRow peakListRow = peakList.getRow(peakListRowIndex);
            
            CompoundIdentity identities[] = peakListRow.getCompoundIdentities();
            CompoundIdentity preferredIdentity = peakListRow.getPreferredCompoundIdentity();
            JComboBox combo;
            
            if ((identities != null) && (identities.length > 0)) {
                combo = new JComboBox(identities);
                combo.addItem("--------------");
            } else {
                combo = new JComboBox();
            }
            
            combo.setFont(comboFont);
            
            combo.addItem(UNKNOWN_IDENTITY);
            combo.addItem("Add new...");
            if (preferredIdentity == null) {
                combo.setSelectedItem(UNKNOWN_IDENTITY);
            } else combo.setSelectedItem(preferredIdentity);
            
            DefaultCellEditor cellEd = new DefaultCellEditor(combo);
            return cellEd;
        }
        
        return super.getCellEditor(row, column);
    
    }

}