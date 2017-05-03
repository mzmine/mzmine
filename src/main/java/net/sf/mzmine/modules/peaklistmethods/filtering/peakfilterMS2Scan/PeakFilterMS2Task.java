/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang 
 * at the Dorrestein Lab (University of California, San Diego). 
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.peaklistmethods.filtering.peakfilterMS2Scan;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter.RowsFilterParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

import com.google.common.collect.Range;

/**
 * Filters out peaks from peak list.
 */
public class PeakFilterMS2Task extends AbstractTask {

    // Logger
    private static final Logger LOG = Logger.getLogger(PeakFilterMS2Task.class
            .getName());

    // Peak lists
    private final MZmineProject project;
    private final PeakList origPeakList;
    private PeakList filteredPeakList;

    // Processed rows counter
    private int processedRows, totalRows;

    // Parameters
    private final ParameterSet parameters;

    /**
     * Create the task.
     *
     * @param list
     *            peak list to process.
     * @param parameterSet
     *            task parameters.
     */
    public PeakFilterMS2Task(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet) {

        // Initialize
        this.project = project;
        parameters = parameterSet;
        origPeakList = list;
        filteredPeakList = null;
        processedRows = 0;
        totalRows = 0;
    }

    @Override
    public double getFinishedPercentage() {
        return totalRows == 0 ? 0.0 : (double) processedRows
                / (double) totalRows;
    }

    @Override
    public String getTaskDescription() {
        return "Filtering peak list";
    }

    @Override
    public void run() {

        if (isCanceled()) {
            return;
        }

        try {
            setStatus(TaskStatus.PROCESSING);
            LOG.info("Filtering peak list");

            // Filter the peak list
            filteredPeakList = filterPeakList(origPeakList);

            if (!isCanceled()) {

                // Add new peaklist to the project
                project.addPeakList(filteredPeakList);

                // Remove the original peaklist if requested
                if (parameters.getParameter(PeakFilterMS2Parameters.AUTO_REMOVE)
                        .getValue()) {
                    project.removePeakList(origPeakList);
                }
                setStatus(TaskStatus.FINISHED);
                LOG.info("Finished peak list filter");
            }
        } catch (Throwable t) {

            setErrorMessage(t.getMessage());
            setStatus(TaskStatus.ERROR);
            LOG.log(Level.SEVERE, "Peak list filter error", t);
        }

    }

    /**
     * Filter the peak list.
     *
     * @param peakList
     *            peak list to filter.
     * @return a new peak list with entries of the original peak list that pass
     *         the filtering.
     */
    private PeakList filterPeakList(final PeakList peakList) {

        // Make a copy of the peakList
        final PeakList newPeakList = new SimplePeakList(peakList.getName()
                + ' '
                + parameters.getParameter(RowsFilterParameters.SUFFIX)
                        .getValue(), peakList.getRawDataFiles());

        // Loop through all rows in peak list
        final PeakListRow[] rows = peakList.getRows();
        totalRows = rows.length;
        for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {
            final PeakListRow row = rows[processedRows];
            final RawDataFile[] rawdatafiles = row.getRawDataFiles();
            int totalRawDataFiles = rawdatafiles.length;
            boolean[] keepPeak = new boolean[totalRawDataFiles];

            for (int i = 0; i < totalRawDataFiles; i++) {
                // Peak values
                keepPeak[i] = true;
                final Feature peak = row.getPeak(rawdatafiles[i]);
                final int msmsScanNumber = peak.getMostIntenseFragmentScanNumber();
                //Check MS/MS filter 
  
            	if(msmsScanNumber < 1)
            		keepPeak[i] = false;	
            }
            newPeakList.addRow(copyPeakRow(row, keepPeak));

        }

        return newPeakList;
    }

    /**
     * Create a copy of a peak list row.
     */
    private static PeakListRow copyPeakRow(final PeakListRow row,
            final boolean[] keepPeak) {

        // Copy the peak list row.
        final PeakListRow newRow = new SimplePeakListRow(row.getID());
        PeakUtils.copyPeakListRowProperties(row, newRow);

        // Copy the peaks.
        int i = 0;
        for (final Feature peak : row.getPeaks()) {

            // Only keep peak if it fulfills the filter criteria
            if (keepPeak[i]) {
                final Feature newPeak = new SimpleFeature(peak);
                PeakUtils.copyPeakProperties(peak, newPeak);
                newRow.addPeak(peak.getDataFile(), newPeak);
            }
            i++;
        }

        return newRow;
    }
}
