package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.scans.ScanUtils;

import java.util.Arrays;

public interface MzMergeMode {

    public static MzMergeMode[] values() {
        return new MzMergeMode[]{MOST_INTENSIVE, WEIGHTED_AVERAGE_CUTOFF_OUTLIERS, WEIGHTED_AVERAGE};
    }

    public double merge(DataPoint[] sources);



    public static MzMergeMode WEIGHTED_AVERAGE = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double mz=0d, intens=0d;
            for (DataPoint d : sources) {
                mz += d.getMZ()*d.getIntensity();
                intens += d.getIntensity();
            }
            return mz/intens;
        }
        @Override
        public String toString() {
            return "weighted average";
        }
    };

    public static MzMergeMode MOST_INTENSIVE = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double mz=Double.NEGATIVE_INFINITY, intens=Double.NEGATIVE_INFINITY;
            for (DataPoint d : sources) {
                if (d.getIntensity()>intens) {
                    mz = d.getMZ();
                    intens = d.getIntensity();
                }
            }
            return mz;
        }
        @Override
        public String toString() {
            return "most intensive";
        }
    };

    public static MzMergeMode WEIGHTED_AVERAGE_CUTOFF_OUTLIERS = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            if (sources.length >= 4) {
                sources = sources.clone();
                ScanUtils.sortDataPointsByMz(sources);
                int i = (int)(sources.length*0.25);
                double mz=0d, intens=0d;
                for (int k=i; k < sources.length-i; ++k) {
                    mz += sources[k].getMZ()*sources[k].getIntensity();
                    intens += sources[k].getIntensity();
                }
                return mz/intens;
            } else return WEIGHTED_AVERAGE.merge(sources);
        }
        @Override
        public String toString() {
            return "weighted average (remove outliers)";
        }
    };

}
