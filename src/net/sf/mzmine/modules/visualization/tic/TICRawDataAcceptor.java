package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataAcceptor;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;

public class TICRawDataAcceptor implements RawDataAcceptor {

	private RawDataAcceptor[] dataAcceptingDataSets;
	private RawDataFile rawDataFile;
	
	TICRawDataAcceptor(TaskController taskController, OpenedRawDataFile dataFile, int scanNumbers[], RawDataAcceptor[] dataAcceptingDataSets, TICVisualizerWindow visualizer) {
		
		this.dataAcceptingDataSets = dataAcceptingDataSets;
		this.rawDataFile = dataFile.getCurrentFile();
		
        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating TIC visualizer of " + dataFile, this);

        taskController.addTask(updateTask, TaskPriority.HIGH, visualizer);
		
	}
	
	public void addScan(Scan scan, int index, int total) {
		for (RawDataAcceptor dataSet : dataAcceptingDataSets)
			dataSet.addScan(scan, index, total);
	}

}
