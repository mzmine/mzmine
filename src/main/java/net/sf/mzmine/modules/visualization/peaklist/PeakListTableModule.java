/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;

public class PeakListTableModule implements MZmineModule {

	private static final String MODULE_NAME = "Peak list table";

	@Override
	public @Nonnull String getName() {
		return MODULE_NAME;
	}

	public static void showNewPeakListVisualizerWindow(PeakList peakList) {
		ParameterSet parameters = MZmineCore.getConfiguration()
				.getModuleParameters(PeakListTableModule.class);
		final PeakListTableWindow window = new PeakListTableWindow(peakList,
				parameters);
		window.setVisible(true);
		
		// Resize window to fit data
		int scrollWidth = window.getJScrollSizeWidth()+43; //43 = Tool bar
		int scrollHeight = window.getJScrollSizeHeight()+120; //120 = Header cells
    	int screenWidth = (int)Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
    	int screenHeight = (int)Math.round(Toolkit.getDefaultToolkit().getScreenSize().getHeight());
    	if (scrollHeight > screenHeight) {
    		scrollHeight = screenHeight-80;
    		scrollWidth = scrollWidth+18; //18 = Scroll bar
    	}
    	if (scrollWidth > screenWidth) {
    		scrollWidth = screenWidth-40;
    	}
		window.setSize(new Dimension(scrollWidth, scrollHeight));
		window.setLocation(20, 20);
		
		// Hack to show the new window in front of the main window
		Timer timer = new Timer();
		timer.schedule(new TimerTask() { public void run() { window.toFront(); } }, 200); //msecs
		timer.schedule(new TimerTask() { public void run() { window.toFront(); } }, 400); //msecs
		
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return PeakListTableParameters.class;
	}

}