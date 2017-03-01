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

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * GroupableTableHeader
 * 
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 */

public class GroupableTableHeader extends JTableHeader {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected Vector<ColumnGroup> columnGroups = null;

    public GroupableTableHeader() {
	super();
	setUI(new GroupableTableHeaderUI());
	setReorderingAllowed(false);
    }

    public GroupableTableHeader(TableColumnModel model) {
	super(model);
	setUI(new GroupableTableHeaderUI());
	setReorderingAllowed(false);
    }

    public void setReorderingAllowed(boolean b) {
	reorderingAllowed = false;
    }

    public void addColumnGroup(ColumnGroup g) {
	if (columnGroups == null) {
	    columnGroups = new Vector<ColumnGroup>();
	}
	columnGroups.addElement(g);
    }

    public void removeColumnGroup(ColumnGroup g) {
	if (columnGroups == null)
	    return;
	columnGroups.remove(g);
    }

    public ColumnGroup[] getColumnGroups() {
	if (columnGroups == null)
	    return null;
	return columnGroups.toArray(new ColumnGroup[0]);
    }

    public Enumeration<?> getColumnGroups(TableColumn col) {
	if (columnGroups == null)
	    return null;
	Enumeration<ColumnGroup> en = columnGroups.elements();
	while (en.hasMoreElements()) {
	    ColumnGroup cGroup = (ColumnGroup) en.nextElement();
	    Vector<?> v_ret = (Vector<?>) cGroup.getColumnGroups(col,
		    new Vector<ColumnGroup>());
	    if (v_ret != null) {
		return v_ret.elements();
	    }
	}
	return null;
    }

    public void setColumnMargin() {
	if (columnGroups == null)
	    return;
	int columnMargin = getColumnModel().getColumnMargin();
	Enumeration<ColumnGroup> en = columnGroups.elements();
	while (en.hasMoreElements()) {
	    ColumnGroup cGroup = (ColumnGroup) en.nextElement();
	    cGroup.setColumnMargin(columnMargin);
	}
    }
}
