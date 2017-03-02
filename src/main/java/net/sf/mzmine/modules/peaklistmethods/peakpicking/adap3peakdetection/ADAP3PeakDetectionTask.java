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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3peakdetection;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.DeconvolutionTask;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;
import net.sf.mzmine.util.adap.ADAPInterface;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3PeakDetectionTask extends AbstractTask {
    
    private static final Logger LOG = Logger.getLogger(DeconvolutionTask.class
            .getName());
    
    // Peak lists.
    private final MZmineProject project;
    private final PeakList originalPeakList;
    private PeakList newPeakList;

    // User parameters
    private final ParameterSet parameters;
    private final int peakSpan;
    private final int valleySpan;
    private final double edgeToHeightRatio;
    private final double deltaToHeightRatio;
    private final int maxWindowSize;
    private final int maxPeakWidth;
    private final double noiseWindowSize;
    private final double signalToNoiseThreshold;
    
    // Counters.
    private int processedRows;
    private int totalRows;
    
    private RSessionWrapper rSession;
    
    public ADAP3PeakDetectionTask(final MZmineProject project, 
            final PeakList list, 
            final ParameterSet parameterSet) 
    {
        // Initialize.
        this.project = project;
        parameters = parameterSet;
        originalPeakList = list;
        newPeakList = null;
        
        processedRows = 0;
        totalRows = 0;
        
        peakSpan = parameters.getParameter(
                ADAP3PeakDetectionParameters.PEAK_SPAN).getValue();
        valleySpan = parameters.getParameter(
                ADAP3PeakDetectionParameters.VALLEY_SPAN).getValue();
        edgeToHeightRatio = parameters.getParameter(
                ADAP3PeakDetectionParameters.EDGE_TO_HEIGHT_RATIO).getValue();
        deltaToHeightRatio = parameters.getParameter(
                ADAP3PeakDetectionParameters.DELTA_TO_HEIGHT_RATIO).getValue();
        maxWindowSize = parameters.getParameter(
                ADAP3PeakDetectionParameters.MAX_WINDOW_SIZE).getValue();
        maxPeakWidth = parameters.getParameter(
                ADAP3PeakDetectionParameters.MAX_PEAK_WIDTH).getValue();
        noiseWindowSize = parameters.getParameter(
                ADAP3PeakDetectionParameters.NOISE_WINDOW_SIZE).getValue();
        signalToNoiseThreshold = parameters.getParameter(
                ADAP3PeakDetectionParameters.SIGNAL_TO_NOISE_THRESHOLD).getValue();
        
    }
    
    @Override
    public String getTaskDescription() {
        return "Peak recognition on " + originalPeakList;
    }
    
    @Override
    public double getFinishedPercentage() {
        return totalRows == 0 ? 0.0 : (double) processedRows
                / (double) totalRows;
    }
    
    @Override
    public void run() {
        if (!isCanceled()) {
            String errorMsg = null;

            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started ADAP Peak Detection on " + originalPeakList);

            // Check raw data files.
            if (originalPeakList.getNumberOfRawDataFiles() > 1) {

                setStatus(TaskStatus.ERROR);
                setErrorMessage("Peak Detection can only be performed on peak lists with a single raw data file");

            } else {
                
                //String[] rPackages = new String[] {"splus2R", "ifultools", "wmtsa"};
                //String[] rPackageVersions = new String[] {"1.2-1", "2.0-4", "2.0-2"};
                String[] rPackages = new String[] {"adap.peak.detection"};
                String[] rPackageVersions = new String[] {"0.0.1"};
                
                this.rSession = new RSessionWrapper(originalPeakList.getName(),
                        rPackages, rPackageVersions);
                
                try {
                    
                    newPeakList = resolvePeaks(originalPeakList, rSession);
                    
                    
                    if (!isCanceled()) {

                        // Add new peaklist to the project.
                        project.addPeakList(newPeakList);

                        // Add quality parameters to peaks
                        QualityParameters.calculateQualityParameters(newPeakList);

                        // Remove the original peaklist if requested.
                        if (parameters.getParameter(
                                ADAP3PeakDetectionParameters.AUTO_REMOVE)
                                .getValue()) 
                        {
                            project.removePeakList(originalPeakList);
                        }

                        setStatus(TaskStatus.FINISHED);
                        LOG.info("Finished peak recognition on "
                                + originalPeakList);
                    }
                    // Turn off R instance.
                    if (this.rSession != null)
                        this.rSession.close(false);
                    
                } catch (RSessionWrapperException e) {
                    errorMsg = "'R computing error' during peak detection. \n"
                            + e.getMessage();
                } catch (Exception e) {
                    errorMsg = "'Unknown error' during peak detection. \n"
                            + e.getMessage();
                } catch (Throwable t) {

                    setStatus(TaskStatus.ERROR);
                    setErrorMessage(t.getMessage());
                    LOG.log(Level.SEVERE, "Peak deconvolution error", t);
                }

                // Turn off R instance, once task ended UNgracefully.
                try {
                    if (this.rSession != null && !isCanceled())
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

                // Report error.
                if (errorMsg != null) {
                    setErrorMessage(errorMsg);
                    setStatus(TaskStatus.ERROR);
                }
            }
        }
    }
    
    private PeakList resolvePeaks (final PeakList peakList,
            RSessionWrapper rSession) throws RSessionWrapperException 
    {
        final RawDataFile dataFile = peakList.getRawDataFile(0);
        
        // Create new peak list.
        final PeakList resolvedPeakList = new SimplePeakList(peakList + " "
                + parameters.getParameter(ADAP3PeakDetectionParameters.SUFFIX)
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
                        "Peak detection by ADAP-3", parameters));
        
        final Feature[] chromatograms = peakList.getPeaks(dataFile);
        final int chromatogramCount = chromatograms.length;
        
        double[] intensities = ADAPInterface.getIntensityVector(peakList);
        double[] mzValues = ADAPInterface.getMZVector(peakList);
        
        
//        // Save to a file -----------------------------------------------------
//        try {
//            PrintWriter writer = new PrintWriter("intensities.txt");
//            for (double intensity : intensities)
//                writer.println(Double.toString(intensity));
//            writer.close();
//            
//            writer = new PrintWriter("mzvalues.txt");
//            for (double mz : mzValues)
//                writer.println(Double.toString(mz));
//            writer.close();
//            
//        } catch (FileNotFoundException e) {}
//        // --------------------------------------------------------------------
        
        
        if (chromatogramCount == 0)
            throw new IllegalArgumentException("The peak list is empty");
          

        final int[] scans = dataFile.getScanNumbers();
  
        // ------------------------
        // Call R-function getPeaks
        // ------------------------
        
        // Load ADAP-3
        rSession.open();

        // Parameters
        rSession.eval("params <- list()");
        rSession.eval("params$nNode <- 1");
        rSession.eval("params$WorkDir <- './'");
        rSession.eval("params$Peak_span <- " + peakSpan);
        rSession.eval("params$Valley_span <- " + valleySpan);
        rSession.eval("params$EdgeToHeightRatio <- " + edgeToHeightRatio);
        rSession.eval("params$DeltaToHeightRatio <- " + deltaToHeightRatio);
        rSession.eval("params$MaxWindowSize <- " + maxWindowSize);
        rSession.eval("params$MaxPeakWidth <- " + maxPeakWidth);
        rSession.eval("params$NoiseWindowSize <- " + noiseWindowSize);
        rSession.eval("params$SignalToNoiseThreshold <- " + signalToNoiseThreshold);

        // Intensities and mzValues
        //rSession.assign("intVec", intensities);
        rSession.assign("mzVec", mzValues);
        
        // Pass intensities in batches because rSession.assign crashes on
        // passing big arrays
        
        int batchSize = 10000000;
        int start = 0;
        int index = 0;
        String rQuery = "intVec <- c(";
        
        while (start < intensities.length) 
        {
            int length = Math.min(batchSize, intensities.length - start);
            double[] batch = new double[length];
            System.arraycopy(intensities, start, batch, 0, length);
            
            String varName = "intVec" + Integer.toString(index);
            rSession.assign(varName, batch);
            
            rQuery += varName + ",";
            
            start += batchSize;
            index++;
        }
        rQuery = rQuery.substring(0, rQuery.length() - 1) + ")";
        rSession.eval(rQuery);
        
        // Now we are ready to run peakpicking in R
        
        int peakID = 1;
        processedRows = 0;
        totalRows = chromatogramCount;
        
        for (int i = 0; i < chromatogramCount; ++i) 
        {
        // for (int i = 18039; i < 18040; ++i) {
            //List<ResolvedPeak> resolvedPeaks = new ArrayList();
            
            Feature chromatogram = chromatograms[i];
            
//            System.out.println(chromatogram.getMZ());
            
            rSession.assign("mz", chromatogram.getMZ());
            rSession.eval("PeakList <- getPeaks(intVec, params, mz, mzVec)");
//            rSession.eval("write.csv(PeakList, file='peaklist.csv', row.names=FALSE)");
            
            
            // -------------------------
            // Read peak picking results
            // -------------------------
            
            final Object leftApexIndexObject, rightApexIndexObject, 
                    leftPeakIndexObject, rightPeakIndexObject, indexObject, 
                    sharpnessObject, signalToNoiseRatioObject, 
                    isSharedObject, offsetObject;
            
            try {
                leftApexIndexObject = rSession.collect("PeakList$Lbound");
                rightApexIndexObject = rSession.collect("PeakList$Rbound");
                leftPeakIndexObject = rSession.collect("PeakList$lboundInd");
                rightPeakIndexObject = rSession.collect("PeakList$rboundInd");
                indexObject = rSession.collect("PeakList$pkInd");
                sharpnessObject = rSession.collect("PeakList$shrp");
                signalToNoiseRatioObject = rSession.collect("PeakList$StN");
                isSharedObject = rSession.collect("PeakList$isShared");
                offsetObject = rSession.collect("PeakList$offset");
            } catch (Exception e) {
                LOG.info("No peaks found for m/z = " + chromatogram.getMZ());
                ++processedRows;
                continue;
            }
            
            //LOG.info(chromatogram.getMZ() + " : " + leftIndexObject + " " + rightIndexObject);
            
            double[] leftApexIndex = toDoubleArray(leftApexIndexObject);
            double[] rightApexIndex = toDoubleArray(rightApexIndexObject);
            double[] leftPeakIndex = toDoubleArray(leftPeakIndexObject);
            double[] rightPeakIndex = toDoubleArray(rightPeakIndexObject);
            double[] peakIndex = toDoubleArray(indexObject);
            double[] sharpness = toDoubleArray(sharpnessObject);
            double[] signalToNoiseRatio = toDoubleArray(signalToNoiseRatioObject);
            double[] isShared = toDoubleArray(isSharedObject);
            double[] offset = toDoubleArray(offsetObject);
            
            
            if (leftApexIndex == null || rightApexIndex == null || peakIndex == null
                    || sharpness == null || signalToNoiseRatio == null
                    || isShared == null || offset == null) 
            {
                LOG.info("No peaks found for m/z = " + chromatogram.getMZ());
                ++processedRows;
                continue;
            }
            
            int length = java.lang.Integer.min(
                    leftApexIndex.length, rightApexIndex.length);

            // --------------------------
            // Create MZmine peak objects
            // --------------------------
            
            for (int j = 0; j < length; ++j) {
                // Substract 1 because r-indices start from 1
                int left = (int) leftApexIndex[j] - 1;
                int right = (int) rightApexIndex[j] - 1;
                
                if (leftPeakIndex[j] <= 0) leftPeakIndex[j] = left;
                if (rightPeakIndex[j] <= 0) rightPeakIndex[j] = right;
                
                ResolvedPeak peak = new ResolvedPeak(chromatogram, left, right);
                
                SimplePeakInformation information = new SimplePeakInformation();
                
                information.addProperty("index", 
                        //Integer.toString(scans[(int) peakIndex[j] - 1])); // Substract one because R-indices start from 1
                        Integer.toString((int) peakIndex[j]));
                information.addProperty("sharpness", 
                        Double.toString(sharpness[j]));
                information.addProperty("signalToNoiseRatio", 
                        Double.toString(signalToNoiseRatio[j]));
                information.addProperty("isShared", 
                        Boolean.toString(1.0 == isShared[j]));
                information.addProperty("offset", 
                        Integer.toString((int) offset[j]));
                information.addProperty("leftSharedBoundary", 
                        Integer.toString((int) leftPeakIndex[j]));
                information.addProperty("rightSharedBoundary",
                        Integer.toString((int) rightPeakIndex[j]));
                
                SimplePeakListRow row = new SimplePeakListRow(peakID++);
                             
                row.addPeak(dataFile, peak);
                row.setPeakInformation(information);
                
                resolvedPeakList.addRow(row);
            }

            ++processedRows;
        }
        
        return resolvedPeakList;
    }
    
    @Override
    public void cancel() {

        super.cancel();
        // Turn off R instance, if already existing.
        try {
            if (this.rSession != null)
                this.rSession.close(true);
        } catch (RSessionWrapperException e) {
            // Silent, always...
        }
    }
    
    double[] toDoubleArray(Object o) {
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
}
