package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYDataset;

import java.util.ArrayList;

public class imsVisualizerXYDataset extends AbstractXYDataset {
    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    ArrayList<Double> retentionTimes;
    private double[] xValues;
    private double[] yValues;

    public imsVisualizerXYDataset(ParameterSet parameters) {

        dataFiles = parameters.getParameter(imsVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        scans = parameters.getParameter(imsVisualizerParameters.scanSelection).getValue()
                .getMatchingScans(dataFiles[0]);

        mzRange = parameters.getParameter(imsVisualizerParameters.mzRange).getValue();

        // Calc xValues retention time
        retentionTimes = new ArrayList<Double>();
        for (int i = 0; i < scans.length; i++) {
            if (i == 0) {
                retentionTimes.add(scans[i].getRetentionTime());
            } else if (scans[i].getRetentionTime() != scans[i - 1].getRetentionTime()) {
                retentionTimes.add(scans[i].getRetentionTime());
            }
        }

        xValues = new double[retentionTimes.size()];
        for (int i = 0; i < retentionTimes.size(); i++) {
            xValues[i] = retentionTimes.get(i);
        }

        // Calc yValues Intensity
        yValues = new double[retentionTimes.size()];
        for (int k = 0; k < retentionTimes.size(); k++) {
            for (int i = 0; i < scans.length; i++) {
                if (scans[i].getRetentionTime() == retentionTimes.get(k)) {
                    DataPoint dataPoints[] = scans[i].getDataPoints();
                    for (int j = 0; j < dataPoints.length; j++) {
                        if (mzRange.contains(dataPoints[j].getMZ())) {
                            yValues[k] = yValues[k] + dataPoints[j].getIntensity();
                        }
                    }
                } else {
                    continue;
                }
            }
        }
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable getSeriesKey(int series) {
        return getRowKey(series);
    }
    public Comparable<?> getRowKey(int item) {
        return scans[item].toString();
    }
    @Override
    public int getItemCount(int series) {
        return retentionTimes.size();
    }

    @Override
    public Number getX(int series, int item) {
        return xValues[item];
    }

    @Override
    public Number getY(int series, int item) {
        return yValues[item];
    }
}
