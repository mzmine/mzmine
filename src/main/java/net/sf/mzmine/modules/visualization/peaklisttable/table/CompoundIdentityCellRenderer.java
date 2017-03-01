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

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import net.sf.mzmine.datamodel.PeakIdentity;

/**
 * Table cell renderer
 */
public class CompoundIdentityCellRenderer implements TableCellRenderer {

    static final Font defaultFont = new Font("SansSerif", Font.PLAIN, 10);

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
	    boolean isSelected, boolean hasFocus, int row, int column) {

	JLabel newLabel = new JLabel();
	newLabel.setHorizontalAlignment(JLabel.LEFT);
	newLabel.setFont(defaultFont);
	newLabel.setOpaque(true);

	Color bgColor;

	if (isSelected)
	    bgColor = table.getSelectionBackground();
	else
	    bgColor = table.getBackground();

	newLabel.setBackground(bgColor);

	if (hasFocus) {
	    Border border = null;
	    if (isSelected)
		border = UIManager
			.getBorder("Table.focusSelectedCellHighlightBorder");
	    if (border == null)
		border = UIManager.getBorder("Table.focusCellHighlightBorder");

	    /*
	     * The "border.getBorderInsets(newPanel) != null" is a workaround
	     * for OpenJDK 1.6.0 bug, otherwise setBorder() may throw a
	     * NullPointerException
	     */
	    if ((border != null) && (border.getBorderInsets(newLabel) != null)) {
		newLabel.setBorder(border);
	    }
	}

	if (value instanceof PeakIdentity) {

	    PeakIdentity identity = (PeakIdentity) value;

	    newLabel.setText(identity.getName());

	    String toolTipText = identity.getDescription();

	    newLabel.setToolTipText(toolTipText);

	}

	return newLabel;

    }

}
