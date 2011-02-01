/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import java.text.NumberFormat;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * Simple table cell renderer that renders percentage instead of double values
 */
public class PercentageCellRenderer extends DefaultTableCellRenderer {

	private final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	protected void setValue(Object value) {
		if ((value == null) || (!(value instanceof Double))) {
			super.setValue(value);
		} else {
			String formatted = percentFormat.format((Double) value);
			super.setValue(formatted);
		}
	}

}
