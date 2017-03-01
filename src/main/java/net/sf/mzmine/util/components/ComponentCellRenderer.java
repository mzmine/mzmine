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

package net.sf.mzmine.util.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openscience.cdk.interfaces.IIsotope;

/**
 * Simple table cell renderer that renders only JComponents
 */
public class ComponentCellRenderer implements TableCellRenderer,
	ListCellRenderer<Object> {

    private boolean createTooltips;
    private Font font;

    /**
     */
    public ComponentCellRenderer() {
	this(false, null);
    }

    /**
     * @param font
     */
    public ComponentCellRenderer(Font font) {
	this(false, font);
    }

    /**
     * @param font
     */
    public ComponentCellRenderer(boolean createTooltips) {
	this(createTooltips, null);
    }

    /**
     * @param font
     */
    public ComponentCellRenderer(boolean createTooltips, Font font) {
	this.createTooltips = createTooltips;
	this.font = font;
    }

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
	    boolean isSelected, boolean hasFocus, int row, int column) {

	JPanel newPanel = new JPanel();
	newPanel.setLayout(new OverlayLayout(newPanel));

	Color bgColor;

	if (isSelected)
	    bgColor = table.getSelectionBackground();
	else
	    bgColor = table.getBackground();

	newPanel.setBackground(bgColor);

	if (hasFocus) {
	    Border border = null;
	    if (isSelected)
		border = UIManager
			.getBorder("Table.focusSelectedCellHighlightBorder");
	    if (border == null)
		border = UIManager.getBorder("Table.focusCellHighlightBorder");
	    if (border != null)
		newPanel.setBorder(border);
	}

	if (value != null) {

	    if (value instanceof JComponent) {

		newPanel.add((JComponent) value);

	    } else {

		JLabel newLabel = new JLabel();
		if (value instanceof IIsotope) {
		    IIsotope is = (IIsotope) value;
		    newLabel.setText(is.getSymbol());
		} else {
		    newLabel.setText(value.toString());
		}

		if (font != null)
		    newLabel.setFont(font);
		else if (table.getFont() != null)
		    newLabel.setFont(table.getFont());

		newPanel.add(newLabel);
	    }

	    if (createTooltips)
		newPanel.setToolTipText(value.toString());

	}

	return newPanel;

    }

    /**
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
     *      java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList<?> list, Object value,
	    int index, boolean isSelected, boolean hasFocus) {

	JPanel newPanel = new JPanel();
	newPanel.setLayout(new OverlayLayout(newPanel));

	Color bgColor;

	if (isSelected)
	    bgColor = list.getSelectionBackground();
	else
	    bgColor = list.getBackground();

	newPanel.setBackground(bgColor);

	if (hasFocus) {
	    Border border = null;
	    if (isSelected)
		border = UIManager
			.getBorder("List.focusSelectedCellHighlightBorder");
	    if (border == null)
		border = UIManager.getBorder("List.focusCellHighlightBorder");
	    if (border != null)
		newPanel.setBorder(border);
	}

	if (value != null) {

	    if (value instanceof JComponent) {

		newPanel.add((JComponent) value);

	    } else {

		JLabel newLabel = new JLabel(value.toString());

		if (font != null)
		    newLabel.setFont(font);
		else if (list.getFont() != null)
		    newLabel.setFont(list.getFont());

		newPanel.add(newLabel);
	    }
	}

	return newPanel;

    }

}
