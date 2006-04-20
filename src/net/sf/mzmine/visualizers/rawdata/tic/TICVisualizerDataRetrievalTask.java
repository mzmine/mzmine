/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.io.IOException;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
public class TICVisualizerDataRetrievalTask implements Task {

    private RawDataFile rawDataFile;
    private TICVisualizer visualizer;
    private int scanNumbers[];
    private int retrievedScans = 0;
    private TaskStatus status;
    private String errorMessage;
    private boolean xicMode = false;
    private double mzRangeMin, mzRangeMax;

    /**
     * constructor for TIC
     * @param rawDataFile
     * @param scanNumbers
     * @param visualizer
     */
    TICVisualizerDataRetrievalTask(RawDataFile rawDataFile, int scanNumbers[],
            TICVisualizer visualizer) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;
        this.scanNumbers = scanNumbers;
    }

    /**
     * constructor for XIC
     * @param rawDataFile
     * @param scanNumbers
     * @param visualizer
     * @param mzRangeMin
     * @param mzRangeMax
     */
    TICVisualizerDataRetrievalTask(RawDataFile rawDataFile, int scanNumbers[],
            TICVisualizer visualizer, double mzRangeMin, double mzRangeMax) {
        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;
        this.scanNumbers = scanNumbers;
        xicMode = true;
        this.mzRangeMin = mzRangeMin;
        this.mzRangeMax = mzRangeMax;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating TIC visualizer of " + rawDataFile;
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
        double intensityValues[], mzValues[] = null, totalIntensity;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                scan = rawDataFile.getScan(scanNumbers[i]);
                totalIntensity = 0;
                intensityValues = scan.getIntensityValues();
                if (xicMode) mzValues = scan.getMZValues();
                for (int j = 0; j < intensityValues.length; j++) {
                    if ((!xicMode) || ((mzValues[j] >= mzRangeMin) && (mzValues[j] <= mzRangeMax)))
                        totalIntensity += intensityValues[j];
                }

                visualizer.updateData(i, scan.getRetentionTime(), totalIntensity);
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
