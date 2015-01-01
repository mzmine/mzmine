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

import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * ColumnGroup
 * 
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 */

public class ColumnGroup {

    protected TableCellRenderer renderer;
    protected Vector<Object> v;
    protected String text;
    protected int margin = 0;

    public ColumnGroup(String text) {
	this(null, text);
    }

    public ColumnGroup(TableCellRenderer renderer, String text) {
	if (renderer == null) {
	    this.renderer = new DefaultTableCellRenderer() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		    JTableHeader header = table.getTableHeader();
		    if (header != null) {
			setForeground(header.getForeground());
			setBackground(header.getBackground());
			setFont(header.getFont());
		    }
		    setHorizontalAlignment(JLabel.CENTER);
		    setText((value == null) ? "" : value.toString());
		    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		    return this;
		}
	    };
	} else {
	    this.renderer = renderer;
	}
	this.text = text;
	v = new Vector<Object>();
    }

    /**
     * @param obj
     *            TableColumn or ColumnGroup
     */
    public void add(Object obj) {
	if (obj == null) {
	    return;
	}
	v.addElement(obj);
    }

    /**
     * @param c
     *            TableColumn
     * @param v
     *            ColumnGroups
     */
    public Vector<?> getColumnGroups(TableColumn c, Vector<ColumnGroup> g) {
	g.addElement(this);
	if (v.contains(c))
	    return g;
	Enumeration<Object> en = v.elements();
	while (en.hasMoreElements()) {
	    Object obj = en.nextElement();
	    if (obj instanceof ColumnGroup) {
		Vector<ColumnGroup> clone = new Vector<ColumnGroup>(g);
		Vector<?> groups = (Vector<?>) ((ColumnGroup) obj)
			.getColumnGroups(c, clone);
		if (groups != null)
		    return groups;
	    }
	}
	return null;
    }

    public TableCellRenderer getHeaderRenderer() {
	return renderer;
    }

    public void setHeaderRenderer(TableCellRenderer renderer) {
	if (renderer != null) {
	    this.renderer = renderer;
	}
    }

    public Object getHeaderValue() {
	return text;
    }

    public Dimension getSize(JTable table) {
	Component comp = renderer.getTableCellRendererComponent(table,
		getHeaderValue(), false, false, -1, -1);
	int height = comp.getPreferredSize().height;
	int width = 0;
	Enumeration<?> en = v.elements();
	while (en.hasMoreElements()) {
	    Object obj = en.nextElement();
	    if (obj instanceof TableColumn) {
		TableColumn aColumn = (TableColumn) obj;
		width += aColumn.getWidth();
		width += margin;
	    } else {
		width += ((ColumnGroup) obj).getSize(table).width;
	    }
	}
	return new Dimension(width, height);
    }

    public void setColumnMargin(int margin) {
	this.margin = margin;
	Enumeration<?> en = v.elements();
	while (en.hasMoreElements()) {
	    Object obj = en.nextElement();
	    if (obj instanceof ColumnGroup) {
		((ColumnGroup) obj).setColumnMargin(margin);
	    }
	}
    }
}
