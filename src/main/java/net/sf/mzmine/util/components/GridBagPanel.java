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
 * WARRANTY; without even the im plied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.components;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

/**
 * Simple JPanel with a GridBagLayout for easier use of the grid bag
 * constraints. It automatically adds a 5px border to the components.
 * 
 */
public class GridBagPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private GridBagLayout layout;

    private final Insets borderInsets = new Insets(0, 0, 5, 5);

    public GridBagPanel() {
	layout = new GridBagLayout();
	setLayout(layout);
    }

    /**
     * Adds a component to the given cell (gridx:gridy) of the grid. The weight
     * of the component is set to 0, therefore it is not resized even when there
     * is extra space available.
     */
    public void add(Component component, int gridx, int gridy) {
	add(component, gridx, gridy, 1, 1, 0, 0, GridBagConstraints.NONE);
    }

    /**
     * Adds a component to the given cell (gridx:gridy) of the grid. The width
     * and height of the cell are also specified, therefore this cell may span
     * over several other cells. The weight of the component is set to 0,
     * therefore it is not resized even when there is extra space available.
     */
    public void add(Component component, int gridx, int gridy, int gridwidth,
	    int gridheight) {
	add(component, gridx, gridy, gridwidth, gridheight, 0, 0,
		GridBagConstraints.NONE);
    }

    /**
     * Adds a component to the given cell (gridx:gridy) of the grid, with given
     * width and height and also weight for resizing.
     */
    public void add(Component component, int gridx, int gridy, int gridwidth,
	    int gridheight, int weightx, int weighty) {
	add(component, gridx, gridy, gridwidth, gridheight, weightx, weighty,
		GridBagConstraints.NONE);
    }

    /**
     * Adds a component to the given cell (gridx:gridy) of the grid, with given
     * width and height and also weight for resizing.
     */
    public void add(Component component, int gridx, int gridy, int gridwidth,
	    int gridheight, int weightx, int weighty, int fill) {

	GridBagConstraints constraints = new GridBagConstraints(gridx, gridy,
		gridwidth, gridheight, weightx, weighty,
		GridBagConstraints.WEST, fill, borderInsets, 0, 0);

	super.add(component);

	layout.setConstraints(component, constraints);

    }

    public void addCenter(Component component, int gridx, int gridy,
	    int gridwidth, int gridheight) {

	GridBagConstraints constraints = new GridBagConstraints(gridx, gridy,
		gridwidth, gridheight, 0, 0, GridBagConstraints.CENTER,
		GridBagConstraints.NONE, borderInsets, 0, 0);

	super.add(component);

	layout.setConstraints(component, constraints);
    }

}
