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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;

/**
 * A modified JList that can reorder items in DefaultListModel by dragging with
 * mouse
 * 
 */
public class DragOrderedJList extends JList {

	private int dragFrom;

	public DragOrderedJList(DefaultListModel model) {
		super(model);

		// add mouse button pressed listener
		addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent m) {
				dragFrom = getSelectedIndex();
			}
		});

		// add mouse move listener
		addMouseMotionListener(new MouseMotionAdapter() {

			public void mouseDragged(MouseEvent m) {

				// get drag target
				int dragTo = getSelectedIndex();

				// ignore event if order has not changed
				if (dragTo == dragFrom)
					return;

				// reorder the item
				DefaultListModel listModel = (DefaultListModel) DragOrderedJList.this
						.getModel();
				Object item = listModel.elementAt(dragFrom);
				listModel.removeElementAt(dragFrom);
				listModel.add(dragTo, item);

				// update drag source
				dragFrom = dragTo;
			}
		});
	}
}