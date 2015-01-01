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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaksummary;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.util.components.PeakSummaryComponent;

/**
 * 
 */
public class PeakSummaryWindow extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PeakSummaryWindow(PeakListRow row) {

	super(row.toString());

	setLayout(new BorderLayout());

	PeakSummaryComponent peakRowSummary = new PeakSummaryComponent(row,
		row.getRawDataFiles(), true, false, true, true, true,
		this.getBackground());

	add(peakRowSummary, BorderLayout.CENTER);

	// Add the Windows menu
	JMenuBar menuBar = new JMenuBar();
	menuBar.add(new WindowsMenu());
	setJMenuBar(menuBar);

	pack();

    }

}
