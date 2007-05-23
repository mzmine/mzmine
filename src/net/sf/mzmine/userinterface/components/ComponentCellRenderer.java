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

package net.sf.mzmine.userinterface.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jfree.ui.OverlayLayout;

/**
 * Simple table cell renderer that renders only JComponents
 */
public class ComponentCellRenderer implements TableCellRenderer,
        ListCellRenderer {

    private Font font;

    /**
     */
    public ComponentCellRenderer() {
    }

    /**
     * @param font
     */
    public ComponentCellRenderer(Font font) {
        this.font = font;
    }

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JPanel newPanel = new JPanel(new OverlayLayout());
        Color bgColor;
        if (isSelected) bgColor = table.getSelectionBackground();
        else bgColor = table.getBackground();

        newPanel.setBackground(bgColor);
        
        if (hasFocus) {
            Border border = null;
            if (isSelected) border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
            if (border == null) border = UIManager.getBorder("Table.focusCellHighlightBorder");
            newPanel.setBorder(border);
        }

        if (value != null) {

            if (value instanceof JComponent) {

                newPanel.add((JComponent) value);

            } else {

                JLabel newLabel = new JLabel(value.toString());

                if (font != null)
                    newLabel.setFont(font);

                newPanel.add(newLabel);
            }
        }

        return newPanel;

    }

    /**
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
     *      java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean hasFocus) {

        if (value instanceof JComponent)
            return (Component) value;

        JLabel newLabel = new JLabel(value.toString());
        if (font != null)
            newLabel.setFont(font);
        return newLabel;

    }

}
