/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import javax.swing.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Vector;

/**
 * A modified JList that can reorder items in the DefaultListModel by dragging with the mouse.
 */
public class DragOrderedJList extends JList {

    private int dragFrom;

    /**
     * Create the list.
     */
    public DragOrderedJList() {

        // Initialize.
        super(new DefaultListModel());
        dragFrom = -1;

        // Add mouse button pressed listener.
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                dragFrom = getSelectedIndex();
            }
        });

        // Add mouse drag listener.
        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(final MouseEvent e) {

                // Get drag target
                final int dragTo = getSelectedIndex();

                // ignore event if order has not changed
                if (dragTo != dragFrom && dragFrom >= 0 && dragTo >= 0) {

                    // Reorder the items.
                    final DefaultListModel listModel = (DefaultListModel) getModel();
                    final Object item = listModel.getElementAt(dragFrom);
                    listModel.removeElementAt(dragFrom);
                    listModel.add(dragTo, item);

                    // Update drag source.
                    dragFrom = dragTo;
                }
            }
        });
    }

    @Override
    public void setModel(final ListModel model) {

        // Ensure only DefaultListModels are used.
        if (!(model instanceof DefaultListModel)) {
            throw new IllegalArgumentException("Only DefaultListModels can be used with this component");
        }
        super.setModel(model);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setListData(final Vector listData) {
        final DefaultListModel model = new DefaultListModel();
        for (final Object element : listData) {
            model.addElement(element);
        }
        setModel(model);
    }
}