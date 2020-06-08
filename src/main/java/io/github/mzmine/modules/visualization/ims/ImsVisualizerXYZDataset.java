package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.AbstractXYZDataset;

public class ImsVisualizerXYZDataset extends AbstractXYZDataset {

    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    private double[] xValues;
    private double[] yValues;
    private double[] zValues;
    private int scanSize;

   public ImsVisualizerXYZDataset ( ParameterSet parameters )
   {
       dataFiles = parameters.getParameter(ImsVisualizerParameters.dataFiles).getValue()
               .getMatchingRawDataFiles();

       scans = parameters.getParameter(ImsVisualizerParameters.scanSelection).getValue()
               .getMatchingScans(dataFiles[0]);

       mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

       scanSize = scans.length;

       // Calc xValues retention time
       xValues = new double[scanSize];
       for (int i = 0; i < scanSize; i++) {
           xValues[i] = scans[i].getRetentionTime();
       }
      // Calculte yvalues mobility value.
      yValues = new double[ scanSize ];
       for (int i = 0; i < scanSize; i++ )
       {
           yValues[i] = scans[ i ].getRetentionTime();
       }

       // Calculate zValues intensity.
       for (int i = 0; i < scanSize; i++ )
       {
           DataPoint dataPoint[] = scans[ i ].getDataPointsByMass(mzRange);

           for (int j = 0; j < scanSize; j++ )
           {
               zValues[ i ] += dataPoint[j].getIntensity();
           }
       }
   }

    @Override
    public int getSeriesCount() {
        return 1;
    }
    public Comparable<?> getRowKey(int item) {
        return scans[item].toString();
    }
    @Override
    public Comparable getSeriesKey(int series) {
        return getRowKey(series);
    }

    @Override
    public Number getZ(int series, int item) {
        return zValues[item];
    }

    @Override
    public int getItemCount(int series) {
        return scanSize;
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
