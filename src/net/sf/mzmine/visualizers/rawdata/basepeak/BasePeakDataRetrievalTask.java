/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.io.IOException;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
public class BasePeakDataRetrievalTask implements Task {

    private RawDataFile rawDataFile;
    private BasePeakVisualizer visualizer;
    private int scanNumbers[];
    private int retrievedScans = 0;
    private TaskStatus status;
    private String errorMessage;

    /**
     * @param rawDataFile
     * @param scanNumbers
     * @param visualizer
     */
    BasePeakDataRetrievalTask(RawDataFile rawDataFile, int scanNumbers[],
            BasePeakVisualizer visualizer) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;
        this.scanNumbers = scanNumbers;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating base peak visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return (float) retrievedScans / scanNumbers.length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        // this task has no result
        return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return TaskPriority.HIGH;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        Scan scan;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                
                scan = rawDataFile.getScan(scanNumbers[i]);
                visualizer.updateData(i, 
                        scan.getRetentionTime(), 
                        scan.getBasePeakIntensity(),
                        scan.getBasePeakMZ());
                
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            retrievedScans++;

        }

        status = TaskStatus.FINISHED;

    }

}
