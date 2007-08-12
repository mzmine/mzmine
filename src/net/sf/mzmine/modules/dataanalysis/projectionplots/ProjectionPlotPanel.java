package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

public class ProjectionPlotPanel extends ChartPanel {

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
	
	//private XYItemRenderer spotRenderer;
	private ProjectionPlotRenderer spotRenderer;
	
	private InterpolatingLookupPaintScale paintScale;
	
	private ProjectionPlotDataset dataset;
	
	public ProjectionPlotPanel(ProjectionPlotWindow masterFrame, ProjectionPlotDataset dataset) {
		super(null);
		
		this.dataset = dataset;
		
		boolean createLegend = false;
		if (dataset.getNumberOfGroups()>1)
			createLegend = true;
		
		chart = ChartFactory.createXYAreaChart(
				"",
				dataset.getXLabel(),
				dataset.getYLabel(),
				dataset,
				PlotOrientation.VERTICAL,
				createLegend,
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
  
        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        // set the X axis (component 1) properties
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(numberFormat);

        // set the Y axis (component 2) properties
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(numberFormat);
       
		plot.setDataset(dataset);

		spotRenderer = new ProjectionPlotRenderer(plot, dataset);
		spotRenderer.setBaseItemLabelGenerator(new ProjectionPlotItemLabelGenerator());
		spotRenderer.setBaseItemLabelsVisible(false);
		spotRenderer.setBaseToolTipGenerator(new ProjectionPlotToolTipGenerator());
		plot.setRenderer(spotRenderer);

		// Setup legend 
		if (createLegend) { 
	        LegendItemCollection legendItemsCollection = new LegendItemCollection();
	        for (int groupNumber=0; groupNumber<dataset.getNumberOfGroups(); groupNumber++) {
	        	Object paramValue = dataset.getGroupParameterValue(groupNumber);
	        	if (paramValue==null) {
	        		// No parameter value available: search for raw data files within this group, and use their names as group's name
	        		String fileNames = new String();
	        		for (int itemNumber=0; itemNumber<dataset.getItemCount(0); itemNumber++) {
	        			RawDataFile rawDataFile = dataset.getRawDataFile(itemNumber);
	        			if (dataset.getGroupNumber(itemNumber)==groupNumber)
	        				fileNames = fileNames.concat(rawDataFile.toString());
	        		}
	        		if (fileNames.isEmpty())
	        			fileNames = "Empty group";
	        		
	        		paramValue = fileNames;
	        	}
	        	legendItemsCollection.add(new LegendItem(paramValue.toString(), "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, spotRenderer.getGroupPaint(groupNumber)));
	        }
	        plot.setFixedLegendItems(legendItemsCollection);    
	        LegendTitle legendTitle = new LegendTitle(plot);
		}
        //chart.addLegend(legendTitle);
        
        
		
		
	}
	
}
