/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolTipUI;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

public class ScatterPlotToolTipUI extends BasicToolTipUI {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private String[] header;
	private String ratioText;
	private Color ratioColor;
	private ScatterPlotDataSet dataSet;
	private PeakListRow row;
	private ChromatographicPeak[] peaks;

	private static DecimalFormat formatter = new DecimalFormat("###.#");
	private static DecimalFormat formatter2 = new DecimalFormat("###");
	private static DecimalFormat formatter3 = new DecimalFormat("###.##");
	private static NumberFormat mzFormat = MZmineCore.getMZFormat();
	private static NumberFormat rtFormat = MZmineCore.getRTFormat();
	private static NumberFormat intensityFormat = MZmineCore
			.getIntensityFormat();
	private static int MAX_WIDTH = 100;
	private static int LINE_WIDTH = 1;
	private static int PLOT_HEIGHT = 50;
	private static int TIP_HEIGHT = 180;
	
	private static BasicStroke bs = new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER);

	// plot colors for plotted files, circulated by numberOfDataSets
	public static final Color[] plotColors = { new Color(0, 0, 192), // blue
			new Color(192, 0, 0), // red
			new Color(0, 192, 0), // green
			Color.magenta, Color.cyan, Color.orange };

	public ScatterPlotToolTipUI() {
		logger.finest("Crea toolTipUI para scatterPlot");
	}

	public void paint(Graphics g, JComponent c) {
		
		logger.finest("Dibuja ToolTip");
		
		JPanel newComponent = ((ScatterPlotToolTip) c).getToolTipComponent();
		//newComponent.setMaximumSize(new Dimension(50,50));
		//newComponent.setMinimumSize(new Dimension(50,50));
		//newComponent.setPreferredSize(new Dimension(50,50));
		Dimension newSize = newComponent.getPreferredSize();
		newComponent.setBounds(10,10, 200, 200); //newSize.width,newSize.height);
		newComponent.paint(g);
		
		if (true) return;

		
		/*JLabel l = new JLabel("Test");
		l.setMaximumSize(new Dimension(50,50));
		l.setMinimumSize(new Dimension(50,50));
		l.setPreferredSize(new Dimension(50,50));
		l.setBounds(10,10,50,50);
		l.paint(g);
		
		if (true) return;*/

		
		Graphics2D g2 = (Graphics2D) g;

		RenderingHints qualityHints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		qualityHints.put(RenderingHints.KEY_COLOR_RENDERING,
				RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		qualityHints.put(RenderingHints.KEY_DITHERING,
				RenderingHints.VALUE_DITHER_ENABLE);
		qualityHints.put(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		qualityHints.put(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		qualityHints.put(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_NORMALIZE);

		g2.setRenderingHints(qualityHints);

		Font defaultFont = g2.getFont();
		FontMetrics metrics = g2.getFontMetrics(g.getFont());
		Dimension size = c.getSize();
		g2.setColor(c.getBackground());
		g2.fillRect(0, 0, size.width, size.height);
		g2.setColor(c.getForeground());

		// Get info
		// int index = ((ScatterPlotToolTip) c).getIndex();
		// row = (SimplePeakListRow) dataSet.getPeakList().getRow(index);
		int[] indexDomains = dataSet.getDomainsIndexes();
		RawDataFile[] rawDataFiles = dataSet.getPeakList().getRawDataFiles();
		int indX = indexDomains[0];
		int indY = indexDomains[1];
		peaks = new ChromatographicPeak[2];
		peaks[0] = row.getPeak(rawDataFiles[indY]);
		peaks[1] = row.getPeak(rawDataFiles[indX]);

		Range rtRange = rawDataFiles[indY].getDataRTRange(1);
		rtRange.extendRange(rawDataFiles[indX].getDataRTRange(1));

		Range intensityRange = peaks[0].getRawDataPointsIntensityRange();
		intensityRange.extendRange(peaks[1].getRawDataPointsIntensityRange());

		double maxIntensity = intensityRange.getMax();

		// Start drawing
		// Header
		int yPosition = 0;
		for (int i = 0; i < header.length; i++) {
			if (i == 0) {
				Font newFont = new Font("SansSerif.bold", Font.PLAIN,
						defaultFont.getSize() + 1);
				g2.setFont(newFont);
				g2.drawString(header[i], 3, (metrics.getHeight())
						* (yPosition + 1));
				g2.setFont(defaultFont);
				yPosition++;
				continue;
			} else {
				g2.drawString(header[i], 3, (metrics.getHeight())
						* (yPosition + 1));
				yPosition++;
			}
		}

		// Plot
		Dimension sizePlot = new Dimension((int) size.getWidth() - 6, PLOT_HEIGHT);
		int startPointPlotX = 3;
		int startPointPlotY = ((metrics.getHeight()) * (yPosition + 1)) + 5;
		g2.drawRoundRect(startPointPlotX, startPointPlotY, sizePlot.width,
				sizePlot.height, 3, 3);
		g2.setColor(Color.WHITE);
		g2.fillRect(startPointPlotX, startPointPlotY, sizePlot.width,
				sizePlot.height);

		int colorIndex = 0;
		for (ChromatographicPeak peak : peaks) {

			// if we have no data, just return
			if ((peak == null) || (peak.getScanNumbers().length == 0))
				continue;

			// get scan numbers, one data point per each scan
			int scanNumbers[] = peak.getScanNumbers();

			// set color for current XIC
			g2.setColor(plotColors[colorIndex]);
			colorIndex = (colorIndex + 1) % plotColors.length;

			// for each datapoint, find [X:Y] coordinates of its point in
			// painted image
			int xValues[] = new int[scanNumbers.length + 2];
			int yValues[] = new int[scanNumbers.length + 2];

			// find one datapoint with maximum intensity in each scan
			for (int i = 0; i < scanNumbers.length; i++) {

				double dataPointIntensity = 0;
				MzDataPoint dataPoint = peak.getMzPeak(scanNumbers[i]);

				if (dataPoint != null)
					dataPointIntensity = dataPoint.getIntensity();

				// get retention time (X value)
				double retentionTime = peak.getDataFile().getScan(
						scanNumbers[i]).getRetentionTime();

				// calculate [X:Y] coordinates
				xValues[i + 1] = (int) Math.floor((retentionTime - rtRange
						.getMin())
						/ rtRange.getSize() * (sizePlot.width - 1))
						+ startPointPlotX;
				yValues[i + 1] = sizePlot.height
						- (int) Math.floor(dataPointIntensity / maxIntensity
								* (sizePlot.height - 1)) + startPointPlotY;

			}

			// add first point
			xValues[0] = xValues[1];
			yValues[0] = sizePlot.height - 1;

			// add terminal point
			xValues[xValues.length - 1] = xValues[xValues.length - 2];
			yValues[yValues.length - 1] = sizePlot.height - 1;

			// draw the peak shape
			g2.drawPolyline(xValues, yValues, xValues.length);

		}

		// Ratio index
		Font ratioFont = new Font("SansSerif.bold", Font.PLAIN,
				defaultFont.getSize() + 7);
		g2.setFont(ratioFont);
		g2.setColor(ratioColor);
		g2.drawString(ratioText, size.width - 50, (metrics.getHeight())
				* (yPosition + 1)+ 25);
		g2.setFont(defaultFont);
		g2.setColor(c.getForeground());
		
		
		// Table of peak's values
		g2.setColor(c.getForeground());
		int offset = LINE_WIDTH + metrics.getHeight()/2 + 3;
		Font newFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont.getSize()+ 1);
		int startPointTableX = 3;
		int startPointTableY = (metrics.getHeight()) * (yPosition + 1) + 70;
		//BasicStroke bs = new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT,
			//	BasicStroke.JOIN_MITER);
		
		Point col1, col2, col3, col4, col5, lastColLine;
		Point row2 = new Point(startPointTableX, startPointTableY + (metrics.getHeight()) + (int) bs.getLineWidth());
		Point row3 = new Point(startPointTableX, row2.y + (metrics.getHeight()) + (int) bs.getLineWidth());
		Point lastRowLine = new Point(startPointTableX, row3.y + (metrics.getHeight()) + (int) bs.getLineWidth());
		col1 = new Point(startPointTableX, startPointTableY);
		
		// Only vertical lines
		g2.setStroke(bs);
		Line2D line = new Line2D.Double(col1.x, col1.y, lastRowLine.x, lastRowLine.y);
		g2.draw(line);

		// File Name
		g2.setFont(newFont);
		String text = "File name";
		g2.drawString(text, col1.x, col1.y + offset);
		g2.setFont(defaultFont);
		g2.drawString(rawDataFiles[indY].getFileName(), row2.x, row2.y + offset);//(int) bs.getLineWidth());
		g2.drawString(rawDataFiles[indX].getFileName(), row3.x, row3.y + offset);//(int) bs.getLineWidth());

		int defaultLen = SwingUtilities.computeStringWidth(metrics, text);
		int len1 = SwingUtilities.computeStringWidth(metrics, rawDataFiles[indY].getFileName());
		int len2 = SwingUtilities.computeStringWidth(metrics, rawDataFiles[indX].getFileName());
		if ((len1 > len2) && (len1 > defaultLen)){
			col2 = new Point(col1.x + startPointTableX + len1, startPointTableY);
		}
		if ((len2 > len1) && (len2 > defaultLen)){
			col2 = new Point(col1.x + startPointTableX + len2, startPointTableY);
		}
		else{
			col2 = new Point(col1.x + startPointTableX + defaultLen, startPointTableY);
		}

		line = new Line2D.Double(col2.x, col2.y, col2.x, lastRowLine.y);
		g2.draw(line);

		// Mass
		g2.setFont(newFont);
		text = "Mass";
		g2.drawString(text, col2.x + LINE_WIDTH, col2.y + offset);
		g2.setFont(defaultFont);
		g2.drawString(mzFormat.format(peaks[0].getMZ()), col2.x + LINE_WIDTH, row2.y + offset);
		g2.drawString(mzFormat.format(peaks[1].getMZ()), col2.x + LINE_WIDTH, row3.y + offset);
		
		len1 = SwingUtilities.computeStringWidth(metrics, mzFormat.format(peaks[0].getMZ()));
		len2 = SwingUtilities.computeStringWidth(metrics, mzFormat.format(peaks[1].getMZ()));
		if (len1 > len2){
			col3 = new Point(col2.x + startPointTableX + len1, startPointTableY);
		}
		else{
			col3 = new Point(col2.x + startPointTableX + len2, startPointTableY);
		}

		line = new Line2D.Double(col3.x, col3.y, col3.x, lastRowLine.y);
		g2.draw(line);

		// RT
		g2.setFont(newFont);
		text = "RT";
		g2.drawString(text, col3.x + LINE_WIDTH, col3.y + offset);
		g2.setFont(defaultFont);
		g2.drawString(rtFormat.format(peaks[0].getRT()), col3.x + LINE_WIDTH, row2.y + offset);
		g2.drawString(rtFormat.format(peaks[1].getRT()), col3.x + LINE_WIDTH, row3.y + offset);

		len1 = SwingUtilities.computeStringWidth(metrics, rtFormat.format(peaks[0].getRT()));
		len2 = SwingUtilities.computeStringWidth(metrics, rtFormat.format(peaks[1].getRT()));
		if (len1 > len2){
			col4 = new Point(col3.x + startPointTableX + len1, startPointTableY);
		}
		else{
			col4 = new Point(col3.x + startPointTableX + len2, startPointTableY);
		}

		line = new Line2D.Double(col4.x, col4.y, col4.x, lastRowLine.y);
		g2.draw(line);

		// Intensity
		g2.setFont(newFont);
		text = "Height";
		g2.drawString(text, col4.x + LINE_WIDTH, col4.y + offset);
		g2.setFont(defaultFont);
		g2.drawString(intensityFormat.format(peaks[0].getHeight()), col4.x + LINE_WIDTH, row2.y + offset);
		g2.drawString(intensityFormat.format(peaks[1].getHeight()), col4.x + LINE_WIDTH, row3.y + offset);

		defaultLen = SwingUtilities.computeStringWidth(metrics, text);
		len1 = SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[0].getHeight()));
		len2 = SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[1].getHeight()));
		if ((len1 > len2) && (len1 > defaultLen)){
			col5 = new Point(col4.x + startPointTableX + len1, startPointTableY);
		}
		if ((len2 > len1) && (len2 > defaultLen)){
			col5 = new Point(col4.x + startPointTableX + len2, startPointTableY);
		}
		else{
			col5 = new Point(col4.x + startPointTableX + defaultLen, startPointTableY);
		}

		line = new Line2D.Double(col5.x, col5.y, col5.x, lastRowLine.y);
		g2.draw(line);


		// Area
		g2.setFont(newFont);
		text = "Area";
		g2.drawString(text, col5.x + LINE_WIDTH, col5.y + offset);
		g2.setFont(defaultFont);
		g2.drawString(rtFormat.format(peaks[0].getArea()), col5.x + LINE_WIDTH, row2.y + offset);
		g2.drawString(rtFormat.format(peaks[1].getArea()), col5.x + LINE_WIDTH, row3.y + offset);

		defaultLen = SwingUtilities.computeStringWidth(metrics, text);
		len1 = SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[0].getArea()));
		len2 = SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[1].getArea()));
		if ((len1 > len2) && (len1 > defaultLen)){
			lastColLine = new Point(col5.x + startPointTableX + len1, startPointTableY);
		}
		if ((len2 > len1) && (len2 > defaultLen)){
			lastColLine = new Point(col5.x + startPointTableX + len2, startPointTableY);
		}
		else{
			lastColLine = new Point(col5.x + startPointTableX + defaultLen, startPointTableY);
		}

		line = new Line2D.Double(col1.x, col1.y, lastColLine.x, lastColLine.y);
		g2.draw(line);
		line = new Line2D.Double(lastColLine.x, lastColLine.y, lastColLine.x, lastRowLine.y);
		g2.draw(line);
		
		// Only horizontal lines
		line = new Line2D.Double(col1.x, col1.y, lastColLine.x, lastColLine.y);
		g2.draw(line);
		line = new Line2D.Double(row2.x, row2.y, lastColLine.x, row2.y);
		g2.draw(line);
		line = new Line2D.Double(row3.x, row3.y, lastColLine.x, row3.y);
		g2.draw(line);
		line = new Line2D.Double(lastRowLine.x, lastRowLine.y, lastColLine.x, lastRowLine.y);
		g2.draw(line);

	}

	public Dimension getPreferredSize(JComponent c) {
		
		JPanel newComponent = ((ScatterPlotToolTip) c).getToolTipComponent();
		return newComponent.getPreferredSize();
		
		
		/*int height, width;
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		int index = ((ScatterPlotToolTip) c).getIndex();
		
		if (index < 0) {
			return new Dimension(0, 0);
		}

		dataSet = ((ScatterPlotToolTip) c).getDataSet();

		if (dataSet == null) {
			return new Dimension(0, 0);
		}

		// Get info
		try {
			int[] indexDomains = dataSet.getDomainsIndexes();
			RawDataFile[] rawDataFiles = dataSet.getPeakList()
					.getRawDataFiles();
			int indX = indexDomains[0];
			int indY = indexDomains[1];
			int fold = ((ScatterPlotToolTip) c).getFold();
			int widthFileY = SwingUtilities.computeStringWidth(metrics, rawDataFiles[indY].getFileName());
			int widthFileX = SwingUtilities.computeStringWidth(metrics, rawDataFiles[indX].getFileName());
			row = (SimplePeakListRow) dataSet.getPeakList().getRow(index);
			peaks = new ChromatographicPeak[2];
			peaks[0] = row.getPeak(rawDataFiles[indY]);
			peaks[1] = row.getPeak(rawDataFiles[indX]);


			PeakIdentity identity = row.getPreferredCompoundIdentity();
			header = new String[3];
			if (identity != null) {
				header[0] = identity.getName();
				header[1] = "Formula: " + identity.getCompoundFormula();
				header[2] = "Identification method: "
						+ identity.getIdentificationMethod();
			} else {
				header[0] = "Unknown";
				header[1] = "Formula: ";
				header[2] = " ";
			}

			double height1 = 1, height2 = 1;
			if (peaks[0] != null){
				height1 = peaks[0].getHeight();
				widthFileY += SwingUtilities.computeStringWidth(metrics, mzFormat.format(peaks[0].getMZ()));
				widthFileY += SwingUtilities.computeStringWidth(metrics, rtFormat.format(peaks[0].getRT()));
				widthFileY += SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[0].getHeight()));
				widthFileY += SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[0].getArea()));
			}
			if (peaks[1] != null){
				height2 = peaks[1].getHeight();
				widthFileX += SwingUtilities.computeStringWidth(metrics, mzFormat.format(peaks[1].getMZ()));
				widthFileX += SwingUtilities.computeStringWidth(metrics, rtFormat.format(peaks[1].getRT()));
				widthFileX += SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[1].getHeight()));
				widthFileX += SwingUtilities.computeStringWidth(metrics, intensityFormat.format(peaks[1].getArea()));
			}

			if ((height1 < 0.0001f) || (height2 < 0.0001f)) {
				ratioText = " ";
			} else {

				double ratio = height1 / height2;

				ratioText = formatter.format(ratio) + "x";
				ratioColor = Color.BLACK;

				if (ratio > fold) {
					ratioColor = Color.RED;
					ratioText = formatter2.format(ratio) + "x";
				}
				if (ratio < ((float) 1 / fold)) {
					ratioColor = Color.BLUE;
					ratioText = formatter3.format(ratio) + "x";
				}

			}

			height = 180;//(metrics.getHeight() * 6) + 200 + (3 * LINE_WIDTH);
			if (widthFileY > widthFileX)
				width = widthFileY + 1;
			else
				width = widthFileX + 1;
			//width = calculateMaxWidth(metrics);
			return new Dimension(width + 6, TIP_HEIGHT);

		} catch (Exception e) {

			String tipTextError = "Valor por default";
			MAX_WIDTH = SwingUtilities.computeStringWidth(metrics, tipTextError);
			height = metrics.getHeight();
			return new Dimension(SwingUtilities.computeStringWidth(metrics,
					tipTextError) + 6, height + 4);

		}*/
	}

	private int calculateMaxWidth(FontMetrics metrics) {

		int width = 0;
		int calculatedWidth = MAX_WIDTH;
		for (String line : header) {
			width = SwingUtilities.computeStringWidth(metrics, line);
			if (width > calculatedWidth) {
				calculatedWidth = width;
			}
		}
		return calculatedWidth + 100;
	}
}
