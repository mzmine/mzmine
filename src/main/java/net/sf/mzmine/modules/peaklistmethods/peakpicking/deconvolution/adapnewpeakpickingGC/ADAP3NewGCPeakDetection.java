/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC;

//import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.INTEGRATION_METHOD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.PEAK_DURATION;
//import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PEAK_SCALES;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.SN_THRESHOLD;
//import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAP3NewGCPeakDetectionParameters.SHARP_THRESHOLD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.MIN_FEAT_HEIGHT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
//import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetectorParameters.PeakIntegrationMethod;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.google.common.collect.Range;

import dulab.adap.datamodel.PeakInfo;
import static dulab.adap.workflow.DeconvolutionGC.DeconvoluteSignal;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAPDetectorParameters.RT_FOR_CWT_SCALES_DURATION;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.COEF_AREA_THRESHOLD;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.EDGE_TO_HEIGHT_RATIO;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.DELTA_TO_HEIGHT_RATIO;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.adapnewpeakpickingGC.ADAP3NewGCPeakDetectionParameters.MAX_WINDOW_SIZE;


/**
 * Use XCMS findPeaks.centWave to identify peaks.
 */
public class ADAP3NewGCPeakDetection implements PeakResolver {
    
    // Logger.
    private static final Logger LOG = Logger.getLogger(ADAP3NewGCPeakDetection.class
            .getName());

    // Name.
    private static final String NAME = "Wavelets (ADAP GC)";

    // Minutes <-> seconds.
    private static final double SECONDS_PER_MINUTE = 60.0;


    @Nonnull
    @Override
    public String getName() {

        return NAME;
    }

    @Nonnull
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {

        return ADAP3NewGCPeakDetectionParameters.class;
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return null;
    }
        @Override
    public boolean getRequiresR() {
        return false;
    }

    @Override
    public String[] getRequiredRPackages() {
        return null;
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            final ParameterSet parameters,
            RSessionWrapper rSession) throws RSessionWrapperException {
        
        int scanNumbers[] = chromatogram.getScanNumbers();
        final int scanCount = scanNumbers.length;
        double retentionTimes[] = new double[scanCount];
        double intensities[] = new double[scanCount];
        RawDataFile dataFile = chromatogram.getDataFile();
        for (int i = 0; i < scanCount; i++) {
            final int scanNum = scanNumbers[i];
            retentionTimes[i] = dataFile.getScan(scanNum).getRetentionTime();
            DataPoint dp = chromatogram.getDataPoint(scanNum);
            if (dp != null)
                intensities[i] = dp.getIntensity();
            else
                intensities[i] = 0.0;
        }
        
        //List<PeakInfo> ADAPPeaks = new ArrayList<PeakInfo>();
        List<PeakInfo> ADAPPeaks = null;

        
        Range<Double> peakDuration = parameters.getParameter(
        PEAK_DURATION).getValue();

        double signalToNoiseThreshold = parameters.getParameter(SN_THRESHOLD).getValue();
        double minFeatureHeight= parameters.getParameter(MIN_FEAT_HEIGHT).getValue();
        double coefAreaThresh = parameters.getParameter(COEF_AREA_THRESHOLD).getValue();
        double edgeToHeightRatio = parameters.getParameter(EDGE_TO_HEIGHT_RATIO).getValue();
        double deltaToHeightRatio = parameters.getParameter(DELTA_TO_HEIGHT_RATIO).getValue();
        double maxWindowSize = parameters.getParameter(MAX_WINDOW_SIZE).getValue();
        
        // get the average rt spacing
        double rtSum = 0.0;
        for (int i =0; i< retentionTimes.length-1; i++){
            rtSum += retentionTimes[i+1]-retentionTimes[i];
        }
        double avgRTInterval = rtSum/((double) (retentionTimes.length-1));
        // Change the lower and uper bounds for the wavelet scales from retention times to number of scans.
        Range<Double> rtRangeForCWTScales = parameters.getParameter(
        RT_FOR_CWT_SCALES_DURATION).getValue();
        double rtLow = rtRangeForCWTScales.lowerEndpoint();
        double rtHigh = rtRangeForCWTScales.upperEndpoint();
        int numScansRTLow = (int) Math.round(rtLow/avgRTInterval);
        int numScansRTHigh= (int) Math.round(rtHigh/avgRTInterval);
        
        if(numScansRTLow<1){
            numScansRTLow = 1;}
        if(numScansRTHigh>=retentionTimes.length){
            numScansRTHigh = retentionTimes.length;
        }
  
        ADAPPeaks = DeconvoluteSignal( retentionTimes, intensities,
                chromatogram.getMZ(), 
                signalToNoiseThreshold, 
                minFeatureHeight,
                peakDuration,
                coefAreaThresh,
                edgeToHeightRatio,
                deltaToHeightRatio,
                maxWindowSize,
                numScansRTLow,
                numScansRTHigh);

        final List<ResolvedPeak> resolvedPeaks;

        if (ADAPPeaks == null){
            resolvedPeaks = new ArrayList<ResolvedPeak>(0);

        } else {

            LOG.finest("Processing peak matrix...");



            // Process peak matrix.
            resolvedPeaks = new ArrayList<ResolvedPeak>(ADAPPeaks.size());
            
            

            // The old way could detect the same peak more than once if the wavlet scales were too large.
            // If the left bounds were the same and there was a null point before the right bounds it would
            //make the same peak twice.
            // To avoid the above see if the peak duration range is met before going into
            // the loop
            
            //for (final double[] peakRow : peakMatrix) {
            for (int i = 0; i < ADAPPeaks.size();i++) {

                PeakInfo curPeak = ADAPPeaks.get(i);
                
                SimplePeakInformation information = new SimplePeakInformation();
                information.addProperty("index", 
                        //Integer.toString(scans[(int) peakIndex[j] - 1])); // Substract one because r-indices start from 1
                        Integer.toString((int) curPeak.peakIndex));
                information.addProperty("sharpness", 
                        Double.toString(curPeak.sharpness));
                information.addProperty("signalToNoiseRatio", 
                        Double.toString(curPeak.signalToNoiseRatio));
                information.addProperty("isShared", 
                        Boolean.toString(curPeak.isShared));
                        //Boolean.toString(1.0 == curPeak.isShared));
                information.addProperty("offset", 
                        Integer.toString((int) curPeak.offset));
                information.addProperty("coeffOverArea", 
                        Double.toString((double) curPeak.coeffOverArea));
                

                
                ResolvedPeak peak = new ResolvedPeak(chromatogram, curPeak.leftApexIndex, curPeak.rightApexIndex);
                peak.setPeakInformation(information);

                


                resolvedPeaks.add(peak);
                //resolvedPeaks.add(new ResolvedPeak(chromatogram,curPeak.leftApexIndex, curPeak.rightApexIndex));


            }
        }

        return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
    }
}
