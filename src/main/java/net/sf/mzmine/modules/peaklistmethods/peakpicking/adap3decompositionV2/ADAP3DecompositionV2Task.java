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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import com.google.common.collect.Range;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
        
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import dulab.adap.workflow.decomposition.Decomposition;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.datamodel.impl.*;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5.ADAP3DecompositionV1_5Parameters;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import javax.annotation.Nonnull;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV2Task extends AbstractTask {
    
     // Logger.
    private static final Logger LOG = Logger.getLogger(ADAP3DecompositionV2Task.class.getName());
    
    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;
    private final Decomposition decomposition;
    
    // User parameters
    private final ParameterSet parameters;
    
    ADAP3DecompositionV2Task(final MZmineProject project, final PeakList list,
                             final ParameterSet parameterSet)
    {
        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        decomposition = new Decomposition();
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
        List <Peak> peaks = new ADAP3DecompositionV2Utils().getPeaks(peakList);

        // Find components (a.k.a. clusters of peaks with fragmentation spectra)
        List <Component> components = getComponents(peaks);

        // Create PeakListRow for each components
        List <PeakListRow> newPeakListRows = new ArrayList <> ();

        int rowID = 0;

        for (final Component component : components) 
        {
            if (component.getSpectrum().isEmpty()) continue;
            
            PeakListRow row = new SimplePeakListRow(++rowID);

            // Create a reference peal
            Feature refPeak = getFeature(dataFile, component.getBestPeak());
//            // Add the reference peak
//            PeakListRow refPeakRow = originalPeakList
//                    .getRow(component.getBestPeak().getInfo().peakID);
//            Feature refPeak = new SimpleFeature(refPeakRow.getBestPeak());

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
//            SimplePeakInformation information = new SimplePeakInformation(
//                    new HashMap<>(refPeakRow.getPeakInformation().getAllProperties()));
//            row.setPeakInformation(information);
            
//            // Set row properties
//            row.setAverageMZ(refPeakRow.getAverageMZ());
//            row.setAverageRT(refPeakRow.getAverageRT());

            // Set row properties
            row.setAverageMZ(refPeak.getMZ());
            row.setAverageRT(refPeak.getRT());
             
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

        Decomposition.Parameters params = new Decomposition.Parameters();

        params.minClusterDistance = this.parameters.getParameter(
                ADAP3DecompositionV2Parameters.MIN_CLUSTER_DISTANCE).getValue();
        params.minClusterSize = this.parameters.getParameter(
                ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE).getValue();
        params.fwhmTolerance = this.parameters.getParameter(
                ADAP3DecompositionV2Parameters.FWHM_TOLERANCE).getValue();
        params.shapeTolerance = this.parameters.getParameter(
                ADAP3DecompositionV2Parameters.SHAPE_TOLERANCE).getValue();
        
        return decomposition.run(params, peaks);
    }

    @Nonnull
    private Feature getFeature(@Nonnull RawDataFile file, @Nonnull Peak peak)
    {
        NavigableMap<Double, Double> chromatogram = peak.getChromatogram();

        // Retrieve scan numbers
        int representativeScan = 0;
        int[] scanNumbers = new int[chromatogram.size()];
        int count = 0;
        for (int num : file.getScanNumbers())
        {
            double retTime = file.getScan(num).getRetentionTime();
            if (chromatogram.keySet().contains(retTime))
                scanNumbers[count++] = num;
            if (retTime == peak.getRetTime())
                representativeScan = num;
        }

        // Calculate peak area
        double area = 0.0;
        Iterator<Map.Entry<Double, Double>> it = chromatogram.entrySet().iterator();
        if (it.hasNext())
        {
            Map.Entry<Double, Double> e1 = it.next();
            Map.Entry<Double, Double> e2;
            while (it.hasNext()) {
                e2 = it.next();
                double base = e2.getKey() - e1.getKey();
                double height = 0.5 * (e1.getValue() + e2.getValue());
                area += base * height;
            }
        }

        // Create array of DataPoints
        DataPoint[] dataPoints = new DataPoint[chromatogram.size()];
        count = 0;
        for (double intensity : chromatogram.values())
            dataPoints[count++] = new SimpleDataPoint(peak.getMZ(), intensity);

        // Find retention time range
        double minRetTime = Double.MAX_VALUE;
        double maxRetTime = -Double.MAX_VALUE;
        for (double retTime : chromatogram.keySet()) {
            if (retTime > maxRetTime) maxRetTime = retTime;
            if (retTime < minRetTime) minRetTime = retTime;
        }

        return new SimpleFeature(file, peak.getMZ(), peak.getRetTime(), peak.getIntensity(),
                area, scanNumbers, dataPoints,
                Feature.FeatureStatus.MANUAL, representativeScan, representativeScan,
                Range.closed(peak.getStartRetTime(), peak.getEndRetTime()),
                Range.closed(peak.getMZ() - 0.01, peak.getMZ() + 0.01),
                Range.closed(0.0, peak.getIntensity()));
    }
}
