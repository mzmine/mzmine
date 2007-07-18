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

package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;

public class TICRawDataAcceptor implements RawDataAcceptor {

	private RawDataAcceptor[] dataAcceptingDataSets;
	private RawDataFile rawDataFile;
	
	TICRawDataAcceptor(RawDataFile dataFile, int scanNumbers[], RawDataAcceptor[] dataAcceptingDataSets, TICVisualizerWindow visualizer) {
		
		this.dataAcceptingDataSets = dataAcceptingDataSets;
		this.rawDataFile = dataFile;
		
        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating TIC visualizer of " + dataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH, visualizer);
		
	}
	
	public void addScan(Scan scan, int index, int total) {
		for (RawDataAcceptor dataSet : dataAcceptingDataSets)
			dataSet.addScan(scan, index, total);
	}

}
