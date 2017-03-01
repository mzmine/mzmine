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

import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.workflow.TwoStepDecomposition;
import dulab.adap.workflow.TwoStepDecompositionParameters;
        
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakInformation;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;

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
    
    // User parameters
    private final ParameterSet parameters;
    
    private RSessionWrapper rSession;
    
    class WindowIndices {
        int[] left;
        int[] right;
    }
    
    public ADAP3DecompositionV1_5Task(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet) {

        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
    }
    
    @Override
    public String getTaskDescription() {
        return "ADAP Peak decomposition on " + originalPeakList;
    }
    
    @Override
    public double getFinishedPercentage() {
        return TwoStepDecomposition.getProcessedPercent();
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
                    // Turn off R instance.
                    if (this.rSession != null)
                        this.rSession.close(false);
                    
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
    
    PeakList decomposePeaks(PeakList peakList) 
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
        List <PeakInfo> peakInfo = new ArrayList <> ();
        Map <PeakInfo, NavigableMap <Double, Double>> chromatograms = 
                new HashMap <> ();
        getInfoAndRawData(peakList, peakInfo, chromatograms);

        if (peakInfo.isEmpty())
            throw new IllegalArgumentException(
                    "Couldn't find peak characteristics (sharpness, "
                    + "signal-to-noise ratio, etc).\nPlease, make sure that "
                    + "the peaks are generated by ADAP-3 Peak Detection");
        
        
        List <Component> components = getComponents(peakInfo, chromatograms);
        List <PeakListRow> newPeakListRows = new ArrayList <> ();

        int rowID = 0;

        for (final Component component : components) 
        {
            System.out.println(Double.toString(component.getMZ()));
            
            if (component.getSpectrum().isEmpty()) continue;
            
            PeakListRow row = new SimplePeakListRow(++rowID);
            
            // Add the reference peak
            PeakListRow refPeakRow = originalPeakList
                    .getRow(component.getBestPeak().getInfo().peakID);
            Feature refPeak = new SimpleFeature(refPeakRow.getBestPeak());

            // Construct spectrum
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
            
            // Construct PeakInformation
            
            SimplePeakInformation information = new SimplePeakInformation(
                    new HashMap <> (refPeakRow.getPeakInformation()
                            .getAllProperties()));
            
            // Save ID if the peak list
            information.addProperty("FROM_PEAKLIST_HASHCODE", 
                    Integer.toString(peakList.hashCode()));
            
            row.setPeakInformation(information);
            
            // Make a comment
            //row.setComment(information.getAllProperties().toString());
            
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
    
//    /**
//     * Reconstruct PeakInfo from PeakIdentity
//     * 
//     * @param peakList
//     * @return 
//     */
//    
//    private List <PeakInfo> getPeakInfo(final PeakList peakList) {
//        
//        List <PeakInfo> result = new ArrayList <> ();
//        
//        for (PeakListRow row : peakList.getRows()) {
//                
//            Feature peak = row.getBestPeak();
//            PeakInformation information = row.getPeakInformation();
//            int[] scanNumbers = peak.getScanNumbers();
//
//            PeakInfo info = new PeakInfo();
//
//            try {
//                // Note: info.peakID is the index of PeakListRow in PeakList.peakListRows (starts from 0)
//                //       row.getID is row.myID (starts from 1)
//                info.peakID = row.getID() - 1;
////                info.peakIndex = Integer.parseInt(
////                        information.getPropertyValue("index"));
//                info.leftApexIndex = scanNumbers[0];
//                info.rightApexIndex = scanNumbers[scanNumbers.length - 1];
//                
//                double height = -Double.MAX_VALUE;
//                
//                for (int scan : scanNumbers) {
//                    double intensity = peak.getDataPoint(scan).getIntensity();
//                    
//                    if (intensity > height) {
//                        height = intensity;
//                        info.peakIndex = scan;
//                    }
//                }
//                
//                info.mzValue = peak.getMZ();
//                info.intensity = peak.getHeight();
//                info.isShared = Boolean.parseBoolean(
//                        information.getPropertyValue("isShared"));
//                info.offset = Integer.parseInt(
//                        information.getPropertyValue("offset"));
//                info.sharpness = Double.parseDouble(
//                        information.getPropertyValue("sharpness"));
//                info.signalToNoiseRatio = Double.parseDouble(
//                        information.getPropertyValue("signalToNoiseRatio"));
//            } catch (Exception e) {
//                LOG.info("Skipping " + row + ": " + e.getMessage());
//                continue;
//            }
//
//            result.add(info);
//        }
//        
//        return result;
//    }
    
    private void getInfoAndRawData(final PeakList peakList,
            List <PeakInfo> peakInfo, 
            Map <PeakInfo, NavigableMap <Double, Double>> chromatograms)
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);
        
        for (PeakListRow row : peakList.getRows()) {
                
            Feature peak = row.getBestPeak();
            PeakInformation information = row.getPeakInformation();
            int[] scanNumbers = peak.getScanNumbers();

            PeakInfo info = new PeakInfo();

            try {
                // Note: info.peakID is the index of PeakListRow in PeakList.peakListRows (starts from 0)
                //       row.getID is row.myID (starts from 1)
                info.peakID = row.getID() - 1;
//                info.peakIndex = Integer.parseInt(
//                        information.getPropertyValue("index"));
                
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
                info.mzValue = peak.getMZ();
                info.intensity = peak.getHeight();
                info.isShared = Boolean.parseBoolean(
                        information.getPropertyValue("isShared"));
                info.offset = Integer.parseInt(
                        information.getPropertyValue("offset"));
                info.sharpness = Double.parseDouble(
                        information.getPropertyValue("sharpness"));
                info.signalToNoiseRatio = Double.parseDouble(
                        information.getPropertyValue("signalToNoiseRatio"));
                info.leftPeakIndex = Integer.parseInt(
                        information.getPropertyValue("leftSharedBoundary"));
                info.rightPeakIndex = Integer.parseInt(
                        information.getPropertyValue("rightSharedBoundary"));
                
            } catch (Exception e) {
                LOG.info("Skipping " + row + ": " + e.getMessage());
                continue;
            }
            
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
            
            chromatograms.put(info, chromatogram);
            peakInfo.add(info);
        }
    }
    
    /**
     * Creates ADAP.RawData object for building chromatograms
     * 
     * @param dataFile RawDataFile object
     * 
     * @param peakList PeakList object
     * 
     * @return RawData object
     */
    
//    private RawData getRawData(final RawDataFile dataFile, 
//            final PeakList peakList) 
//    {
//        
//        
//        return new RawData(
//                ADAPInterface.getRetTimeVector(peakList),
//                ADAPInterface.getIntensityVector(peakList),
//                ADAPInterface.getMZVector(peakList));
//    }
    
    /**
     * Performs ADAP Peak Decomposition
     * 
     * @param peakInfo information on peaks (index, sharpness, etc.)
     * @param windowInfo information on TIC-windows (window range)
     * @param rawData RawData object for building chromatograms
     * @return Collection of dulab.adap.Component objects
     */
    
    private List <Component> getComponents(List <PeakInfo> peakInfo,
            Map <PeakInfo, NavigableMap <Double, Double>> chromatograms) 
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
        params.shapeSimThreshold = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD).getValue();
        params.minModelPeakStN = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_MODEL_STN).getValue();
        params.minModelPeakShapness = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS).getValue();
        params.modelPeakChoice = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MODEL_PEAK_CHOICE).getValue();
        params.deprecatedMZValues = this.parameters.getParameter(
                ADAP3DecompositionV1_5Parameters.MZ_VALUES).getValue();
        
        return TwoStepDecomposition.run(params, peakInfo, chromatograms);
    }
}
