package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.userinterface.Desktop;

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
