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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.border.Border;

/**
 * A modified JList that can reorder items in DefaultListModel by dragging with
 * mouse
 * 
 */
public class OrderComponent<ValueType> extends JList<ValueType> implements
	MouseListener, MouseMotionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int dragFrom;
    private final DefaultListModel<ValueType> listModel;

    public OrderComponent() {

	// for some reason, plain JList does not have a border (at least on Mac)
	Border border = BorderFactory.createEtchedBorder();
	setBorder(border);

	listModel = new DefaultListModel<ValueType>();
	setModel(listModel);

	// add mouse listeners
	addMouseListener(this);
	addMouseMotionListener(this);

    }

    public Object[] getValues() {
	return listModel.toArray();
    }

    public void setValues(ValueType newValues[]) {
	listModel.removeAllElements();
	for (ValueType value : newValues)
	    listModel.addElement(value);

	// Adjust the size of the component
	Dimension size = getPreferredSize();
	if (size.width < 150)
	    size.width = 150;
	setPreferredSize(size);

    }

    @Override
    public void mouseDragged(MouseEvent event) {
	// get drag target
	int dragTo = getSelectedIndex();

	// ignore event if order has not changed
	if (dragTo == dragFrom)
	    return;

	// reorder the item

	ValueType item = listModel.elementAt(dragFrom);
	listModel.removeElementAt(dragFrom);
	listModel.add(dragTo, item);

	// update drag source
	dragFrom = dragTo;
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }

    @Override
    public void mousePressed(MouseEvent event) {
	dragFrom = getSelectedIndex();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }
}