/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5;

import dulab.adap.common.algorithms.FeatureTools;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.workflow.TwoStepDecomposition;
import dulab.adap.workflow.TwoStepDecompositionParameters;
        
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.*;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;

import javax.annotation.Nonnull;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV1_5Task extends AbstractTask {
    
     // Logger.
    private static final Logger LOG = Logger.getLogger(ADAP3DecompositionV1_5Task.class.getName());
    
    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;
    private final TwoStepDecomposition decomposition;
    
    // User parameters
    private final ParameterSet parameters;
    
    ADAP3DecompositionV1_5Task(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet)
    {
        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        decomposition = new TwoStepDecomposition();
    }
    
    @Override
    public String getTaskDescription() {
        return "ADAP Peak decomposition on " + originalPeakList;
    }
    
    @Override
    public double getFinishedPercentage() {
        return decomposition.getProcessedPercent();
    }
    
    @Override
    public void run() {
        if (!isCanceled()) {
            String errorMsg = null;

            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started ADAP Peak Decomposition on " + originalPeakList);

            // Check raw data files.
            if (originalPeakList.getNumberOfRawDataFiles() > 1) {

                setStatus(TaskStatus.ERROR);
                setErrorMessage("Peak Decomposition can only be performed on peak lists with a single raw data file");

            } else {
                
                try {
                    
                    newPeakList = decomposePeaks(originalPeakList);  
                    
                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        project.addPeakList(newPeakList);

                        // Add quality parameters to peaks
                        QualityParameters.calculateQualityParameters(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(
                                ADAP3DecompositionV1_5Parameters.AUTO_REMOVE).getValue()) 
                        {
                            project.removePeakList(originalPeakList);
                        }

                        setStatus(TaskStatus.FINISHED);
                        LOG.info("Finished peak decomposition on "
                                + originalPeakList);
                    }
                    
                } catch (IllegalArgumentException e) {
                    errorMsg = "Incorrect Peak List selected:\n"
                            + e.getMessage();
                } catch (IllegalStateException e) {
                    errorMsg = "Peak decompostion error:\n"
                            + e.getMessage();
                } catch (Exception e) {
                    errorMsg = "'Unknown error' during peak decomposition. \n"
                            + e.getMessage();
                } catch (Throwable t) {

                    setStatus(TaskStatus.ERROR);
                    setErrorMessage(t.getMessage());
                    LOG.log(Level.SEVERE, "Peak decompostion error", t);
                }

                // Report error.
                if (errorMsg != null) {
                    setErrorMessage(errorMsg);
                    setStatus(TaskStatus.ERROR);
                }
            }
        }
    }
    
    private PeakList decomposePeaks(PeakList peakList)
            throws CloneNotSupportedException, IOException
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);
        
        // Create new peak list.
        final PeakList resolvedPeakList = new SimplePeakList(peakList + " "
                + parameters.getParameter(ADAP3DecompositionV1_5Parameters.SUFFIX)
                        .getValue(), dataFile);
        
        // Load previous applied methods.
        for (final PeakList.PeakListAppliedMethod method : 
                peakList.getAppliedMethods()) 
        {
            resolvedPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak list.
        resolvedPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Peak deconvolution by ADAP-3", parameters));
        
        // Collect peak information
        List <Peak> peaks = getPeaks(peakList, 
                this.parameters.getParameter(ADAP3DecompositionV1_5Parameters
                        .EDGE_TO_HEIGHT_RATIO).getValue(),
                this.parameters.getParameter(ADAP3DecompositionV1_5Parameters
                        .DELTA_TO_HEIGHT_RATIO).getValue());

        // Find components (a.k.a. clusters of peaks with fragmentation spectra)
        List <Component> components = getComponents(peaks);

        // Create PeakListRow for each components
        List <PeakListRow> newPeakListRows = new ArrayList <> ();

        int rowID = 0;

        for (final Component component : components) 
        {
            if (component.getSpectrum().isEmpty()) continue;
            
            PeakListRow row = new SimplePeakListRow(++rowID);
            
            // Add the reference peak
            PeakListRow refPeakRow = originalPeakList
                    .getRow(component.getBestPeak().getInfo().peakID);
            Feature refPeak = new SimpleFeature(refPeakRow.getBestPeak());

            // Add spectrum
            List <DataPoint> dataPoints = new ArrayList <> ();
            for (Map.Entry <Double, Double> entry : 
                    component.getSpectrum().entrySet()) 
            {
                dataPoints.add(new SimpleDataPoint(
                        entry.getKey(), entry.getValue()));
            }
            
            refPeak.setIsotopePattern(new SimpleIsotopePattern(
                    dataPoints.toArray(new DataPoint[dataPoints.size()]),
                    IsotopePattern.IsotopePatternStatus.PREDICTED,
                    "Spectrum"));
            
            row.addPeak(dataFile, refPeak);

            // Add PeakInformation
            SimplePeakInformation information = new SimplePeakInformation(
                    new HashMap<>(refPeakRow.getPeakInformation().getAllProperties()));
            row.setPeakInformation(information);
            
            // Set row properties
            row.setAverageMZ(refPeakRow.getAverageMZ());
            row.setAverageRT(refPeakRow.getAverageRT());
             
            // resolvedPeakList.addRow(row);
            newPeakListRows.add(row);
        }
        
        // ------------------------------------
        // Sort new peak rows by retention time
        // ------------------------------------
        
        Collections.sort(newPeakListRows, new Comparator <PeakListRow> () {
            @Override
            public int compare(PeakListRow row1, PeakListRow row2) 
            {
                double retTime1 = row1.getAverageRT();
                double retTime2 = row2.getAverageRT();
                
                return Double.compare(retTime1, retTime2);
            }
        });
        
        for (PeakListRow row : newPeakListRows)
            resolvedPeakList.addRow(row);
        
        return resolvedPeakList;
    }
    
    /**
     * Convert MZmine PeakList to a list of ADAP Peaks
     * 
     * @param peakList MZmine PeakList object
     * @param edgeToHeightThreshold edge-to-height threshold to determine peaks that can be merged
     * @param deltaToHeightThreshold delta-to-height threshold to determine peaks that can be merged
     * @return list of ADAP Peaks
     */

    @Nonnull
    public static List <Peak> getPeaks(final PeakList peakList,
            final double edgeToHeightThreshold,
            final double deltaToHeightThreshold)
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);
        
        List <Peak> peaks = new ArrayList <> ();
        
        for (PeakListRow row : peakList.getRows()) 
        {
            Feature peak = row.getBestPeak();
            int[] scanNumbers = peak.getScanNumbers();
            
            // Build chromatogram
            NavigableMap <Double, Double> chromatogram = new TreeMap <> ();
            for (int scanNumber : scanNumbers) {
                DataPoint dataPoint = peak.getDataPoint(scanNumber);
                if (dataPoint != null)
                    chromatogram.put(
                            dataFile.getScan(scanNumber).getRetentionTime(),
                            dataPoint.getIntensity());
            }
            
            if (chromatogram.size() <= 1) continue;
            
            // Fill out PeakInfo
            PeakInfo info = new PeakInfo();

            try {
                // Note: info.peakID is the index of PeakListRow in PeakList.peakListRows (starts from 0)
                //       row.getID is row.myID (starts from 1)
                info.peakID = row.getID() - 1;
                
                double height = -Double.MIN_VALUE;
                for (int scan : scanNumbers) {
                    double intensity = peak.getDataPoint(scan).getIntensity();
                    
                    if (intensity > height) {
                        height = intensity;
                        info.peakIndex = scan;
                    }
                }
                
                info.leftApexIndex = scanNumbers[0];
                info.rightApexIndex = scanNumbers[scanNumbers.length - 1];
                info.retTime = peak.getRT();
                info.mzValue = peak.getMZ();
                info.intensity = peak.getHeight();
                info.leftPeakIndex = info.leftApexIndex;
                info.rightPeakIndex = info.rightApexIndex;
                
            } catch (Exception e) {
                LOG.info("Skipping " + row + ": " + e.getMessage());
                continue;
            }
            
            peaks.add(new Peak(chromatogram, info));
        }
        
        FeatureTools.correctPeakBoundaries(peaks,
                edgeToHeightThreshold, deltaToHeightThreshold);
        
        return peaks;
    }
    
    /**
     * Performs ADAP Peak Decomposition
     * 
     * @param peaks list of Peaks
     * @return Collection of dulab.adap.Component objects
     */
    
    private List <Component> getComponents(List <Peak> peaks)
    {
        // -----------------------------
        // ADAP Decomposition Parameters
        // -----------------------------
        
        TwoStepDecompositionParameters params = 
                new TwoStepDecompositionParameters();
        
        params.minClusterDistance = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_DISTANCE).getValue();
        params.minClusterSize = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_SIZE).getValue();
        params.minClusterIntensity = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_INTENSITY).getValue();
        params.useIsShared = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.USE_ISSHARED).getValue();
        params.edgeToHeightRatio = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO).getValue();
        params.deltaToHeightRatio = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO).getValue();
        params.shapeSimThreshold = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD).getValue();
        params.minModelPeakSharpness = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS).getValue();
        params.modelPeakChoice = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MODEL_PEAK_CHOICE).getValue();
        params.deprecatedMZValues = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MZ_VALUES).getValue();
        
        return decomposition.run(params, peaks);
    }
}
