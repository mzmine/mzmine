/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.experimentaltic;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;


/**
 *
 */
class RawDataFileDataSet extends DefaultTableXYDataset  {

    private RawDataFile rawDataFile;
    private int[] scanNumbers;
    private XYSeries series;
    
    RawDataFileDataSet(RawDataFile rawDataFile, int msLevel, TICVisualizer visualizer) {
        
        series = new XYSeries(rawDataFile.getOriginalFile().getName(), false, false);
        
        addSeries(series);
        
        this.rawDataFile = rawDataFile;
        
        scanNumbers = rawDataFile.getScanNumbers(msLevel);
        assert scanNumbers != null;

        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                series);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
            visualizer.taskStarted(updateTask);
            updateTask.run();
            visualizer.taskFinished(updateTask);
        } else
            TaskController.getInstance().addTask(updateTask, visualizer);
        
    }
    
    int getSeriesIndex(double retentionTime, double intensity) {
        int seriesIndex = series.indexOf(retentionTime);
        if (seriesIndex < 0) return -1;
        if (series.getY(seriesIndex).equals(intensity)) return seriesIndex;
        return -1;
    }
    
    int getScanNumber(int index) {
        return scanNumbers[index];
    }
    
    RawDataFile getRawDataFile() {
        return rawDataFile;
    }
}
