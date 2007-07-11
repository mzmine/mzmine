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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableParameters;
import net.sf.mzmine.userinterface.components.PeakXICComponent;

import org.jfree.ui.OverlayLayout;

/**
 * 
 */
class PeakShapeCellRenderer implements TableCellRenderer {

    private PeakList peakList;
    private PeakListTableParameters parameters;
    
    /**
     * 
     */
    PeakShapeCellRenderer(PeakList peakList, PeakListTableParameters parameters) {
        this.peakList = peakList;
        this.parameters = parameters;
    }

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JPanel newPanel = new JPanel(new OverlayLayout());

        Color bgColor;

        if (isSelected)
            bgColor = table.getSelectionBackground();
        else
            bgColor = table.getBackground();

        newPanel.setBackground(bgColor);

        if (value instanceof Peak) {

            Peak peak = (Peak) value;
            float maxHeight = 0;
            
            switch (parameters.getPeakShapeNormalization()) {
            case GLOBALMAX:
                maxHeight = peakList.getDataPointMaxIntensity();
                break;
            case ROWMAX:
                int rowNumber = peakList.getPeakRow(peak);
                maxHeight = peakList.getRow(rowNumber).getDataPointMaxIntensity();
                break;
            default:
                maxHeight = peak.getDataPointMaxIntensity();
                break;
            }
            
            PeakXICComponent xic = new PeakXICComponent(peak, maxHeight);

            newPanel.add(xic);

            newPanel.setToolTipText(peak.toString());
           
        }

        return newPanel;

    }

}
