/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.*;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;
import org.jfree.data.xy.AbstractXYZDataset;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sf.mzmine.taskcontrol.TaskStatus.*;

/**
 * TIC visualizer data set.  One data set is created per file shown in this visualizer.  We need to create separate
 * data set for each file because the user may add/remove files later.
 */
public class TICDataSet extends AbstractXYZDataset implements Task {

    // Logger.
    private static final Logger LOG = Logger.getLogger(TICDataSet.class.getName());

    // For comparing small differences.
    private static final double EPSILON = 0.0000001;

    // Refresh interval (in milliseconds).
    private static final long REDRAW_INTERVAL = 100L;

    // Last time the data set was redrawn.
    private static long lastRedrawTime = System.currentTimeMillis();

    private final TICVisualizerWindow visualizer;
    private final RawDataFile dataFile;

    private final int[] scanNumbers;
    private final int totalScans;
    private int processedScans;

    private final double[] basePeakValues;
    private final double[] intensityValues;
    private final double[] rtValues;
    private final Range mzRange;
    private double intensityMin;
    private double intensityMax;

    private TaskStatus status;
    private final LinkedList<TaskListener> taskListeners;
    private String errorMessage;

    /**
     * Create the data set.
     *
     * @param file           data file to plot.
     * @param theScanNumbers scans to plot.
     * @param rangeMZ        range of m/z to plot.
     * @param window         visualizer window.
     */
    public TICDataSet(final RawDataFile file,
                      final int[] theScanNumbers,
                      final Range rangeMZ,
                      final TICVisualizerWindow window) {

        // Initialize.
        visualizer = window;
        mzRange = rangeMZ;
        dataFile = file;
        scanNumbers = theScanNumbers.clone();
        totalScans = scanNumbers.length;
        basePeakValues = new double[totalScans];
        intensityValues = new double[totalScans];
        rtValues = new double[totalScans];
        processedScans = 0;
        intensityMin = 0.0;
        intensityMax = 0.0;
        taskListeners = new LinkedList<TaskListener>();
        status = WAITING;
        errorMessage = null;

        // Start-up the refresh task.
        MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
    }

    @Override
    public void cancel() {

        setStatus(CANCELED);
    }

    @Override
    public String getErrorMessage() {

        return errorMessage;
    }

    @Override
    public double getFinishedPercentage() {

        return totalScans == 0 ? 0.0 : (double) processedScans / (double) totalScans;
    }

    @Override
    public TaskStatus getStatus() {

        return status;
    }

    @Override
    public String getTaskDescription() {

        return "Updating TIC visualizer of " + dataFile;
    }

    @Override
    public Object[] getCreatedObjects() {

        return null;
    }

    /**
     * Adds a TaskListener to this Task
     *
     * @param t The TaskListener to add
     */
    @Override
    public void addTaskListener(final TaskListener t) {

        taskListeners.add(t);
    }

    /**
     * Returns all of the TaskListeners which are listening to this task.
     *
     * @return An array containing the TaskListeners
     */
    @Override
    public TaskListener[] getTaskListeners() {

        return taskListeners.toArray(new TaskListener[taskListeners.size()]);
    }

    @Override
    public void run() {

        try {
            setStatus(PROCESSING);

            calculateValues();

            if (status != CANCELED) {

                // Always redraw when we add last value.
                refresh();

                LOG.info("TIC data calculated for " + dataFile);
                setStatus(FINISHED);
            }
        }
        catch (Throwable t) {

            LOG.log(Level.SEVERE, "Problem calculating data set values for " + dataFile, t);
            setStatus(ERROR);
            errorMessage = t.getMessage();
        }
    }

    @Override
    public int getSeriesCount() {

        return 1;
    }

    @Override
    public Comparable<String> getSeriesKey(final int series) {

        return dataFile.getName();
    }

    @Override
    public Number getZ(final int series, final int item) {

        return basePeakValues[item];
    }

    @Override
    public int getItemCount(final int series) {

        return processedScans;
    }

    @Override
    public Number getX(final int series, final int item) {

        return rtValues[item];
    }

    @Override
    public Number getY(final int series, final int item) {

        return intensityValues[item];
    }

    /**
     * Returns index of data point which exactly matches given X and Y values
     *
     * @param retentionTime retention time.
     * @param intensity     intensity.
     * @return the nearest data point index.
     */
    public int getIndex(final double retentionTime, final double intensity) {

        int index = -1;
        for (int i = 0;
             index < 0 && i < processedScans;
             i++) {

            if (Math.abs(retentionTime - rtValues[i]) < EPSILON &&
                Math.abs(intensity - intensityValues[i]) < EPSILON) {

                index = i;
            }
        }

        return index;
    }

    public int getScanNumber(final int item) {

        return scanNumbers[item];
    }

    public RawDataFile getDataFile() {

        return dataFile;
    }

    /**
     * Checks if given data point is local maximum.
     *
     * @param item the index of the item to check.
     * @return true/false if the item is   a local maximum.
     */
    public boolean isLocalMaximum(final int item) {

        final boolean isLocalMaximum;
        if (item <= 0 || item >= processedScans - 1) {

            isLocalMaximum = false;

        } else {

            final double intensity = intensityValues[item];
            isLocalMaximum = intensityValues[item - 1] <= intensity && intensity >= intensityValues[item + 1];
        }

        return isLocalMaximum;
    }

    /**
     * Gets indexes of local maxima within given range.
     *
     * @param xMin minimum of range on x-axis.
     * @param xMax maximum of range on x-axis.
     * @param yMin minimum of range on y-axis.
     * @param yMax maximum of range on y-axis.
     * @return the local maxima in the given range.
     */
    public int[] findLocalMaxima(final double xMin,
                                 final double xMax,
                                 final double yMin,
                                 final double yMax) {

        // Save data set size.
        final int currentSize = processedScans;
        final double[] rtCopy;

        // If the RT values array is not filled yet, create a smaller copy.
        if (currentSize < rtValues.length) {

            rtCopy = new double[currentSize];
            System.arraycopy(rtValues, 0, rtCopy, 0, currentSize);

        } else {

            rtCopy = rtValues;
        }

        int startIndex = Arrays.binarySearch(rtCopy, xMin);
        if (startIndex < 0) {

            startIndex = -startIndex - 1;
        }

        final int length = rtCopy.length;
        final Collection<Integer> indices = new ArrayList<Integer>(length);
        for (int index = startIndex;
             index < length && rtCopy[index] <= xMax;
             index++) {

            // Check Y range..
            final double intensity = intensityValues[index];
            if (yMin <= intensity && intensity <= yMax && isLocalMaximum(index)) {

                indices.add(index);
            }
        }

        return CollectionUtils.toIntArray(indices);
    }

    public double getMinIntensity() {

        return intensityMin;
    }

    private void calculateValues() {

        // Determine plot type.
        final PlotType plotType = visualizer != null ? visualizer.getPlotType() : PlotType.BASEPEAK;

        // Process each scan.
        for (int index = 0;
             status != CANCELED && index < totalScans;
             index++) {

            // Current scan.
            final Scan scan = dataFile.getScan(scanNumbers[index]);

            // Determine base peak value.
            final DataPoint basePeak =
                    scan.getMZRange().isWithin(mzRange) ? scan.getBasePeak() : ScanUtils.findBasePeak(scan, mzRange);
            if (basePeak != null) {

                basePeakValues[index] = basePeak.getMZ();
            }

            // Determine peak intensity.
            double intensity = 0.0;
            if (plotType == PlotType.TIC) {

                // Total ion count.
                intensity =
                        scan.getMZRange().isWithin(mzRange) ? scan.getTIC() : ScanUtils.calculateTIC(scan, mzRange);

            } else if (plotType == PlotType.BASEPEAK && basePeak != null) {

                intensity = basePeak.getIntensity();
            }

            intensityValues[index] = intensity;
            rtValues[index] = scan.getRetentionTime();

            // Update min and max.
            if (index == 0) {

                intensityMin = intensity;
                intensityMax = intensity;

            } else {

                intensityMin = Math.min(intensity, intensityMin);
                intensityMax = Math.max(intensity, intensityMax);
            }

            processedScans++;

            // Refresh every REDRAW_INTERVAL ms.
            synchronized (TICDataSet.class) {

                if (System.currentTimeMillis() - lastRedrawTime > REDRAW_INTERVAL) {

                    refresh();
                    lastRedrawTime = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     * Notify data set listener (on the EDT).
     */
    private void refresh() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                fireDatasetChanged();
            }
        });
    }

    private void fireTaskEvent() {

        final TaskEvent event = new TaskEvent(this);
        for (final TaskListener t : taskListeners) {

            t.statusChanged(event);
        }
    }

    private void setStatus(final TaskStatus newStatus) {

        status = newStatus;
        fireTaskEvent();
    }
}
