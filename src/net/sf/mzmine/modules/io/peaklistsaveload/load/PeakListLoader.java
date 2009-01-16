/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.io.peaklistsaveload.load;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class PeakListLoader implements MZmineModule, ActionListener {
	
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakListLoaderParameters parameters;
    private Desktop desktop;


	public ParameterSet getParameterSet() {
        return parameters;
	}

	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

        parameters = new PeakListLoaderParameters();

        desktop.addMenuItem(MZmineMenu.PEAKLISTEXPORT, "Load peak list",
                "Load a peak list from a file", KeyEvent.VK_E, true,
                this, null);
		
	}
	
	public void initModule(JMenuItem menuItem) {

		this.desktop = MZmineCore.getDesktop();

        parameters = new PeakListLoaderParameters();
        
        menuItem.addActionListener(this);
		
	}

	public void setParameters(ParameterSet parameterValues) {
        this.parameters = (PeakListLoaderParameters) parameters;
	}

	public void actionPerformed(ActionEvent e) {
		
		ExitCode setupExitCode = setupParameters(parameters);

        if (setupExitCode != ExitCode.OK) {
            return;
        }

        runModule(null, null, parameters, null);
	}

	public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters, TaskGroupListener taskGroupListener) {


        PeakListLoaderTask task = new PeakListLoaderTask((PeakListLoaderParameters) parameters);

        TaskGroup newGroup = new TaskGroup(new Task[]{task}, null,
                taskGroupListener);

        // start this group
        newGroup.start();

        return newGroup;

	}

	public ExitCode setupParameters(ParameterSet parameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (PeakListLoaderParameters) parameters);

        dialog.setVisible(true);

        return dialog.getExitCode();	}
	

}
