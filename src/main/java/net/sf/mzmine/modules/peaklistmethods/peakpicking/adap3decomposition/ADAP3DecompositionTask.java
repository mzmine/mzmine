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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;


import dulab.adap.common.algorithms.machineleanring.OptimizationParameters;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.datamodel.RawData;
import dulab.adap.datamodel.Sample;
import dulab.adap.datamodel.WindowInfo;
import dulab.adap.workflow.DecompositionParameters;
import java.io.FileNotFoundException;
        
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;
import net.sf.mzmine.util.adap.ADAPInterface;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionTask extends AbstractTask {
    
     // Logger.
    private static final Logger LOG = Logger.getLogger(
            ADAP3DecompositionTask.class.getName());
    
    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;
    
    final private Sample sample;
    
    // Counters.
    private double rawDataProcessed;
    private double ticProcessed;
    private double decompositionProcessed;
    

    // User parameters
    private final ParameterSet parameters;
    
    private RSessionWrapper rSession;
    
    class WindowIndices {
        int[] left;
        int[] right;
    }
    
    public ADAP3DecompositionTask(final MZmineProject project, final PeakList list,
            final ParameterSet parameterSet) {

        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        rawDataProcessed = 0.0;
        ticProcessed = 0.0;
        decompositionProcessed = 0.0;
        
        sample = new Sample();
    }
    
    @Override
    public String getTaskDescription() {
        return "ADAP3 Peak decomposition on " + originalPeakList;
    }
    
    @Override
    public double getFinishedPercentage() {
        decompositionProcessed = sample.getProcessedPercent();
        
        return 0.333 * rawDataProcessed + 
                0.333 * ticProcessed +
                0.333 * decompositionProcessed;
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
                
                /*String[] rPackages = new String[] {"splus2R", "ifultools", "wmtsa"};
                String[] rPackageVersions = new String[] {"1.2-1", "2.0-4", "2.0-2"};

                this.rSession = new RSessionWrapper(originalPeakList.getName(), 
                        rPackages, rPackageVersions);
                */
                try {
                    
                    newPeakList = decomposePeaks(originalPeakList);  
                    
                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        project.addPeakList(newPeakList);

                        // Add quality parameters to peaks
                        QualityParameters.calculateQualityParameters(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(
                                ADAP3DecompositionParameters.AUTO_REMOVE).getValue()) 
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
                    errorMsg = "Incorrect Peak List selected. \n"
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
    
    private List <WindowInfo> ticPeakDetection(RawDataFile dataFile) {
        List <WindowInfo> result = new ArrayList();
        
        ParameterSet ticParameters = this.parameters.getParameter(
                ADAP3DecompositionParameters.TIC_WINDOW).getValue();
        final double peakSpan = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.PEAK_SPAN).getValue();
        final double valleySpan = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.VALLEY_SPAN).getValue();
        final double edgeToHeightRatio = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.EDGE_TO_HEIGHT_RATIO).getValue();
        final double deltaToHeightRatio = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.DELTA_TO_HEIGHT_RATIO).getValue();
        final int maxWindowSize = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.MAX_WINDOW_SIZE).getValue();
        final int maxPeakWidth = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.MAX_PEAK_WIDTH).getValue();
        final double noiseWindowSize = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.NOISE_WINDOW_SIZE).getValue();
        final double signalToNoiseThreshold = ticParameters.getParameter(
                ADAP3TICWindowDetectionParameters.SIGNAL_TO_NOISE_THRESHOLD).getValue();
        
        Scan selectedScans[] = new ScanSelection(1).getMatchingScans(dataFile);
        int scanCount = selectedScans.length;
        
        if (scanCount == 0)
            throw new IllegalArgumentException("No peaks found");
        
        double[] intensities = new double[scanCount];
        for (int i = 0; i < scanCount; ++i)
            intensities[i] = selectedScans[i].getTIC();
        
        // Save to a file -----------------------------------------------------
        try {
            PrintWriter writer = new PrintWriter("ticintensities.txt");
            for (double intensity : intensities)
                writer.println(Double.toString(intensity));
            writer.close();
            
        } catch (FileNotFoundException e) {}
        // --------------------------------------------------------------------
        
        //String[] rPackages = new String[] {"splus2R", "ifultools", "wmtsa"};
        //String[] rPackageVersions = new String[] {"1.2-1", "2.0-4", "2.0-2"};
        String[] rPackages = new String[] {"adap.peak.detection"};
        String[] rPackageVersions = new String[] {"0.0.1"};

        RSessionWrapper rSession = new RSessionWrapper(originalPeakList.getName(), 
                rPackages, rPackageVersions);

        String errorMsg = null;
        
        try {
            rSession.open();

            // Load ADAP-3
            //String rPath = "/Users/aleksandrsmirnov/Projects/adap-gc_3";
            //rSession.assign("codeDir", rPath);
            //rSession.eval("source(paste('" + rPath + "', 'pipeline.r', sep='/'))");

            // Parameters
            rSession.eval("params <- list()");
            rSession.eval("params$nNode <- 1");
            //rSession.eval("params$WorkDir <- './'");
            rSession.eval("params$Peak_span <- " + peakSpan);
            rSession.eval("params$Valley_span <- " + valleySpan);
            rSession.eval("params$EdgeToHeightRatio <- " + edgeToHeightRatio);
            rSession.eval("params$DeltaToHeightRatio <- " + deltaToHeightRatio);
            rSession.eval("params$MaxWindowSize <- " + maxWindowSize);
            rSession.eval("params$MaxPeakWidth <- " + maxPeakWidth);
            rSession.eval("params$NoiseWindowSize <- " + noiseWindowSize);
            rSession.eval("params$SignalToNoiseThreshold <- " + signalToNoiseThreshold);

            // Intensities
            rSession.assign("vecInt", intensities);

            rSession.eval("PeakList <- getPeaks(vecInt, params)");

            final double[] leftIndex = toDoubleArray(rSession
                    .collect("PeakList$lboundInd"));

            final double[] rightIndex = toDoubleArray(rSession
                    .collect("PeakList$rboundInd"));

            if (leftIndex == null || rightIndex == null) return result;

            int length = java.lang.Integer.min(
                    leftIndex.length, rightIndex.length);

            double prev_leftIndex = 0, prev_rightIndex = 0;

            for (int i = 0; i < length; ++i) {

                if (leftIndex[i] != prev_leftIndex
                        || rightIndex[i] != prev_rightIndex)
                {
                    WindowInfo info = new WindowInfo();

                    info.leftBoundIndex = (int) leftIndex[i];
                    info.rightBoundIndex = (int) rightIndex[i];

                    result.add(info);

                    prev_leftIndex = leftIndex[i];
                    prev_rightIndex = rightIndex[i];
                }
            }
        } catch (RSessionWrapperException e) {
            errorMsg = "'R computing error' during peak decomposition. \n"
                    + e.getMessage();
        }

        // Turn off R instance, once task ended UNgracefully.
        if (this.rSession != null && !isCanceled()) {
            try {
                rSession.close(isCanceled());
            } catch (RSessionWrapperException e) {
                if (!isCanceled()) {
                    // Do not override potential previous error message.
                    if (errorMsg == null) {
                        errorMsg = e.getMessage();
                    }
                } else {
                    // User canceled: Silent.
                }
            }
        }

        // Report error.
        if (errorMsg != null) {
            setErrorMessage(errorMsg);
            setStatus(TaskStatus.ERROR);
        }

        return result;
    }
    
    PeakList decomposePeaks(PeakList peakList) 
            throws CloneNotSupportedException, IOException
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);
        
        // Create new peak list.
        final PeakList resolvedPeakList = new SimplePeakList(peakList + " "
                + parameters.getParameter(ADAP3DecompositionParameters.SUFFIX)
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
        //List <PeakInfo> peakInfo = getPeakInfo(peakList);

        if (peakInfo.isEmpty())
            throw new IllegalArgumentException(
                    "Couldn't find peak characteristics (sharpness, "
                    + "signal-to-noise ratio, etc).\nPlease, make sure that "
                    + "the peaks are generated by ADAP-3 Peak Detection");
        
        // Get Raw Data object for building chromatograms
        // RawData rawData = getRawData(dataFile, chromatogramList);

        // Find TIC windows
        List <WindowInfo> windowInfo = ticPeakDetection(dataFile);
        
        ticProcessed += 1.0;
        
        List <Component> components = getComponents(peakInfo, windowInfo,
                chromatograms);

        int rowID = 0;

        for (final Component component : components) 
        {
            System.out.println(Double.toString(component.getMZ()));
            
            if (component.getSpectrum().isEmpty()) continue;
            
            PeakListRow row = new SimplePeakListRow(++rowID);
            
            // Add the reference peak
            PeakListRow refPeakRow = originalPeakList
                    .getRow(component.getBestPeak().getInfo().peakID);
            Feature refPeak = refPeakRow.getBestPeak();

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
            
            /*
            // Save peak indices
            NavigableMap <Double, Integer> peakIndices =
                    new TreeMap <> ();
            for (final Peak peak : component.getPeaks())
                peakIndices.put(peak.getMZ(), peak.getInfo().peakID);
            information.addProperty("ADAP_PEAK_IDS", peakIndices.toString());
            */
            
            // Save ID if the peak list
            information.addProperty("FROM_PEAKLIST_HASHCODE", 
                    Integer.toString(peakList.hashCode()));
            
            row.setPeakInformation(information);
            
            // Make a comment
            //row.setComment(information.getAllProperties().toString());
            
            // Set row properties
            row.setAverageMZ(refPeakRow.getAverageMZ());
            row.setAverageRT(refPeakRow.getAverageRT());
             
            resolvedPeakList.addRow(row);
        }
        
        return resolvedPeakList;
    }
    
    private double[] toDoubleArray(Object o) {
        double[] result = null;
        
        if (o instanceof double[])
                result = (double[]) o;
        
        else if (o instanceof Double) 
        {
            result = new double[1];
            result[0] = (double) o;
        }
        
        return result;
    }
    
    /**
     * Reconstruct PeakInfo from PeakIdentity
     * 
     * @param peakList
     * @return 
     */
    
    private List <PeakInfo> getPeakInfo(final PeakList peakList) {
        
        List <PeakInfo> result = new ArrayList <> ();
        
        for (PeakListRow row : peakList.getRows()) {
                
            Feature peak = row.getBestPeak();
            PeakInformation information = row.getPeakInformation();
            int[] scanNumbers = peak.getScanNumbers();

            PeakInfo info = new PeakInfo();

            try {
                // Note: info.peakID is the index of PeakListRow in PeakList.peakListRows (starts from 0)
                //       row.getID is row.myID (starts from 1)
                info.peakID = row.getID() - 1;
                info.peakIndex = Integer.parseInt(
                        information.getPropertyValue("index"));
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
            } catch (Exception e) {
                LOG.info("Skipping " + row + ": " + e.getMessage());
                continue;
            }

            result.add(info);
        }
        
        return result;
    }
    
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
                info.peakIndex = Integer.parseInt(
                        information.getPropertyValue("index"));
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
    
    private RawData getRawData(final RawDataFile dataFile, 
            final PeakList peakList) 
    {
        
        
        return new RawData(
                ADAPInterface.getRetTimeVector(peakList),
                ADAPInterface.getIntensityVector(peakList),
                ADAPInterface.getMZVector(peakList));
    }
    
    /**
     * Performs ADAP Peak Decomposition
     * 
     * @param peakInfo information on peaks (index, sharpness, etc.)
     * @param windowInfo information on TIC-windows (window range)
     * @param rawData RawData object for building chromatograms
     * @return Collection of dulab.adap.Component objects
     */
    
    private List <Component> getComponents(List <PeakInfo> peakInfo,
            List <WindowInfo> windowInfo, 
            Map <PeakInfo, NavigableMap <Double, Double>> chromatograms) 
    {
        // -----------------------------
        // ADAP Decomposition Parameters
        // -----------------------------
        
        ParameterSet paramSet = this.parameters.getParameter(
                ADAP3DecompositionParameters.EIC_DECOMPOSITION).getValue();
        
        DecompositionParameters params = new DecompositionParameters();
        params.maxRetTimeClusterWidth = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MAX_RT_CLUSTER_WIDTH).getValue();
        params.maxShapeClusterWidth = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MAX_SHAPE_CLUSTER_WIDTH).getValue();
        params.minClusterIntensity = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MIN_CLUSTER_INTENSITY).getValue();
        params.minClusterSize = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MIN_CLUSTER_SIZE).getValue();
        params.minWindowSize = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MIN_WINDOW_SIZE).getValue();
        params.minModelPeakShapness = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MIN_MODEL_SHARPNESS).getValue();
        params.minModelPeakStN = paramSet.getParameter(
                ADAP3EICDecompositionParameters.MIN_MODEL_STN).getValue();
        params.retTimeTolerance = paramSet.getParameter(
                ADAP3EICDecompositionParameters.RT_TOLERANCE).getValue();
        
        // ----------------------------
        // ADAP Optimization Parameters
        // ----------------------------
        
        paramSet = this.parameters.getParameter(
                ADAP3DecompositionParameters.OPTIMIZATION).getValue();
        
        OptimizationParameters optParams = new OptimizationParameters();
        optParams.gradientTolerance = paramSet.getParameter(
                ADAP3OptimizationParameters.GRADIENT_TOLERANCE).getValue();
        optParams.costTolerance = paramSet.getParameter(
                ADAP3OptimizationParameters.COST_TOLERANCE).getValue();
        optParams.alpha = paramSet.getParameter(
                ADAP3OptimizationParameters.ALPHA).getValue();
        optParams.maxIterationCount = paramSet.getParameter(
                ADAP3OptimizationParameters.MAX_ITERATION).getValue();
        optParams.verbose = paramSet.getParameter(
                ADAP3OptimizationParameters.VERBOSE).getValue();
        
        params.optimizationParams = optParams;
        
        // -------------
        // Decomposition
        // -------------
        
        sample.addPeaks(params, peakInfo, windowInfo, chromatograms);

        return sample.getComponents();
    }
}
