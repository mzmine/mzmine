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

import java.awt.Dimension;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;

/**
 * 
 */
public class PeakSummaryVisualizerModule implements MZmineModule {

    /**
     * @see net.sf.mzmine.modules.MZmineModule#getName()
     */
    @Override
    public @Nonnull String getName() {
	return "Peak list row summary";
    }

    /**
     * @see net.sf.mzmine.modules.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public static void showNewPeakSummaryWindow(PeakListRow row) {
	final PeakSummaryWindow newWindow = new PeakSummaryWindow(row);
	newWindow.setVisible(true);
	newWindow.setLocation(20, 20);
	newWindow.setSize(new Dimension(1000, 600));

	// Hack to show the new window in front of the main window
	Timer timer = new Timer();
	timer.schedule(new TimerTask() {
	    public void run() {
		newWindow.toFront();
	    }
	}, 200); // msecs
	timer.schedule(new TimerTask() {
	    public void run() {
		newWindow.toFront();
	    }
	}, 400); // msecs
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return SimpleParameterSet.class;
    }

}