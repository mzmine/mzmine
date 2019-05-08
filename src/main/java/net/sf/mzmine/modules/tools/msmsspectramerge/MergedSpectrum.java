package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

import java.util.Arrays;
import java.util.Set;

public class MergedSpectrum {
    public final MergedDataPoint[] data;
    public final RawDataFile[] origins;
    public final int[] scanIds;
    public final PolarityType polarity;
    public final int precursorCharge;
    public final double precursorMz;
    protected double bestFragmentScanScore;
    public int removedScansByLowQuality;
    public int removedScansByLowCosine;
    private final static MergedSpectrum EMPTY = new MergedSpectrum(new MergedDataPoint[0],new RawDataFile[0],new int[0],0d, PolarityType.UNKNOWN,0, 0,0, 0d);
    public static MergedSpectrum empty() {
        return EMPTY;
    }

    public MergedSpectrum(MergedDataPoint[] data, Set<RawDataFile> origins, Set<Integer> scanIds, double precursorMz, PolarityType polarity, int precursorCharge, int removedScansByLowQuality, int removedScansByLowCosine) {
        this(data, origins.toArray(new RawDataFile[origins.size()]), scanIds.stream().sorted().mapToInt(x->x).toArray(), precursorMz, polarity,precursorCharge, removedScansByLowQuality, removedScansByLowCosine, 0d);
    }

    public MergedSpectrum(Scan single, String massList) {
        DataPoint[] dataPoints = single.getMassList(massList).getDataPoints();
        this.data = new MergedDataPoint[dataPoints.length];
        for (int k=0; k < dataPoints.length; ++k) {
            this.data[k] = new MergedDataPoint(MzMergeMode.MOST_INTENSIVE, IntensityMergeMode.MAXIMUM, dataPoints[k]);
        }
        this.origins = new RawDataFile[]{single.getDataFile()};
        this.scanIds = new int[single.getScanNumber()];
        this.polarity = single.getPolarity();
        this.precursorCharge = single.getPrecursorCharge();
        this.removedScansByLowCosine = 0;
        this.removedScansByLowQuality = 0;
        this.precursorMz = single.getPrecursorMZ();
    }

    public MergedSpectrum(MergedDataPoint[] data, RawDataFile[] origins, int[] scanIds, double precursorMz, PolarityType polarity, int precursorCharge, int removedScansByLowQuality, int removedScansByLowCosine, double bestFragmentScanScore) {
        this.data = data;
        this.origins = origins;
        this.scanIds = scanIds;
        this.bestFragmentScanScore = bestFragmentScanScore;
        this.precursorMz = precursorMz;
        this.removedScansByLowQuality = removedScansByLowQuality;
        this.removedScansByLowCosine = removedScansByLowCosine;
        this.polarity = polarity;
        this.precursorCharge = precursorCharge;
    }

    public String toString() {
        if (origins.length>0) {
            final String name = origins[0].getName();
            if (origins.length>1) {
                return "Merged spectrum from " + name + " and " + (origins.length-1) + " others";
            } else {
                return "Merged spectrum from " + name;
            }
        } else return "merged spectrum";
    }

    public int totalNumberOfScans() {
        return scanIds.length+removedScansByLowCosine+removedScansByLowQuality;
    }

    public MergedSpectrum merge(MergedSpectrum right, MergedDataPoint[] mergedSpectrum) {
        final RawDataFile[] norigs = Arrays.copyOf(origins, origins.length+right.origins.length);
        System.arraycopy(right.origins, 0, norigs, origins.length, right.origins.length);
        final int[] nscans = Arrays.copyOf(scanIds, scanIds.length+right.scanIds.length);
        System.arraycopy(right.scanIds, 0, nscans, scanIds.length, right.scanIds.length);
        MergedSpectrum newMerged = new MergedSpectrum(mergedSpectrum, norigs, nscans, right.precursorMz, right.polarity, right.precursorCharge, removedScansByLowQuality + right.removedScansByLowQuality, removedScansByLowCosine + right.removedScansByLowCosine, Math.max(bestFragmentScanScore, right.bestFragmentScanScore));
        return newMerged;
    }

    public double getTIC() {
        double tic = 0d;
        for (MergedDataPoint p : data)
            tic += p.intensity;
        return tic;
    }

    public MergedSpectrum filterByRelativeNumberOfScans(double minimumRelativeNumberOfScans) {
        int minNum = (int)(data.length*minimumRelativeNumberOfScans);
        if (minNum>1) return filterByNumberOfScans(minNum);
        else return this;
    }

    public MergedSpectrum filterByNumberOfScans(int minimumNumberOfScans) {
        return new MergedSpectrum(
                Arrays.stream(data).filter(x->x.sources.length >= minimumNumberOfScans).toArray(MergedDataPoint[]::new),
                origins,
                scanIds,
                precursorMz,
                polarity,
                precursorCharge,
                removedScansByLowQuality,
                removedScansByLowCosine,
                bestFragmentScanScore

        );
    }
}