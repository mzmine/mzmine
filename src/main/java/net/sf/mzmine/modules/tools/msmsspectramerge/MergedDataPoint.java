package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.scans.ScanUtils;
import java.util.Arrays;
import java.util.Locale;

/**
 * A data point which is the result of merging several other data points.
 * It keep tracks of its origins and might calculate its mass and intensity based on averaging or suming
 * over its origins.
 */
public class MergedDataPoint implements DataPoint {

    /**
     * the origins which are merged into this data point
     */
    protected final DataPoint[] sources;

    /**
     * the m/z of this data point
     */
    protected final double mz;

    /**
     * the intensity of this data point
     */
    protected final double intensity;

    /**
     * @param mzMergeMode how to merge the m/z values
     * @param intensityMergeMode how to merge the intensity values
     * @param sources data points to merge
     */
    public MergedDataPoint(MzMergeMode mzMergeMode, IntensityMergeMode intensityMergeMode, DataPoint... sources) {
        this.sources = sources;
        // calculate intensity
        this.intensity = intensityMergeMode.merge(sources);
        // calculate m/z
        this.mz = mzMergeMode.merge(sources);

    }

    public String toString() {
        return String.valueOf(mz) + " " + String.valueOf(intensity);
    }

    /**
     * Merge this peak with another peak. If the other peak is a MergedDataPoint itself, merge the source peaks
     * of both merged data points together.
     * @param additional
     * @param mergeMode how to merge the m/z values of the peaks
     * @param intensityMergeMode how to merge the intensity values of the peaks
     * @return new merged data point
     */
    public MergedDataPoint merge(DataPoint additional, MzMergeMode mergeMode, IntensityMergeMode intensityMergeMode) {
        if (additional instanceof MergedDataPoint) {
            final MergedDataPoint ad = (MergedDataPoint)additional;
            DataPoint[] cop = Arrays.copyOf(sources, sources.length + ad.sources.length);
            System.arraycopy(ad.sources, 0, cop, sources.length, ad.sources.length );
            return new MergedDataPoint(mergeMode, intensityMergeMode, cop);
        } else {
            DataPoint[] cop = Arrays.copyOf(sources, sources.length + 1);
            cop[cop.length - 1] = additional;
            return new MergedDataPoint(mergeMode,intensityMergeMode, cop);
        }
    }

    /**
     * @return a description of the merged peak
     */
    public String getComment() {
        double smallest = Double.POSITIVE_INFINITY, largest = Double.NEGATIVE_INFINITY, average = 0d;
        int apex = 0;
        for (int k = 0; k < sources.length; ++k) {
            final DataPoint p = sources[k];
            smallest = Math.min(smallest, p.getMZ());
            largest = Math.max(largest, p.getMZ());
            average += Math.abs(p.getMZ() - mz);
            if (p.getIntensity() > sources[apex].getIntensity()) apex = k;
        }
        average /= sources.length;
        // median
        final DataPoint[] copy = sources.clone();
        ScanUtils.sortDataPointsByMz(copy);
        double medianMz = copy[copy.length / 2].getMZ();
        if (copy.length > 1 && copy.length % 2 == 0)
            medianMz = (medianMz + copy[(copy.length) / 2 - 1].getMZ()) / 2d;
        return String.format(Locale.US, "%.5f ... %.5f  (median = %.5f, apex = %.5f). Standard deviation = %.5f. Peaks: %d", smallest, largest, medianMz, sources[apex].getMZ(), average, sources.length);
    }

    @Override
    public double getMZ() {
        return mz;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

}
