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
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * GroupableTableHeaderUI
 * 
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 */

public class GroupableTableHeaderUI extends BasicTableHeaderUI {

    public void paint(Graphics g, JComponent c) {
	Rectangle clipBounds = g.getClipBounds();
	if (header.getColumnModel() == null)
	    return;
	((GroupableTableHeader) header).setColumnMargin();
	int column = 0;
	Dimension size = header.getSize();
	Rectangle cellRect = new Rectangle(0, 0, size.width, size.height);
	Hashtable<ColumnGroup, Rectangle> h = new Hashtable<ColumnGroup, Rectangle>();
	int columnMargin = header.getColumnModel().getColumnMargin();

	Enumeration<?> enumeration = header.getColumnModel().getColumns();
	while (enumeration.hasMoreElements()) {
	    cellRect.height = size.height;
	    cellRect.y = 0;
	    TableColumn aColumn = (TableColumn) enumeration.nextElement();
	    Enumeration<?> cGroups = ((GroupableTableHeader) header)
		    .getColumnGroups(aColumn);
	    if (cGroups != null) {
		int groupHeight = 0;
		while (cGroups.hasMoreElements()) {
		    ColumnGroup cGroup = (ColumnGroup) cGroups.nextElement();
		    Rectangle groupRect = (Rectangle) h.get(cGroup);
		    if (groupRect == null) {
			groupRect = new Rectangle(cellRect);
			Dimension d = cGroup.getSize(header.getTable());
			groupRect.width = d.width;
			groupRect.height = d.height;
			h.put(cGroup, groupRect);
		    }
		    paintCell(g, groupRect, cGroup);
		    groupHeight += groupRect.height;
		    cellRect.height = size.height - groupHeight;
		    cellRect.y = groupHeight;
		}
	    }
	    cellRect.width = aColumn.getWidth() + columnMargin;
	    if (cellRect.intersects(clipBounds)) {
		paintCell(g, cellRect, column);
	    }
	    cellRect.x += cellRect.width;
	    column++;
	}
    }

    private void paintCell(Graphics g, Rectangle cellRect, int columnIndex) {
	TableColumn aColumn = header.getColumnModel().getColumn(columnIndex);
	TableCellRenderer renderer = aColumn.getHeaderRenderer();
	if (renderer == null)
	    renderer = header.getDefaultRenderer();
	Component component = renderer.getTableCellRendererComponent(
		header.getTable(), aColumn.getHeaderValue(), false, false, -1,
		columnIndex);
	rendererPane.add(component);
	rendererPane.paintComponent(g, component, header, cellRect.x,
		cellRect.y, cellRect.width, cellRect.height, true);
    }

    private void paintCell(Graphics g, Rectangle cellRect, ColumnGroup cGroup) {
	TableCellRenderer renderer = cGroup.getHeaderRenderer();
	Component component = renderer.getTableCellRendererComponent(
		header.getTable(), cGroup.getHeaderValue(), false, false, -1,
		-1);
	rendererPane.add(component);
	rendererPane.paintComponent(g, component, header, cellRect.x,
		cellRect.y, cellRect.width, cellRect.height, true);
    }

    private int getHeaderHeight() {
	int height = 0;
	TableColumnModel columnModel = header.getColumnModel();
	for (int column = 0; column < columnModel.getColumnCount(); column++) {
	    TableColumn aColumn = columnModel.getColumn(column);
	    TableCellRenderer renderer = aColumn.getHeaderRenderer();
	    if (renderer == null)
		renderer = header.getDefaultRenderer();
	    Component comp = renderer.getTableCellRendererComponent(
		    header.getTable(), aColumn.getHeaderValue(), false, false,
		    -1, column);
	    int cHeight = comp.getPreferredSize().height;
	    Enumeration<?> en = ((GroupableTableHeader) header)
		    .getColumnGroups(aColumn);
	    if (en != null) {
		while (en.hasMoreElements()) {
		    ColumnGroup cGroup = (ColumnGroup) en.nextElement();
		    cHeight += cGroup.getSize(header.getTable()).height;
		}
	    }
	    height = Math.max(height, cHeight);
	}
	return height;
    }

    private Dimension createHeaderSize(long width) {
	TableColumnModel columnModel = header.getColumnModel();
	width += columnModel.getColumnMargin() * columnModel.getColumnCount();
	if (width > Integer.MAX_VALUE) {
	    width = Integer.MAX_VALUE;
	}
	return new Dimension((int) width, getHeaderHeight());
    }

    public Dimension getPreferredSize(JComponent c) {
	long width = 0;
	Enumeration<?> enumeration = header.getColumnModel().getColumns();
	while (enumeration.hasMoreElements()) {
	    TableColumn aColumn = (TableColumn) enumeration.nextElement();
	    width = width + aColumn.getPreferredWidth();
	}
	return createHeaderSize(width);
    }

}
