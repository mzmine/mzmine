package net.sf.mzmine.modules.tools.msmsspectramerge;

/**
 * A fragment scan consists of a list of MS/MS spectra surrounded by MS1 scans
 */

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.util.scans.ScanUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An MS/MS scan with some statistics about its precursor in MS
 */
class FragmentScan {

    private static final double CHIMERIC_INTENSITY_THRESHOLD = 0.1d;
    /**
     * The raw data file this scans are derived from
     */
    protected final RawDataFile origin;

    /**
     * The feature this scans are derived from
     */
    protected final Feature feature;

    /**
     * mass list to use
     */
    protected final String massList;

    /**
     * the MS1 scan that comes before the first MS/MS
     */
    protected final Integer ms1ScanNumber;
    /**
     * the MS1 scan that comes after the last MS/MS
     */
    protected final Integer ms1SucceedingScanNumber;
    /**
     * all consecutive(!) MS/MS scans. There should ne no other MS1 scan between them
     */
    protected final int[] ms2ScanNumbers;
    /**
     * the intensity of the precursor peak in MS (left or right from MS/MS scans)
     */
    protected double precursorIntensityLeft, precursorIntensityRight;
    /**
     * the sumed up intensity of chimeric peaks (left or right from MS/MS scans)
     */
    protected double chimericIntensityLeft, chimericIntensityRight;

    /**
     * precursor charge of fragment scan
     */
    protected int precursorCharge;
    private PolarityType polarity;


    static FragmentScan[] getAllFragmentScansFor(Feature feature, String massList, Range<Double> isolationWindow, double massAccuracyInPPM) {
        final RawDataFile file = feature.getDataFile();
        final int[] ms2 = feature.getAllMS2FragmentScanNumbers().clone();
        Arrays.sort(ms2);
        final List<FragmentScan> fragmentScans = new ArrayList<>();
        // search for ms1 scans
        int i=0;
        while (i < ms2.length) {
            int scanNumber = ms2[i];
            Scan scan = file.getScan(scanNumber);
            Scan precursorScan = ScanUtils.findPrecursorScan(scan);
            Scan precursorScan2 = ScanUtils.findSucceedingPrecursorScan(scan);
            int j = precursorScan2 == null ? ms2.length : Arrays.binarySearch(ms2, precursorScan2.getScanNumber());
            if (j < 0) j = -j - 1;
            final int[] subms2 = new int[j - i];
            for (int k = i; k < j; ++k) subms2[k - i] = ms2[k];

            fragmentScans.add(new FragmentScan(
                    file,
                    feature,
                    massList,
                    precursorScan != null ? precursorScan.getScanNumber() : null,
                    precursorScan2 != null ? precursorScan2.getScanNumber() : null,
                    subms2,
                    isolationWindow,
                    massAccuracyInPPM
            ));
            i = j;
        }
        return fragmentScans.toArray(new FragmentScan[0]);
    }

    private FragmentScan(RawDataFile origin, Feature feature, String massList, Integer ms1ScanNumber, Integer ms1ScanNumber2, int[] ms2ScanNumbers, Range<Double> isolationWindow, double massAccuracyInPPM) {
        this.origin = origin;
        this.feature = feature;
        this.massList = massList;
        this.ms1ScanNumber = ms1ScanNumber;
        this.ms1SucceedingScanNumber = ms1ScanNumber2;
        this.ms2ScanNumbers = ms2ScanNumbers;
        double[] precInfo = new double[2];
        detectPrecursor(ms1ScanNumber, feature.getMZ(), isolationWindow, massAccuracyInPPM, precInfo);
        this.precursorIntensityLeft = precInfo[0];
        this.chimericIntensityLeft = precInfo[1];
        detectPrecursor(ms1SucceedingScanNumber, feature.getMZ(), isolationWindow, massAccuracyInPPM, precInfo);
        this.precursorIntensityRight = precInfo[0];
        this.chimericIntensityRight = precInfo[1];
    }

    /**
     * interpolate the precursor intensity and chimeric intensity of the MS1 scans linearly by retention time to
     * estimate this values for the MS2 scans
     * @return two arrays, one for precursor intensities, one for chimeric intensities, for all MS2 scans
     */
    protected double[][] getInterpolatedPrecursorAndChimericIntensities() {
        Scan left = origin.getScan(ms1ScanNumber);
        Scan right = origin.getScan(ms1SucceedingScanNumber);
        final double[][] values = new double[2][ms2ScanNumbers.length];
        for (int k=0; k < ms2ScanNumbers.length; ++k) {
            Scan ms2 = origin.getScan(ms2ScanNumbers[k]);
            double rtRange = (ms2.getRetentionTime() - left.getRetentionTime())/(right.getRetentionTime()-left.getRetentionTime());
            if (rtRange >= 0 && rtRange <= 1) {
                values[0][k] = (1d-rtRange) * precursorIntensityLeft + (rtRange)*precursorIntensityRight;
                values[0][k] = (1d-rtRange) * chimericIntensityLeft + (rtRange)*chimericIntensityRight;
            } else {
                LoggerFactory.getLogger(FragmentScan.class).warn("Retention time is non-monotonic within scan numbers.");
                values[0][k] = precursorIntensityLeft;
                values[1][k] = chimericIntensityLeft+chimericIntensityRight;
            }
        }
        return values;
    }

    /**
     * search for precursor peak in MS1
     */
    private void detectPrecursor(int ms1Scan, double precursorMass, Range<Double> isolationWindow, double ppm, double[] precInfo) {
        Scan spectrum = origin.getScan(ms1Scan);
        this.precursorCharge = spectrum.getPrecursorCharge();
        this.polarity = spectrum.getPolarity();
        DataPoint[] dps = spectrum.getDataPointsByMass(Range.closed(precursorMass+isolationWindow.lowerEndpoint(), precursorMass+isolationWindow.upperEndpoint()));
        // for simplicity, just use the most intensive peak within ppm range
        int bestPeak = -1;
        double highestIntensity = 0d;
        for (int mppm = 1; mppm < 3; ++mppm) {
            final double maxDiff = (mppm * ppm) * 1e-6 * Math.max(200, precursorMass);
            for (int i = 0; i < dps.length; ++i) {
                final DataPoint p = dps[i];
                if (p.getIntensity() <= highestIntensity)
                    continue;
                final double mzdiff = Math.abs(p.getMZ() - precursorMass);
                if (mzdiff <= maxDiff) {
                    highestIntensity = p.getIntensity();
                    bestPeak = i;
                }
            }
            if (bestPeak >= 0)
                break;
        }
        // now sum up all remaining intensities. Leave out isotopes. leave out peaks with intensity below 10%
        // of the precursor. They won't contaminate fragment scans anyways
        precInfo[0] = highestIntensity;
        precInfo[1] = 0d;
        final double threshold = highestIntensity* CHIMERIC_INTENSITY_THRESHOLD;
        foreachpeak:
        for (int i = 0; i < dps.length; ++i) {
            if (i != bestPeak && dps[i].getIntensity()>threshold) {
                // check for isotope peak
                final double maxDiff = ppm * 1e-6 * Math.max(200, precursorMass) + 0.03;
                for (int k = 1; k < 5; ++k) {
                    final double isoMz = precursorMass + k * 1.0015;
                    final double diff = isoMz - dps[i].getMZ();
                    if (Math.abs(diff) <= maxDiff) {
                        continue foreachpeak;
                    } else if (diff > 0.5) {
                        break;
                    }
                }
                precInfo[1] += dps[i].getIntensity();
            }
        }
    }
}