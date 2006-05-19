/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class TICItemLabelGenerator implements XYItemLabelGenerator {

    private TICPlot plot;
    private NumberFormat intensityFormat = new DecimalFormat("0.00E0"); // TODO

    TICItemLabelGenerator(TICPlot plot) {
        this.plot = plot;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataset, int series, int item) {

        final double originalX = dataset.getXValue(series, item);

        final double pointX = plot.getPlot().getDomainAxis().getRange()
                .getLength()
                / plot.getWidth();

        for (int i = item - 1; i > 1; i--) {
            if (dataset.getXValue(series, i) < (originalX - 50 * pointX))
                break;
            if (dataset.getYValue(series, item) <= dataset.getYValue(series, i))
                return null;
        }
        for (int i = item + 1; i < dataset.getItemCount(series); i++) {
            if (dataset.getXValue(series, i) > (originalX + 50 * pointX))
                break;
            if (dataset.getYValue(series, item) <= dataset.getYValue(series, i))
                return null;
        }

        return intensityFormat.format(dataset.getYValue(series, item));
    }

}
