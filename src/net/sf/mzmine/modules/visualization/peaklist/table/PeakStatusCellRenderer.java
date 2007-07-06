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
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jfree.ui.OverlayLayout;

import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.userinterface.components.ColorCircle;

/**
 * Table cell renderer
 */
class PeakStatusCellRenderer implements TableCellRenderer {

    private static final ColorCircle greenCircle = new ColorCircle(Color.green);
    private static final ColorCircle redCircle = new ColorCircle(Color.red);
    private static final ColorCircle yellowCircle = new ColorCircle(Color.yellow);

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

        if (hasFocus) {
            Border border = null;
            if (isSelected)
                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
            if (border == null)
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
            newPanel.setBorder(border);
        }

        if (value != null) {
            PeakStatus status = (PeakStatus) value;

            switch (status) {
            case DETECTED:
                newPanel.add(greenCircle);
                break;
            case ESTIMATED:
                newPanel.add(yellowCircle);
                break;
            default:
                newPanel.add(redCircle);
                break;
            }

            newPanel.setToolTipText(status.toString());

        } else {
            newPanel.add(redCircle);
        }

        return newPanel;

    }

}
