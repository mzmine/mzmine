/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class TICToolTipGenerator implements XYToolTipGenerator {

    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    private static NumberFormat intensityFormat = new DecimalFormat("0.00E0");

    /**
     * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateToolTip(XYDataset dataset, int series, int item) {
        double rtValue = dataset.getYValue(series, item);
        double intValue = dataset.getYValue(series, item);
        return "Retention time: " + rtFormat.format(rtValue) + ", IC: "
                + intensityFormat.format(intValue);
    }

}
