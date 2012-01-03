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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

public class HistogramToolBar extends JToolBar {

	private static final Icon axesIcon = new ImageIcon("icons/axesicon.png");

	public HistogramToolBar(ActionListener actionPerfomer) {

		super(JToolBar.VERTICAL);

		setFloatable(false);
		setFocusable(false);
		setMargin(new Insets(5, 5, 5, 5));
		setBackground(Color.white);

		JButton button1 = new JButton(axesIcon);
		button1.addActionListener(actionPerfomer);
		button1.setActionCommand("SETUP_AXES");
		button1.setToolTipText("Setup ranges for axes");
		this.add(button1);

	}

}