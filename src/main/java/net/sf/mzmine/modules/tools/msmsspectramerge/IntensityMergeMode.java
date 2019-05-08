package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.datamodel.DataPoint;

public interface IntensityMergeMode {

    static IntensityMergeMode[] values() {
        return new IntensityMergeMode[]{SUM,MAXIMUM,AVERAGE};
    }

    public double merge(DataPoint[] sources);

    public static final IntensityMergeMode MAXIMUM = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double max = 0d;
            for (DataPoint p : sources)
                max = Math.max(p.getIntensity(), max);
            return max;
        }

        @Override
        public String toString() {
            return "maximum intensity";
        }
    };
    public static final IntensityMergeMode SUM = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double sum = 0d;
            for (DataPoint p : sources)
                sum += p.getIntensity();
            return sum;
        }
        @Override
        public String toString() {
            return "sum intensities";
        }
    };
    public static final IntensityMergeMode AVERAGE = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double avg = 0d;
            for (DataPoint p : sources)
                avg += p.getIntensity();
            return avg / sources.length;
        }
        @Override
        public String toString() {
            return "mean intensity";
        }
    };

}
