package  io.github.mzmine.modules.dataprocessing.featdet_ML;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class SplitSeries {
    
    //@brief cuts out a single region of length @regionSize with start point @start
    public static final double[] extractSingleRegion(double[] fullSeries, int start, int regionSize){
        double[] standardRegion = new double[regionSize];
        System.arraycopy(fullSeries, start , standardRegion, 0, regionSize);

        //How should .orElse be handeled?
        final double maxIntensity = Arrays.stream(standardRegion).max().orElse(Double.NaN);
        if (maxIntensity != 0){
            Arrays.stream(standardRegion).map(n -> n/maxIntensity).toArray();
        }
        return standardRegion;
    }    
 
    //@brief iterates over the full series and cuts outs regions with overslap @regionSize/2. Adds zero padding to the end to get the right length.
    public static final List<double[]> extractRegionBatch(double[] fullSeries, int regionSize){
        int seriesLength = fullSeries.length;
        int numRegions = seriesLength/(regionSize/2);
        int paddingLength=(numRegions+1)*(regionSize/2)-seriesLength;
        double[] paddedSeries = new double[seriesLength + paddingLength];
        System.arraycopy(fullSeries, 0,paddedSeries,0, seriesLength);
        List<double[]> regionBatch = new ArrayList<>(); 
        for (int i=0; i<numRegions;i++){
           regionBatch.add(extractSingleRegion(fullSeries, i*(regionSize/2), regionSize)); 
        }
        return regionBatch;
    }
    
    //@brief extract batch with default length 128
    public static final List<double[]> extractRegionBatch(double[] fullSeries){
        return extractRegionBatch(fullSeries, 128);
    }
}
