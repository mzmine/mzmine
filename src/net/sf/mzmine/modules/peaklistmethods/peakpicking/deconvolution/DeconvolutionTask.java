/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * @see
 */
class DeconvolutionTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList originalPeakList;

    // scan counter
    private int processedRows = 0, totalRows;
    private int newPeakID = 1;

    // User parameters
    private String suffix;
    private boolean removeOriginal;

    private int peakResolverTypeNumber;

    // Peak Resolvers
    private PeakResolver peakResolver;

    private ParameterSet pbParameters;
    
	private SimplePeakList newPeakList;

    /**
     * @param dataFile
     * @param parameters
     */
    DeconvolutionTask(PeakList peakList, DeconvolutionParameters parameters) {

        this.originalPeakList = peakList;

        peakResolverTypeNumber = parameters.getPeakResolverTypeNumber();
        pbParameters = parameters.getPeakResolverParameters(peakResolverTypeNumber);
        suffix = (String) parameters.getParameterValue(DeconvolutionParameters.suffix);
        removeOriginal = (Boolean) parameters.getParameterValue(DeconvolutionParameters.autoRemove);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Peak recognition on " + originalPeakList;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0;
        else
            return (double) processedRows / totalRows;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        setStatus( TaskStatus.PROCESSING );

        logger.info("Started peak recognition on " + originalPeakList);

        // Create new peak constructor according with the user's selection
        String peakResolverClassName = DeconvolutionParameters.peakResolverClasses[peakResolverTypeNumber];
        try {
            Class peakResolverClass = Class.forName(peakResolverClassName);
            Constructor peakResolverConstruct = peakResolverClass.getConstructors()[0];
            peakResolver = (PeakResolver) peakResolverConstruct.newInstance(pbParameters);
        } catch (Exception e) {
            errorMessage = "Error trying to make an instance of peak builder "
                    + peakResolverClassName;
            setStatus( TaskStatus.ERROR );
            return;
        }

        // Get data file information
        RawDataFile dataFile = originalPeakList.getRawDataFile(0);
        int scanNumbers[] = dataFile.getScanNumbers(1);
        double retentionTimes[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++)
            retentionTimes[i] = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        double intensities[] = new double[scanNumbers.length];

        // Create new peak list
		newPeakList = new SimplePeakList(
                originalPeakList + " " + suffix, dataFile);

        totalRows = originalPeakList.getNumberOfRows();

        for (ChromatographicPeak chromatogram : originalPeakList.getPeaks(dataFile)) {

            if ( isCanceled( ))
                return;

            // Load the intensities into array
            for (int i = 0; i < scanNumbers.length; i++) {
            	DataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);
				if (dp != null)
					intensities[i] = dp.getIntensity();
                else
                    intensities[i] = 0;
            }

            // Resolve peaks
            ChromatographicPeak peaks[] = peakResolver.resolvePeaks(
                    chromatogram, scanNumbers, retentionTimes, intensities);

            // Add peaks to the new peak list
            for (ChromatographicPeak finishedPeak : peaks) {
                SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                newPeakID++;
                newRow.addPeak(dataFile, finishedPeak);
                newPeakList.addRow(newRow);
            }

            processedRows++;
        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(newPeakList);
        
        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(originalPeakList);
        
		// Load previous applied methods
		for (PeakListAppliedMethod proc: originalPeakList.getAppliedMethods()){
			newPeakList.addDescriptionOfAppliedTask(proc);
		}
        
        // Add task description to peakList
        newPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Peak deconvolution by " + 
        		DeconvolutionParameters.peakResolverNames[peakResolverTypeNumber], pbParameters));


        setStatus( TaskStatus.FINISHED );

        logger.info("Finished peak recognition on " + originalPeakList);

    }

	public Object[] getCreatedObjects() {
		return new Object[] { newPeakList };
	}
	
}
