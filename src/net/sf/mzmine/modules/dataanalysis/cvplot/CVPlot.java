package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.JLabel;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

public class CVPlot extends ChartPanel {
	
	private JFreeChart chart;
	private XYPlot plot;

    private static final Color gridColor = Color.lightGray;
    private static final Color crossHairColor = Color.gray;
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11); 

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            1.0f, new float[] { 5, 3 }, 0);

	
	private XYItemRenderer spotRenderer;
	
	public CVPlot(CVAnalyzerWindow masterFrame, CVDataset dataset) {
		super(null);
		
		chart = ChartFactory.createXYAreaChart(
				"Coefficient of variation",
				"Retention time",
				"m/z",
				dataset,
				PlotOrientation.VERTICAL,
				false,
				false,
				false
				);
		chart.setBackgroundPaint(Color.white);
		setChart(chart);
		
        // title
        TextTitle chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);
		
        // set the plot properties
        plot = chart.getXYPlot();		
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        // set grid properties
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);

        // set crosshair (selection) properties
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairPaint(crossHairColor);
        plot.setRangeCrosshairPaint(crossHairColor);
        plot.setDomainCrosshairStroke(crossHairStroke);
        plot.setRangeCrosshairStroke(crossHairStroke);
        
       
        Desktop desktop = MainWindow.getInstance();
        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat mzFormat = desktop.getMZFormat();

        // set the X axis (retention time) properties
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(rtFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);

        // set the Y axis (intensity) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(mzFormat);
			
		spotRenderer = new CVRenderer(dataset);
		
		plot.setDataset(dataset);
		plot.setRenderer(spotRenderer);
	
	}
	
}
