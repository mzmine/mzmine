package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

public class CVPlot extends ChartPanel {

    private static final Color gridColor = Color.lightGray;
    private static final Color crossHairColor = Color.gray;
    private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11); 
    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            1.0f, new float[] { 5, 3 }, 0);

	private JFreeChart chart;
	private XYPlot plot;
	private ValueAxis paintScaleAxis;
	private PaintScaleLegend paintScaleLegend;
	
	private XYItemRenderer spotRenderer;
	
	private InterpolatingLookupPaintScale paintScale;
	
	private CVDataset dataset;
	
	public CVPlot(CVAnalyzerWindow masterFrame, CVDataset dataset) {
		super(null);
		
		this.dataset = dataset;
		
		chart = ChartFactory.createXYAreaChart(
				"",
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
        chart.removeSubtitle(chartTitle);

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
			
       
        // Create paint scale with default color slide
        paintScale = new InterpolatingLookupPaintScale();
        paintScale.add(0.00, new Color(0,  0,  0));
        paintScale.add(0.15, new Color(102,255,102));
        paintScale.add(0.30, new Color( 51,102,255));
        paintScale.add(0.45, new Color(255,  0,  0));
		
		plot.setDataset(dataset);
		spotRenderer = new CVRenderer(dataset, paintScale);
		plot.setRenderer(spotRenderer);
		
		// Add a paintScaleLegend to chart
		
		paintScaleAxis = new NumberAxis("CV");
		paintScaleAxis.setRange(paintScale.getLowerBound(), paintScale.getUpperBound());
		
		paintScaleLegend = new PaintScaleLegend(paintScale, paintScaleAxis);
		paintScaleLegend.setPosition(plot.getDomainAxisEdge());
		paintScaleLegend.setMargin(5,25,5,25);
		
		chart.addSubtitle(paintScaleLegend);

	
	}
	
	public InterpolatingLookupPaintScale getPaintScale() {
		return paintScale;
	}
	
	public void setPaintScale(InterpolatingLookupPaintScale paintScale) {
		spotRenderer = new CVRenderer(dataset, paintScale);
		plot.setRenderer(spotRenderer);

		this.paintScale = paintScale;
		paintScaleAxis.setRange(paintScale.getLowerBound(), paintScale.getUpperBound());
		paintScaleLegend.setScale(paintScale);
	}
	
}
