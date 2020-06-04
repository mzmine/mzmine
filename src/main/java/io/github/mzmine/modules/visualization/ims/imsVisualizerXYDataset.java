package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYDataset;

import java.util.ArrayList;

public class imsVisualizerXYDataset extends AbstractXYDataset {
    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    ArrayList<Double> mzValues;
    ArrayList<Double> mobility;
    private double[] xValues;
    private double[] yValues;

    public imsVisualizerXYDataset(ParameterSet parameters) {

        dataFiles = parameters.getParameter(imsVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        scans = parameters.getParameter(imsVisualizerParameters.scanSelection).getValue()
                .getMatchingScans(dataFiles[0]);

        mzRange = parameters.getParameter(imsVisualizerParameters.mzRange).getValue();

        // Calc xValues retention time
        mzValues = new ArrayList<Double>();
        mobility = new ArrayList<Double>();

        for (int i = 0; i < scans.length; i++) {
            DataPoint dataPoint[] = scans[i].getDataPoints();
            double sum = 0.0;
            for ( int k = 0; k < dataPoint.length; k++ )
            {
                sum += dataPoint[k].getMZ();
            }
            mobility.add(scans[i].getMobility());
            double avarageMZ = dataPoint.length > 0 ? sum / dataPoint.length : 0.0;
            mzValues.add(avarageMZ);
        }

        yValues = new double[ mobility.size() ];
        xValues = new double[ mzValues.size() ];

        for ( int k = 0; k < mobility.size(); k++ )
        {
            yValues[ k ] = mobility.get( k );
            xValues[ k ] = mzValues.get( k );
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
        return mobility.size();
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
