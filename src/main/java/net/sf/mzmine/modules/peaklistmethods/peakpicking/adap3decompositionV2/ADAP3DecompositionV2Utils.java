package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import net.sf.mzmine.datamodel.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import java.util.logging.Logger;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class ADAP3DecompositionV2Utils
{
    private final Logger log;
    
    private final Map<Integer, Double> retTimes;

    public ADAP3DecompositionV2Utils() {
        this.log = Logger.getLogger(ADAP3DecompositionV2Task.class.getName());
        this.retTimes = new HashMap<>();
    }

    /**
     * Convert MZmine PeakList to a list of ADAP Peaks
     *
     * @param peakList MZmine PeakList object
     * @return list of ADAP Peaks
     */
    @Nonnull
    public List<BetterPeak> getPeaks(@Nonnull final PeakList peakList)
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);

        List<BetterPeak> peaks = new ArrayList<>();
        
        for (PeakListRow row : peakList.getRows())
        {
            Feature peak = row.getBestPeak();
            int[] scanNumbers = peak.getScanNumbers();


            // Build chromatogram
            double[] retTimes = new double[scanNumbers.length];
            double[] intensities = new double[scanNumbers.length];
            for (int i = 0; i < scanNumbers.length; ++i) {
                int scan = scanNumbers[i];
                retTimes[i] = getRetTime(dataFile, scan);
                DataPoint dataPoint = peak.getDataPoint(scan);
                if (dataPoint != null)
                    intensities[i] = dataPoint.getIntensity();
            }
            Chromatogram chromatogram = new Chromatogram(retTimes, intensities);

//            // Build chromatogram
//            NavigableMap<Double, Double> chromatogram = new TreeMap<>();
//            for (int scanNumber : scanNumbers) {
//                DataPoint dataPoint = peak.getDataPoint(scanNumber);
//                if (dataPoint != null)
//                    chromatogram.put(
//                            getRetTime(dataFile, scanNumber),
//                            dataPoint.getIntensity());
//            }

            if (chromatogram.length <= 1) continue;

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
                log.info("Skipping " + row + ": " + e.getMessage());
                continue;
            }

            peaks.add(new BetterPeak(chromatogram, info));
        }

//        return FeatureTools.mergePeaks(peaks);
        return peaks;
    }
    
    private double getRetTime(RawDataFile dataFile, int scan) {
        Double retTime = retTimes.get(scan);
        if (retTime == null) {
            retTime = dataFile.getScan(scan).getRetentionTime();
            retTimes.put(scan, retTime);
        }
        return retTime;
    }
}
