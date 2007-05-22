package net.sf.mzmine.modules.visualization.tic;

import java.util.Date;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataAcceptor;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

public class IntegratedPeakAreaDataSet extends DefaultTableXYDataset implements
		RawDataAcceptor {


    private TICVisualizerWindow visualizer;
    private int[] scanNumbers;
    private XYSeries series;
    private double mzMin, mzMax;
    private int firstScan, lastScan;

    IntegratedPeakAreaDataSet(OpenedRawDataFile dataFile,
            int scanNumbers[], double mzMin, double mzMax,
            TICVisualizerWindow visualizer) {

        this.visualizer = visualizer;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.scanNumbers = scanNumbers;
        
        // Find first and last scan number inside the integrated peak area
        firstScan = scanNumbers[0];
        for (int i=0; i<scanNumbers.length; i++)
        	if (firstScan>scanNumbers[i])
        		firstScan = scanNumbers[i];
        
        lastScan = scanNumbers[0];
        for (int i=0; i<scanNumbers.length; i++) 
        	if (lastScan<scanNumbers[i]) 
        		lastScan = scanNumbers[i];


        series = new XYSeries(dataFile.toString(), false, false);

        addSeries(series);

    }

    int getSeriesIndex(double retentionTime, double intensity) {
        int seriesIndex = series.indexOf(retentionTime);
        if (seriesIndex < 0)
            return -1;
        if (series.getY(seriesIndex).equals(intensity))
            return seriesIndex;
        return -1;
    }

    int getScanNumber(int index) {
        return scanNumbers[index];
    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(Scan scan, int index, int total) {

    	int scanNumber = scan.getScanNumber();
    	
    	if ( (scanNumber<firstScan) || (scanNumber>lastScan) ) return;
    	
        double intensityValues[] = scan.getIntensityValues();

        double totalIntensity = 0;

        switch (visualizer.getPlotType()) {

        case TIC:
            double mzValues[] = scan.getMZValues();
            for (int j = 0; j < intensityValues.length; j++) {
                if ((mzValues[j] >= mzMin) && (mzValues[j] <= mzMax))
                    totalIntensity += intensityValues[j];
            }
            break;

        case BASE_PEAK:
            double basePeak[] = ScanUtils.findBasePeak(scan, mzMin, mzMax);
            totalIntensity = basePeak[1];
            break;

        }


        // always redraw when we add last value
        boolean notify = false;
        if (scanNumber == lastScan)
            notify = true;

        series.add(scan.getRetentionTime(), totalIntensity, notify);

    }


}
