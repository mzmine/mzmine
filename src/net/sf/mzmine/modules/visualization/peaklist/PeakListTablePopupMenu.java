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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.util.GUIUtils;
import sunutils.TableSorter;


/**
 *
 */
public class PeakListTablePopupMenu extends JPopupMenu implements ActionListener {
    
    private PeakListTable table;
    private PeakList peakList;
    
    private JMenuItem setRowHeightItem;
    private JMenuItem deleteRowsItem;
    private JMenuItem plotRowsItem;
    private JMenu peakShapeHeightItem;
    private JMenuItem peakMaximumItem;
    private JMenuItem rowMaximumItem;
    private JMenuItem globalMaximumItem;
    private JMenuItem showXICItem;
    
    PeakListTablePopupMenu(PeakListTable table, PeakList peakList) {
        
        this.table = table;
        this.peakList = peakList;
        
        setRowHeightItem = GUIUtils.addMenuItem(this, "Set row height", this);
        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows", this);
        plotRowsItem = GUIUtils.addMenuItem(this, "Plot selected rows using Intensity Plot module", this);

        peakShapeHeightItem = new JMenu("Peak shape height...");
        add(peakShapeHeightItem);
        
        peakMaximumItem = GUIUtils.addMenuItem(peakShapeHeightItem, "Peak maximum", this);
        rowMaximumItem = GUIUtils.addMenuItem(peakShapeHeightItem, "Row maximum", this);
        globalMaximumItem = GUIUtils.addMenuItem(peakShapeHeightItem, "Global maximum", this);
        
        showXICItem = GUIUtils.addMenuItem(this, "Show XIC", this);
        
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
        
        if (src == deleteRowsItem) {
            
            int rowsToDelete[] = table.getSelectedRows();
            // sort row indices
            Arrays.sort(rowsToDelete);
            
            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                peakList.removeRow(rowsToDelete[i]);
            }
            
            table.getModel().fireTableDataChanged();
            
        }
        
    }

}
