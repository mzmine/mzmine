package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import net.sf.mzmine.datamodel.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class ADAP3DecompositionV2Utils
{
    private final Logger log;

    public ADAP3DecompositionV2Utils() {
        this.log = Logger.getLogger(ADAP3DecompositionV2Task.class.getName());
    }

    /**
     * Convert MZmine PeakList to a list of ADAP Peaks
     *
     * @param peakList MZmine PeakList object
     * @return list of ADAP Peaks
     */
    @Nonnull
    public List<Peak> getPeaks(@Nonnull final PeakList peakList)
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);

        List <Peak> peaks = new ArrayList<>();

        for (PeakListRow row : peakList.getRows())
        {
            Feature peak = row.getBestPeak();
            int[] scanNumbers = peak.getScanNumbers();

            // Build chromatogram
            NavigableMap<Double, Double> chromatogram = new TreeMap<>();
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
                log.info("Skipping " + row + ": " + e.getMessage());
                continue;
            }

            peaks.add(new Peak(chromatogram, info));
        }

//        return FeatureTools.mergePeaks(peaks);
        return peaks;
    }
}
