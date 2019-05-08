package net.sf.mzmine.modules.tools.msmsspectramerge;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.scans.ScanUtils;

import java.util.Arrays;

/**
 * Calculates some quality score given a MS/MS and its surrounding MS1
 */
public interface Ms2QualityScoreModel {

    /**
     * Calculate the quality score for all MS/MS scans within the fragment scan object
     * @param fragmentScan
     * @return a score which is higher for high quality MS/MS and negative for scans that should be removed
     */
    public double[] calculateQualityScore(FragmentScan fragmentScan);

    public static Ms2QualityScoreModel SelectByTIC = new Ms2QualityScoreModel() {
        @Override
        public double[] calculateQualityScore(FragmentScan fragmentScan) {
            final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
            Range<Double> mzRange = fragmentScan.feature.getMZ() < 75 ? Range.closed(50d,fragmentScan.feature.getMZ()) : Range.closed(0d, fragmentScan.feature.getMZ()-20d);
            for (int i=0; i < scores.length; ++i) {
                double tic = ScanUtils.calculateTIC(fragmentScan.origin.getScan(fragmentScan.ms2ScanNumbers[i]), mzRange);
                scores[i] = tic;
            }
            return scores;
        }
    };

    public static Ms2QualityScoreModel SelectBy20HighestPeaks = new Ms2QualityScoreModel() {
        @Override
        public double[] calculateQualityScore(FragmentScan fragmentScan) {
            final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
            Range<Double> mzRange = fragmentScan.feature.getMZ() < 75 ? Range.closed(50d,fragmentScan.feature.getMZ()) : Range.closed(0d, fragmentScan.feature.getMZ());
            for (int i=0; i < scores.length; ++i) {
                DataPoint[] spectrum = fragmentScan.origin.getScan(fragmentScan.ms2ScanNumbers[i]).getMassList(fragmentScan.massList).getDataPoints();
                Arrays.sort(spectrum, (u,v)->Double.compare(v.getIntensity(),u.getIntensity()));
                for (int j=0; j < spectrum.length; ++j)
                    scores[i] += spectrum[j].getIntensity();
            }
            return scores;
        }
    };

    public static Ms2QualityScoreModel SelectByMs1Intensity = new Ms2QualityScoreModel() {
        @Override
        public double[] calculateQualityScore(FragmentScan fragmentScan) {
            return fragmentScan.getInterpolatedPrecursorAndChimericIntensities()[0];
        }
    };

    public static Ms2QualityScoreModel SelectByLowChimericIntensityRelativeToMs1Intensity = new Ms2QualityScoreModel() {
        @Override
        public double[] calculateQualityScore(FragmentScan fragmentScan) {
            final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
            final double[][] interpolations = fragmentScan.getInterpolatedPrecursorAndChimericIntensities();
            for (int k=0; k < scores.length; ++k) {
                if (interpolations[0][k]<=0) continue;
                final double relative = Math.min(10, interpolations[0][k]/interpolations[1][k]);
                scores[k] = interpolations[0][k] * relative;
            }
            return scores;
        }
    };

}
