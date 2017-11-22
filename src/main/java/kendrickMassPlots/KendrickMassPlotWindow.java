package kendrickMassPlots;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisCollection;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;

public class KendrickMassPlotWindow  extends JFrame{
	 /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private XYDataset dataset;
    private JFreeChart chart;

    public KendrickMassPlotWindow(ParameterSet parameters) {

        PeakList peakList = parameters
                .getParameter(KendrickMassPlotParameters.peakList).getValue()
                .getMatchingPeakLists()[0];

        String title = "Kendrick mass plot [" + peakList + "]";
        String xAxisLabel = parameters
                .getParameter(KendrickMassPlotParameters.xAxisValues)
                .getValue();
        String yAxisLabel = parameters
                .getParameter(KendrickMassPlotParameters.yAxisValues)
                .getValue();

        // create dataset
        dataset = new KendrickMassPlotXYDataset(parameters);
        // create new JFreeChart
        logger.finest("Creating new chart instance");
       
            chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel,
                    dataset, PlotOrientation.VERTICAL, true, true, false);

            Plot plot = chart.getPlot();

            // set renderer
           

            // set tooltip generator
           

           

        chart.setBackgroundPaint(Color.white);

        // create chart JPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);

      

        // disable maximum size (we don't want scaling)
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        // set title properties
        TextTitle chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        LegendTitle legend = chart.getLegend();
        legend.setItemFont(legendFont);
        legend.setBorder(0, 0, 0, 0);
        legend.setVisible(false);

       
        setTitle(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();

        // get the window settings parameter
        ParameterSet paramSet = MZmineCore.getConfiguration()
                .getModuleParameters(KendrickMassPlotModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(KendrickMassPlotParameters.windowSettings);

        // update the window and listen for changes
        settings.applySettingsToWindow(this);
        this.addComponentListener(settings);

    }

    JFreeChart getChart() {
        return chart;
    }

	
}
