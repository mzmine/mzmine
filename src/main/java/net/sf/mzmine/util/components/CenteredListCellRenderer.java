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

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

/**
 * Same as DefaultListCellRenderer, only centers the contents of the cell
 */
public class CenteredListCellRenderer extends DefaultListCellRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Component getListCellRendererComponent(JList<?> jList, Object o,
	    int i, boolean b, boolean b1) {

	JLabel rendrlbl = (JLabel) super.getListCellRendererComponent(jList, o,
		i, b, b1);

	rendrlbl.setHorizontalAlignment(SwingConstants.CENTER);

	return rendrlbl;

    }

}
