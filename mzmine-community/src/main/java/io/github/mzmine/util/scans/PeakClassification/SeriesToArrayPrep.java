package io.github.mzmine.util.scans.PeakClassification;


import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.FeatureFullDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class SeriesToArrayPrep {
    
    public static final double[][] extractFeature(FeatureFullDataAccess fullSeries, IonTimeSeries<? extends Scan> resolved) {
        //gets start and end retention time for detected range
        final float start = resolved.getRetentionTime(0);
        final float end = resolved.getRetentionTime(resolved.getNumberOfValues() - 1);
        //gets index of detected peak and corresponding RT 
        final int maximumIndex = FeatureDataUtils.getMostIntenseIndex(resolved);
        final float rtAtMax = resolved.getRetentionTime(maximumIndex);

        //searches indices for range and peak in full series 
        final int maximumIndexFull = BinarySearch.binarySearch(rtAtMax, DefaultTo.CLOSEST_VALUE, fullSeries.getNumberOfValues(), i -> fullSeries.getRetentionTime(i));
        final int leftRangeIndex = BinarySearch.binarySearch(start, DefaultTo.CLOSEST_VALUE, fullSeries.getNumberOfValues(), i ->fullSeries.getRetentionTime(i));
        final int rightRangeIndex = BinarySearch.binarySearch(end, DefaultTo.CLOSEST_VALUE, fullSeries.getNumberOfValues(), i ->fullSeries.getRetentionTime(i));

        //calculates the size (in indices) of the range to the left and right of peak
        int relativeOffsetLeft = Math.min(maximumIndexFull - leftRangeIndex, 32);
        int relativeOffsetRight = Math.min(rightRangeIndex - maximumIndexFull,32);

        //caluclates left and right bound for the region in full series
        //offset in case +-32 is out of bounds
        int leftIndex = Math.max(0,maximumIndexFull-32);
        int rightIndex = Math.min(fullSeries.getNumberOfValues(),maximumIndexFull+32);
        int offset = Math.max(32 - maximumIndexFull,0);

        //copies standard region from full series to new array and normalize values
        double[] standardRegionIntensity = new double[64];
        System.arraycopy(fullSeries.getIntensityValues(), leftIndex, standardRegionIntensity, offset, rightIndex - leftIndex);
        final double maxIntensity = Arrays.stream(standardRegionIntensity).max().orElse(Double.NaN);
        for(int i =0; i<64;i++){
            standardRegionIntensity[i] = standardRegionIntensity[i]/maxIntensity;
        }

        //creates a second array where we only copy the entries in the detected region
        double[] standardRegionRange = new double[64];
        System.arraycopy(standardRegionIntensity, 32-relativeOffsetLeft, standardRegionRange, 32-relativeOffsetLeft, relativeOffsetLeft+relativeOffsetRight);

        //combind both arrays to obtain double[2][64] as input for the model
        double[][] intensityWithBackground = new double[2][64];
        intensityWithBackground[0]= standardRegionIntensity;
        intensityWithBackground[1] = standardRegionRange;
        return intensityWithBackground;
        }

        //iteratively applies extractFeature to create a batch for prediction
        public static final List<double[][]> extractFeatureBatch(FeatureFullDataAccess fullSeries, List<IonTimeSeries<? extends Scan>> resolvedList){
            final int batchSize = resolvedList.size();
            List<double[][]> batchedStandardRegions = new ArrayList<>(); 
            for(int i=0; i<batchSize; i++){
                batchedStandardRegions.add(extractFeature(fullSeries, resolvedList.get(i)));
            }
            return batchedStandardRegions;
        }

}
