package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYZDataset;

public class imsVisualizerXYZDataset extends AbstractXYZDataset {

    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    private double[] xValues;
    private int[] yValues;
    private double[] zValues;


    public imsVisualizerXYZDataset(ParameterSet parameters) {

        dataFiles = parameters.getParameter(imsVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        scans = parameters.getParameter(imsVisualizerParameters.scanSelection).getValue()
                .getMatchingScans(dataFiles[0]);

        mzRange = parameters.getParameter(imsVisualizerParameters.mzRange).getValue();

        // Calc xValues retention time
        xValues = new double[scans.length];
        for (int i = 0; i < scans.length; i++) {
            xValues[i] = scans[i].getRetentionTime();
        }

        // Calc yValues Drift time (bins)
        yValues = new int[scans.length];
        int numberOfBins = getNumberOfBins(scans);
        int binNumber = 1;
        for (int i = 0; i < scans.length; i++) {
            yValues[i] = binNumber;
            if (binNumber <= numberOfBins) {
                binNumber++;
            }
            if (binNumber > numberOfBins) {
                binNumber = 1;
            }
        }

        // Calc zValues
        zValues = new double[scans.length];
        for (int i = 0; i < scans.length; i++) {
            DataPoint dataPoints[] = scans[i].getDataPoints();
            for (int j = 0; j < dataPoints.length; j++) {
                if (mzRange.contains(dataPoints[j].getMZ())) {
                    zValues[i] = zValues[i] + dataPoints[j].getIntensity();
                }
            }
        }
    }

    private int getNumberOfBins(Scan scans[]) {
        int numberOfBins = 0;
        double scanRetentionTime = scans[0].getRetentionTime();
        for (int i = 0; i < scans.length; i++) {
            if (scanRetentionTime == scans[i].getRetentionTime()) {
                numberOfBins++;
            } else {
                break;
            }
        }
        return numberOfBins;
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }
    @Override
    public Number getZ(int series, int item) {
        return zValues[item];
    }
    @Override
    public int getItemCount(int series) {
        return scans.length;
    }
    @Override
    public Number getX(int series, int item) {
        return xValues[item];
    }
    @Override
    public Number getY(int series, int item) {
        return yValues[item];
    }
    public Comparable<?> getRowKey(int item) {
        return scans[item].toString();
    }

    @Override
    public Comparable getSeriesKey(int series) {
        return getRowKey(series);
    }
}
